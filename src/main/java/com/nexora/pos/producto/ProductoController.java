package com.nexora.pos.producto;

import com.nexora.pos.producto.dto.EntradaInventarioRequest;
import com.nexora.pos.producto.dto.ProductoRequest;
import com.nexora.pos.producto.dto.ProductoResponse;
import com.nexora.pos.shared.Pagina;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Capa delgada a propósito: solo traduce HTTP <-> llamadas al servicio. Cero lógica de negocio aquí. */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    /** GET /api/productos?q=coca&page=0&size=20 — busca por nombre o código; sin `q` lista todo. */
    @GetMapping
    public Pagina<ProductoResponse> buscar(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos,
                                           @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return service.buscar(q, incluirInactivos, pageable);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Lo que llama la caja cuando el lector escanea un código. */
    @GetMapping("/codigo/{codigoBarras}")
    public ProductoResponse obtenerPorCodigo(@PathVariable String codigoBarras) {
        return service.obtenerPorCodigo(codigoBarras);
    }

    @GetMapping("/bajo-stock")
    public List<ProductoResponse> bajoStock() {
        return service.bajoStock();
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/entradas")
    public ProductoResponse registrarEntrada(@PathVariable Long id, @Valid @RequestBody EntradaInventarioRequest request) {
        return service.registrarEntrada(id, request.cantidad());
    }

    @PostMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable Long id) {
        service.activar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
