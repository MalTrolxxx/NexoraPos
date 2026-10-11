package com.nexora.pos.venta;

import com.nexora.pos.cliente.Cliente;
import com.nexora.pos.cliente.ClienteRepository;
import com.nexora.pos.producto.Producto;
import com.nexora.pos.producto.ProductoRepository;
import com.nexora.pos.shared.ResourceNotFoundException;
import com.nexora.pos.venta.dto.AgregarProductoRequest;
import com.nexora.pos.venta.dto.CobroRequest;
import com.nexora.pos.venta.dto.CorteResponse;
import com.nexora.pos.venta.dto.TotalPorMetodo;
import com.nexora.pos.venta.dto.VentaResponse;
import com.nexora.pos.venta.dto.VentaResumenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquesta las ventas: carga lo necesario, delega las reglas a la entidad `Venta` y define los límites
 * de transacción. Las reglas en sí (stock, estado, cambio) viven en la entidad.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final Clock reloj;

    @Transactional
    public VentaResponse abrir(Long clienteId) {
        Cliente cliente = clienteId == null ? null : clientes.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente con id " + clienteId));
        return VentaResponse.from(ventas.save(new Venta(cliente)));
    }

    /** Las ventas en espera; con `clienteId`, solo las de ese cliente. */
    public List<VentaResumenResponse> abiertas(Long clienteId) {
        return ventas.resumenPorEstado(EstadoVenta.ABIERTA, clienteId);
    }

    public VentaResponse obtener(Long id) {
        return VentaResponse.from(buscarOFallar(id));
    }

    @Transactional
    public VentaResponse agregar(Long ventaId, AgregarProductoRequest request) {
        Venta venta = buscarOFallar(ventaId);
        Producto producto = resolverProducto(request);
        venta.agregar(producto, request.cantidad() == null ? BigDecimal.ONE : request.cantidad());
        ventas.flush(); // inserta la línea nueva ya, para devolverla con su id
        return VentaResponse.from(venta);
    }

    @Transactional
    public VentaResponse cambiarCantidad(Long ventaId, Long detalleId, BigDecimal cantidad) {
        Venta venta = buscarOFallar(ventaId);
        venta.cambiarCantidad(detalleId, cantidad);
        return VentaResponse.from(venta);
    }

    @Transactional
    public VentaResponse quitar(Long ventaId, Long detalleId) {
        Venta venta = buscarOFallar(ventaId);
        venta.quitar(detalleId);
        return VentaResponse.from(venta);
    }

    @Transactional
    public VentaResponse cobrar(Long ventaId, CobroRequest request) {
        Venta venta = buscarOFallar(ventaId);
        // Bloquea y carga de una vez todos los productos de la venta: los proxies LAZY de las líneas
        // quedan apuntando a estas instancias, así que descontar stock no dispara más consultas.
        productos.bloquearParaActualizar(venta.idsDeProductos());
        venta.cobrar(request.metodoPago(), request.pagoCon());
        return VentaResponse.from(venta);
    }

    @Transactional
    public VentaResponse cancelar(Long ventaId) {
        Venta venta = buscarOFallar(ventaId);
        venta.cancelar();
        return VentaResponse.from(venta);
    }

    /** Corte de caja de `fecha` (hoy si no se indica), en la zona horaria de la tienda. */
    public CorteResponse corte(LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        Instant desde = dia.atStartOfDay(reloj.getZone()).toInstant();
        Instant hasta = dia.plusDays(1).atStartOfDay(reloj.getZone()).toInstant();

        List<TotalPorMetodo> porMetodo = ventas.totalesPorMetodo(EstadoVenta.PAGADA, desde, hasta);
        long numVentas = porMetodo.stream().mapToLong(TotalPorMetodo::ventas).sum();
        BigDecimal total = porMetodo.stream().map(TotalPorMetodo::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = numVentas == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(numVentas), 2, RoundingMode.HALF_UP);
        return new CorteResponse(dia, numVentas, total, promedio, porMetodo);
    }

    private Producto resolverProducto(AgregarProductoRequest request) {
        if (request.productoId() != null) {
            return productos.findById(request.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + request.productoId()));
        }
        String codigo = request.codigoBarras().trim();
        return productos.findByCodigoBarrasAndActivoTrue(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un producto activo con el código " + codigo));
    }

    private Venta buscarOFallar(Long id) {
        return ventas.findConDetallesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la venta con id " + id));
    }
}
