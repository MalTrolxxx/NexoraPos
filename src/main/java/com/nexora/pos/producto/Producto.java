package com.nexora.pos.producto;

import com.nexora.pos.shared.ReglaDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto del catálogo (lo que se vende: agua, suplementos, accesorios...).
 *
 * Esta clase nunca sale del paquete `producto` hacia afuera de la app: lo que viaja por la
 * API son los DTO (ProductoRequest / ProductoResponse). Así, si mañana cambia cómo guardamos
 * el producto, no rompemos el contrato con quien consuma la API.
 *
 * No hay setters públicos a propósito: los cambios de estado pasan por métodos con nombre de
 * negocio (ajustarStock, desactivar...) para que la entidad no pueda quedar en un estado inválido
 * (por ejemplo, stock negativo) sin importar desde dónde se le llame.
 */
@Entity
@Table(name = "producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA lo necesita por reflexión; el código de la app usa el constructor de abajo
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Bloqueo optimista: si dos cajeros leen el mismo producto y guardan a la vez, el segundo save falla en vez de pisar al primero. */
    @Version
    private Long version;

    public Producto(String nombre, BigDecimal precio, Integer stock) {
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    public void actualizarDatos(String nombre, BigDecimal precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    /** @param cantidad positiva para entradas de inventario, negativa para salidas (ventas). */
    public void ajustarStock(int cantidad) {
        int nuevoStock = this.stock + cantidad;
        if (nuevoStock < 0) {
            throw new ReglaDeNegocioException(
                    "Stock insuficiente de \"" + nombre + "\": disponible " + stock + ", se pidieron " + Math.abs(cantidad));
        }
        this.stock = nuevoStock;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }
}
