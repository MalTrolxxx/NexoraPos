package com.nexora.pos.venta;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El flujo completo de una caja, de punta a punta: HTTP → controlador → servicio → JPA → H2 con las
 * migraciones reales. @Transactional revierte todo al terminar cada test, así no se ensucian entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VentaFlujoTest {

    @Autowired
    private MockMvcTester mvc;

    private long jamonId;
    private long cocaId;

    @BeforeEach
    void catalogo() {
        cocaId = idDe(post("/api/productos", """
                {"codigoBarras": "7501055300075", "nombre": "Coca-Cola 600ml", "unidad": "PIEZA",
                 "precio": 18.00, "costo": 13.50, "stockInicial": 10, "stockMinimo": 5}"""));
        jamonId = idDe(post("/api/productos", """
                {"nombre": "Jamón de pavo", "unidad": "KILO", "precio": 140.00, "stockInicial": 2}"""));
    }

    @Test
    void venta_completa_escaneando_cobrando_en_efectivo_y_corte() {
        long ventaId = idDe(post("/api/ventas", null));

        for (int i = 0; i < 3; i++) {
            post("/api/ventas/" + ventaId + "/detalles", """
                    {"codigoBarras": "7501055300075"}""");
        }
        MvcTestResult conJamon = post("/api/ventas/" + ventaId + "/detalles",
                "{\"productoId\": " + jamonId + ", \"cantidad\": 0.333}");
        assertThat(conJamon).hasStatusOk();
        assertThat(leer(conJamon, "$.detalles.length()")).isEqualTo(2);
        assertThat(numero(conJamon, "$.detalles[0].cantidad")).isEqualByComparingTo("3.0");
        assertThat(numero(conJamon, "$.total")).isEqualByComparingTo("100.62");

        MvcTestResult abiertas = mvc.get().uri("/api/ventas/abiertas").exchange();
        assertThat(leer(abiertas, "$[?(@.id == " + ventaId + ")].lineas")).asString().contains("2");

        MvcTestResult cobro = post("/api/ventas/" + ventaId + "/cobro", """
                {"metodoPago": "EFECTIVO", "pagoCon": 200}""");
        assertThat(cobro).hasStatusOk();
        assertThat(leer(cobro, "$.estado")).isEqualTo("PAGADA");
        assertThat(numero(cobro, "$.cambio")).isEqualByComparingTo("99.38");

        assertThat(numero(mvc.get().uri("/api/productos/" + cocaId).exchange(), "$.stock")).isEqualByComparingTo("7.0");
        assertThat(numero(mvc.get().uri("/api/productos/" + jamonId).exchange(), "$.stock")).isEqualByComparingTo("1.667");

        MvcTestResult corte = mvc.get().uri("/api/ventas/corte").exchange();
        assertThat(leer(corte, "$.ventas")).isEqualTo(1);
        assertThat(numero(corte, "$.total")).isEqualByComparingTo("100.62");
        assertThat(leer(corte, "$.porMetodo[0].metodoPago")).isEqualTo("EFECTIVO");

        assertThat(post("/api/ventas/" + ventaId + "/detalles", "{\"productoId\": " + cocaId + "}"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void un_cliente_puede_tener_varias_ventas_en_espera() {
        long clienteId = idDe(post("/api/clientes", """
                {"nombre": "Doña Lupe", "telefono": "5551234567"}"""));
        long primera = idDe(post("/api/ventas", "{\"clienteId\": " + clienteId + "}"));
        long segunda = idDe(post("/api/ventas", "{\"clienteId\": " + clienteId + "}"));
        post("/api/ventas/" + primera + "/detalles", "{\"productoId\": " + cocaId + "}");

        MvcTestResult abiertas = mvc.get().uri("/api/ventas/abiertas?clienteId=" + clienteId).exchange();

        assertThat(leer(abiertas, "$[*].id")).asString().contains(String.valueOf(primera), String.valueOf(segunda));
        assertThat(leer(abiertas, "$[0].clienteNombre")).isEqualTo("Doña Lupe");
    }

    @Test
    void pago_insuficiente_no_cierra_la_venta_ni_toca_el_stock() {
        long ventaId = idDe(post("/api/ventas", null));
        post("/api/ventas/" + ventaId + "/detalles", "{\"productoId\": " + cocaId + ", \"cantidad\": 2}");

        assertThat(post("/api/ventas/" + ventaId + "/cobro", """
                {"metodoPago": "EFECTIVO", "pagoCon": 20}""")).hasStatus(HttpStatus.CONFLICT);

        assertThat(leer(mvc.get().uri("/api/ventas/" + ventaId).exchange(), "$.estado")).isEqualTo("ABIERTA");
        assertThat(numero(mvc.get().uri("/api/productos/" + cocaId).exchange(), "$.stock")).isEqualByComparingTo("10.0");
    }

    @Test
    void quitar_y_editar_lineas_recalcula_el_total() {
        long ventaId = idDe(post("/api/ventas", null));
        post("/api/ventas/" + ventaId + "/detalles", "{\"productoId\": " + cocaId + "}");
        MvcTestResult conJamon = post("/api/ventas/" + ventaId + "/detalles",
                "{\"productoId\": " + jamonId + ", \"cantidad\": 0.5}");
        long lineaCoca = ((Number) leer(conJamon, "$.detalles[0].id")).longValue();
        long lineaJamon = ((Number) leer(conJamon, "$.detalles[1].id")).longValue();

        MvcTestResult editada = mvc.put().uri("/api/ventas/" + ventaId + "/detalles/" + lineaCoca)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\": 4}").exchange();
        assertThat(numero(editada, "$.total")).isEqualByComparingTo("142.0"); // 4 x 18 + 0.5 x 140

        MvcTestResult sinJamon = mvc.delete().uri("/api/ventas/" + ventaId + "/detalles/" + lineaJamon).exchange();
        assertThat(numero(sinJamon, "$.total")).isEqualByComparingTo("72.0");
    }

    @Test
    void errores_de_entrada_responden_400_con_el_detalle() {
        long ventaId = idDe(post("/api/ventas", null));

        MvcTestResult sinProducto = post("/api/ventas/" + ventaId + "/detalles", "{\"cantidad\": 1}");
        assertThat(sinProducto).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(leer(sinProducto, "$.detalles.productoIndicado")).isEqualTo("Indica productoId o codigoBarras");

        assertThat(post("/api/productos", """
                {"nombre": "Leche", "unidad": "LITRO", "precio": 28}""")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/api/productos?sort=noExiste").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void el_lector_encuentra_el_producto_y_la_busqueda_pagina() {
        assertThat(leer(mvc.get().uri("/api/productos/codigo/7501055300075").exchange(), "$.id"))
                .isEqualTo((int) cocaId);
        assertThat(mvc.get().uri("/api/productos/codigo/000").exchange()).hasStatus(HttpStatus.NOT_FOUND);

        MvcTestResult busqueda = mvc.get().uri("/api/productos?q=JAM&size=1").exchange();
        assertThat(leer(busqueda, "$.contenido[0].nombre")).isEqualTo("Jamón de pavo");
        assertThat(leer(busqueda, "$.tamano")).isEqualTo(1);
    }

    private MvcTestResult post(String uri, String json) {
        var request = mvc.post().uri(uri);
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    private static Object leer(MvcTestResult result, String jsonPath) {
        return JsonPath.read(cuerpo(result), jsonPath);
    }

    /** Los montos se comparan por valor (3 == 3.000), sin importar cómo los escriba el JSON. */
    private static BigDecimal numero(MvcTestResult result, String jsonPath) {
        return new BigDecimal(leer(result, jsonPath).toString());
    }

    private static long idDe(MvcTestResult result) {
        assertThat(result.getResponse().getStatus()).as(cuerpo(result)).isEqualTo(201);
        return ((Number) leer(result, "$.id")).longValue();
    }

    private static String cuerpo(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
