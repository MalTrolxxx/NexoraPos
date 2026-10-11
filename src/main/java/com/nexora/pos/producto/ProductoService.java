package com.nexora.pos.producto;

import com.nexora.pos.producto.dto.ProductoRequest;
import com.nexora.pos.producto.dto.ProductoResponse;
import com.nexora.pos.shared.Pagina;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import com.nexora.pos.shared.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas de negocio del catálogo. El controlador solo delega aquí; nunca llama al repositorio
 * directamente, para que ninguna regla (como el nombre o el código único) se pueda saltar.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // por defecto todo es de solo lectura; los métodos que escriben lo marcan explícitamente
public class ProductoService {

    /** Campos por los que se permite ordenar el listado: cualquier otro es un 400, no un error 500 de Hibernate. */
    private static final Set<String> ORDENABLES = Set.of("nombre", "precio", "stock", "codigoBarras", "creadoEn");

    private final ProductoRepository repository;

    public Pagina<ProductoResponse> buscar(String q, boolean incluirInactivos, Pageable pageable) {
        pageable.getSort().forEach(orden -> {
            if (!ORDENABLES.contains(orden.getProperty())) {
                throw new SolicitudInvalidaException("No se puede ordenar por \"" + orden.getProperty()
                        + "\". Opciones: " + String.join(", ", ORDENABLES));
            }
        });
        String filtro = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        return Pagina.de(repository.buscar(filtro, incluirInactivos, pageable).map(ProductoResponse::from));
    }

    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscarOFallar(id));
    }

    public ProductoResponse obtenerPorCodigo(String codigoBarras) {
        return repository.findByCodigoBarrasAndActivoTrue(codigoBarras.trim())
                .map(ProductoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un producto activo con el código " + codigoBarras));
    }

    public List<ProductoResponse> bajoStock() {
        return repository.findBajoStock().stream().map(ProductoResponse::from).toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = normalizar(request.codigoBarras());
        if (repository.existsByNombreIgnoreCase(request.nombre())) {
            throw nombreRepetido(request.nombre());
        }
        if (codigo != null && repository.existsByCodigoBarras(codigo)) {
            throw codigoRepetido(codigo);
        }
        Producto producto = new Producto(codigo, request.nombre(), request.unidad(), request.precio(), request.costo(),
                ceroSiNulo(request.stockInicial()), ceroSiNulo(request.stockMinimo()));
        return ProductoResponse.from(repository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarOFallar(id);
        String codigo = normalizar(request.codigoBarras());
        if (repository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw nombreRepetido(request.nombre());
        }
        if (codigo != null && repository.existsByCodigoBarrasAndIdNot(codigo, id)) {
            throw codigoRepetido(codigo);
        }
        producto.actualizarDatos(codigo, request.nombre(), request.unidad(), request.precio(), request.costo(),
                ceroSiNulo(request.stockMinimo()));
        // Sin save(): dentro de una transacción, Hibernate detecta los cambios en la entidad
        // "managed" y los guarda solo al terminar el método (dirty checking).
        return ProductoResponse.from(producto);
    }

    @Transactional
    public ProductoResponse registrarEntrada(Long id, BigDecimal cantidad) {
        Producto producto = buscarOFallar(id);
        producto.ajustarStock(cantidad);
        return ProductoResponse.from(producto);
    }

    @Transactional
    public void desactivar(Long id) {
        buscarOFallar(id).desactivar();
    }

    @Transactional
    public void activar(Long id) {
        buscarOFallar(id).activar();
    }

    private Producto buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + id));
    }

    /** Un código vacío se guarda como null; si no, dos productos a granel chocarían con el índice único. */
    private static String normalizar(String codigo) {
        return (codigo == null || codigo.isBlank()) ? null : codigo.trim();
    }

    private static BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static ReglaDeNegocioException nombreRepetido(String nombre) {
        return new ReglaDeNegocioException("Ya existe un producto con el nombre \"" + nombre + "\"");
    }

    private static ReglaDeNegocioException codigoRepetido(String codigo) {
        return new ReglaDeNegocioException("Ya existe un producto con el código de barras " + codigo);
    }
}
