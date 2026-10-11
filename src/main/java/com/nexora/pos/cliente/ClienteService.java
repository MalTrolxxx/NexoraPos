package com.nexora.pos.cliente;

import com.nexora.pos.cliente.dto.ClienteRequest;
import com.nexora.pos.cliente.dto.ClienteResponse;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reglas de negocio de clientes: el teléfono, si se da, no se puede repetir. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository repository;

    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(ClienteResponse::from).toList();
    }

    public ClienteResponse obtener(Long id) {
        return ClienteResponse.from(buscarOFallar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String telefono = normalizarTelefono(request.telefono());
        if (telefono != null && repository.existsByTelefono(telefono)) {
            throw telefonoRepetido(telefono);
        }
        return ClienteResponse.from(repository.save(new Cliente(request.nombre(), telefono)));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarOFallar(id);
        String telefono = normalizarTelefono(request.telefono());
        if (telefono != null && repository.existsByTelefonoAndIdNot(telefono, id)) {
            throw telefonoRepetido(telefono);
        }
        cliente.actualizarDatos(request.nombre(), telefono);
        return ClienteResponse.from(cliente);
    }

    /** Un teléfono vacío se guarda como null; si no, dos clientes sin teléfono chocarían con el índice único. */
    private String normalizarTelefono(String telefono) {
        return (telefono == null || telefono.isBlank()) ? null : telefono.trim();
    }

    private ReglaDeNegocioException telefonoRepetido(String telefono) {
        return new ReglaDeNegocioException("Ya existe un cliente con el teléfono \"" + telefono + "\"");
    }

    private Cliente buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente con id " + id));
    }
}
