package com.nexora.pos.venta;

import com.nexora.pos.cliente.Cliente;
import com.nexora.pos.producto.Producto;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Una venta (ticket). Es la raíz del agregado: las líneas solo se tocan a través de ella, y ella
 * se encarga de que el total, el estado y el stock sean siempre coherentes.
 *
 * Mientras está ABIERTA el stock NO se descuenta (el cliente puede seguir agregando o arrepentirse);
 * se descuenta todo junto al cobrar.
 */
@Entity
@Table(name = "venta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private EstadoVenta estado;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleVenta> detalles = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metodo_pago", length = 15)
    private MetodoPago metodoPago;

    @Column(name = "pago_con", precision = 12, scale = 2)
    private BigDecimal pagoCon;

    @Column(precision = 12, scale = 2)
    private BigDecimal cambio;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    /** Si dos cajas editan la misma venta abierta a la vez, la segunda recibe 409 en vez de pisar a la primera. */
    @Version
    private Long version;

    public Venta(Cliente cliente) {
        this.cliente = cliente;
        this.estado = EstadoVenta.ABIERTA;
        this.total = BigDecimal.ZERO;
        this.creadaEn = Instant.now();
    }

    /** Solo lectura hacia afuera: agregar o quitar líneas tiene que pasar por los métodos de abajo. */
    public List<DetalleVenta> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /**
     * Si el producto ya está en la venta (al mismo precio) se suma a esa línea en lugar de crear otra:
     * escanear 3 veces el mismo refresco produce una línea con cantidad 3.
     */
    public DetalleVenta agregar(Producto producto, BigDecimal cantidad) {
        exigirAbierta();
        if (!producto.isActivo()) {
            throw new ReglaDeNegocioException("\"" + producto.getNombre() + "\" está desactivado y no se puede vender");
        }
        exigirCantidadPositiva(producto, cantidad);
        exigirStock(producto, cantidadEnVenta(producto, null).add(cantidad));

        DetalleVenta linea = detalles.stream()
                .filter(d -> d.esDe(producto) && d.getPrecioUnitario().compareTo(producto.getPrecio()) == 0)
                .findFirst()
                .orElse(null);
        if (linea == null) {
            linea = new DetalleVenta(this, producto, cantidad);
            detalles.add(linea);
        } else {
            linea.cambiarCantidad(linea.getCantidad().add(cantidad));
        }
        recalcularTotal();
        return linea;
    }

    public void cambiarCantidad(Long detalleId, BigDecimal cantidad) {
        exigirAbierta();
        DetalleVenta linea = buscarLinea(detalleId);
        Producto producto = linea.getProducto();
        exigirCantidadPositiva(producto, cantidad);
        exigirStock(producto, cantidadEnVenta(producto, linea).add(cantidad));
        linea.cambiarCantidad(cantidad);
        recalcularTotal();
    }

    public void quitar(Long detalleId) {
        exigirAbierta();
        detalles.remove(buscarLinea(detalleId));
        recalcularTotal();
    }

    /**
     * Cierra la venta y descuenta el stock de cada producto. Si algún producto no alcanza, se lanza la
     * excepción y la transacción completa se revierte: nunca queda una venta a medio descontar.
     */
    public void cobrar(MetodoPago metodo, BigDecimal pagoCon) {
        exigirAbierta();
        if (detalles.isEmpty()) {
            throw new ReglaDeNegocioException("No se puede cobrar una venta sin productos");
        }
        if (metodo == MetodoPago.EFECTIVO) {
            if (pagoCon == null || pagoCon.compareTo(total) < 0) {
                throw new ReglaDeNegocioException("El pago (" + (pagoCon == null ? "0" : pagoCon.toPlainString())
                        + ") no cubre el total (" + total.toPlainString() + ")");
            }
            this.pagoCon = pagoCon;
        } else {
            this.pagoCon = total; // tarjeta o transferencia: se cobra exacto
        }
        for (DetalleVenta linea : detalles) {
            linea.getProducto().ajustarStock(linea.getCantidad().negate());
        }
        this.metodoPago = metodo;
        this.cambio = this.pagoCon.subtract(total);
        this.estado = EstadoVenta.PAGADA;
        this.cerradaEn = Instant.now();
    }

    /** Como el stock solo se descuenta al cobrar, cancelar una venta abierta no tiene que devolver nada al inventario. */
    public void cancelar() {
        exigirAbierta();
        this.estado = EstadoVenta.CANCELADA;
        this.cerradaEn = Instant.now();
    }

    /** Ids de los productos de la venta, sin cargar los productos (se leen del proxy LAZY). */
    public Set<Long> idsDeProductos() {
        return detalles.stream().map(d -> d.getProducto().getId()).collect(Collectors.toSet());
    }

    private void exigirAbierta() {
        if (estado != EstadoVenta.ABIERTA) {
            throw new ReglaDeNegocioException("La venta " + id + " ya está " + estado + " y no se puede modificar");
        }
    }

    private static void exigirCantidadPositiva(Producto producto, BigDecimal cantidad) {
        if (cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor a 0");
        }
        producto.exigirCantidadValida(cantidad, "cantidad");
    }

    /** Aviso temprano para el cajero; la validación definitiva ocurre al cobrar con el stock bloqueado. */
    private static void exigirStock(Producto producto, BigDecimal cantidadTotal) {
        if (cantidadTotal.compareTo(producto.getStock()) > 0) {
            throw new ReglaDeNegocioException("Stock insuficiente de \"" + producto.getNombre() + "\": disponible "
                    + producto.getStock().stripTrailingZeros().toPlainString()
                    + ", en esta venta " + cantidadTotal.stripTrailingZeros().toPlainString());
        }
    }

    /** Cuánto de este producto ya hay en la venta, sin contar la línea `excepto` (la que se está editando). */
    private BigDecimal cantidadEnVenta(Producto producto, DetalleVenta excepto) {
        return detalles.stream()
                .filter(d -> d != excepto && d.esDe(producto))
                .map(DetalleVenta::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DetalleVenta buscarLinea(Long detalleId) {
        return detalles.stream()
                .filter(d -> detalleId.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("La venta " + id + " no tiene la línea " + detalleId));
    }

    private void recalcularTotal() {
        this.total = detalles.stream().map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
