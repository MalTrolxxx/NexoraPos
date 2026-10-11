package com.nexora.pos.producto;

import com.nexora.pos.shared.ReglaDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto del catálogo de la tienda (refrescos, abarrotes, granel...).
 *
 * Esta clase nunca sale del paquete `producto` hacia afuera de la app: lo que viaja por la
 * API son los DTO (ProductoRequest / ProductoResponse). Así, si mañana cambia cómo guardamos
 * el producto, no rompemos el contrato con quien consuma la API.
 *
 * No hay setters públicos a propósito: los cambios de estado pasan por métodos con nombre de
 * negocio (ajustarStock, desactivar...) para que la entidad no pueda quedar en un estado inválido
 * (stock negativo, media pieza de refresco) sin importar desde dónde se le llame.
 */
@Entity
@Table(name = "producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA lo necesita por reflexión; el código de la app usa el constructor de abajo
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opcional: los productos a granel normalmente no traen código. */
    @Column(name = "codigo_barras", length = 50)
    private String codigoBarras;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // columna VARCHAR, no el tipo ENUM nativo de MySQL: agregar una unidad no requiere ALTER TABLE
    @Column(nullable = false, length = 10)
    private UnidadVenta unidad;

    /** Precio de venta por pieza o por kilo, según `unidad`. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Lo que le cuesta a la tienda; opcional, sirve para calcular la ganancia. */
    @Column(precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stock;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Bloqueo optimista: si dos cajeros leen el mismo producto y guardan a la vez, el segundo save falla en vez de pisar al primero. */
    @Version
    private Long version;

    public Producto(String codigoBarras, String nombre, UnidadVenta unidad, BigDecimal precio,
                    BigDecimal costo, BigDecimal stockInicial, BigDecimal stockMinimo) {
        this.unidad = unidad;
        exigirCantidadValida(stockInicial, "stock inicial");
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.precio = precio;
        this.costo = costo;
        this.stock = stockInicial;
        this.stockMinimo = stockMinimo;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    public Producto(String nombre, UnidadVenta unidad, BigDecimal precio, BigDecimal stockInicial) {
        this(null, nombre, unidad, precio, null, stockInicial, BigDecimal.ZERO);
    }

    public void actualizarDatos(String codigoBarras, String nombre, UnidadVenta unidad,
                                BigDecimal precio, BigDecimal costo, BigDecimal stockMinimo) {
        if (!unidad.admite(stock)) {
            throw new ReglaDeNegocioException(
                    "No se puede cambiar \"" + nombre + "\" a " + unidad + ": el stock actual (" + stock + ") no es un número entero");
        }
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.unidad = unidad;
        this.precio = precio;
        this.costo = costo;
        this.stockMinimo = stockMinimo;
    }

    /** @param cantidad positiva para entradas de inventario, negativa para salidas (ventas). */
    public void ajustarStock(BigDecimal cantidad) {
        exigirCantidadValida(cantidad.abs(), "cantidad");
        BigDecimal nuevoStock = stock.add(cantidad);
        if (nuevoStock.signum() < 0) {
            throw new ReglaDeNegocioException(
                    "Stock insuficiente de \"" + nombre + "\": disponible " + stock.stripTrailingZeros().toPlainString()
                            + ", se pidieron " + cantidad.abs().stripTrailingZeros().toPlainString());
        }
        this.stock = nuevoStock;
    }

    /** Lanza si la cantidad no tiene sentido para la unidad del producto (p. ej. 1.5 piezas). */
    public void exigirCantidadValida(BigDecimal cantidad, String concepto) {
        if (cantidad.signum() < 0 || !unidad.admite(cantidad)) {
            throw new ReglaDeNegocioException(
                    "La " + concepto + " " + cantidad.toPlainString() + " no es válida para \"" + nombre + "\" (se vende por " + unidad + ")");
        }
    }

    public boolean isBajoStock() {
        return stock.compareTo(stockMinimo) <= 0;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }
}
