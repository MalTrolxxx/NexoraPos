package com.nexora.pos.venta;

import com.nexora.pos.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Una línea del ticket. Solo la crea y modifica `Venta` (constructor y métodos package-private),
 * así el total de la venta nunca queda desfasado de sus líneas.
 */
@Entity
@Table(name = "detalle_venta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    /** LAZY: para el ticket basta `nombreProducto`; el producto solo se carga al cobrar (para descontar stock). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "nombre_producto", nullable = false, length = 120)
    private String nombreProducto;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    DetalleVenta(Venta venta, Producto producto, BigDecimal cantidad) {
        this.venta = venta;
        this.producto = producto;
        this.nombreProducto = producto.getNombre();
        this.precioUnitario = producto.getPrecio();
        cambiarCantidad(cantidad);
    }

    void cambiarCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
        // 0.333 kg x $140.00 = $46.62: se redondea al centavo en cada línea, como en un ticket real.
        this.subtotal = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }

    /** Compara por id (sin inicializar el proxy LAZY); si aún no hay id, por referencia. */
    boolean esDe(Producto otro) {
        if (producto == otro) {
            return true;
        }
        return producto.getId() != null && producto.getId().equals(otro.getId());
    }
}
