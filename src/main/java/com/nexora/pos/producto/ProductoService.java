package com.nexora.pos.producto;

import com.nexora.pos.producto.dto.ProductoRequest;
import com.nexora.pos.producto.dto.ProductoResponse;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio del catálogo. El controlador solo delega aquí; nunca llama al repositorio
 * directamente, para que ninguna regla (como el nombre único) se pueda saltar.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // por defecto todo es de solo lectura; los métodos que escriben lo marcan explícitamente
public class ProductoService {

    private final ProductoRepository repository;

    public List<ProductoResponse> listar() {
        return repository.findAll().stream().map(ProductoResponse::from).toList();
    }

    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscarOFallar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (repository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ReglaDeNegocioException("Ya existe un producto con el nombre \"" + request.nombre() + "\"");
        }
        Producto producto = new Producto(request.nombre(), request.precio(), request.stock());
        return ProductoResponse.from(repository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarOFallar(id);
        producto.actualizarDatos(request.nombre(), request.precio());
        // Sin save(): dentro de una transacción, Hibernate detecta los cambios en la entidad
        // "managed" y los guarda solo al terminar el método (dirty checking).
        return ProductoResponse.from(producto);
    }

    @Transactional
    public void desactivar(Long id) {
        buscarOFallar(id).desactivar();
    }

    private Producto buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + id));
    }
}
