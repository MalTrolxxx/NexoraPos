package com.nexora.pos.venta;

import com.nexora.pos.venta.dto.AbrirVentaRequest;
import com.nexora.pos.venta.dto.AgregarProductoRequest;
import com.nexora.pos.venta.dto.CantidadRequest;
import com.nexora.pos.venta.dto.CobroRequest;
import com.nexora.pos.venta.dto.CorteResponse;
import com.nexora.pos.venta.dto.VentaResponse;
import com.nexora.pos.venta.dto.VentaResumenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Flujo típico en caja:
 * POST /api/ventas → POST /{id}/detalles (por cada escaneo) → POST /{id}/cobro.
 * Una venta puede quedarse abierta (en espera) mientras se atiende a otro cliente.
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService service;

    @PostMapping
    public ResponseEntity<VentaResponse> abrir(@RequestBody(required = false) AbrirVentaRequest request) {
        VentaResponse venta = service.abrir(request == null ? null : request.clienteId());
        return ResponseEntity.created(URI.create("/api/ventas/" + venta.id())).body(venta);
    }

    @GetMapping("/abiertas")
    public List<VentaResumenResponse> abiertas(@RequestParam(required = false) Long clienteId) {
        return service.abiertas(clienteId);
    }

    /** GET /api/ventas/corte?fecha=2026-10-11 (sin fecha: hoy). */
    @GetMapping("/corte")
    public CorteResponse corte(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.corte(fecha);
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/detalles")
    public VentaResponse agregar(@PathVariable Long id, @Valid @RequestBody AgregarProductoRequest request) {
        return service.agregar(id, request);
    }

    @PutMapping("/{id}/detalles/{detalleId}")
    public VentaResponse cambiarCantidad(@PathVariable Long id, @PathVariable Long detalleId,
                                         @Valid @RequestBody CantidadRequest request) {
        return service.cambiarCantidad(id, detalleId, request.cantidad());
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public VentaResponse quitar(@PathVariable Long id, @PathVariable Long detalleId) {
        return service.quitar(id, detalleId);
    }

    @PostMapping("/{id}/cobro")
    public VentaResponse cobrar(@PathVariable Long id, @Valid @RequestBody CobroRequest request) {
        return service.cobrar(id, request);
    }

    @PostMapping("/{id}/cancelar")
    public VentaResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
