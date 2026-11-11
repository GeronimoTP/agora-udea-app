package com.udea.agora_backend.controller;

import com.udea.agora_backend.dominio.EstadosSemillero;
import com.udea.agora_backend.dto.response.PendientesResponseDTO;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.LiderazgoSemilleroService;
import com.udea.agora_backend.service.SolicitudCatalogoService;
import com.udea.agora_backend.service.SolicitudRetiroService;
import com.udea.agora_backend.service.autorizacion.UsuarioAutenticadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pendientes")
@RequiredArgsConstructor
@Tag(name = "Pendientes", description = "Avisos de solicitudes que esperan respuesta del usuario de la sesión")
public class PendientesController {

    private final UsuarioAutenticadoService usuarioAutenticado;
    private final LiderazgoSemilleroService liderazgoSemilleroService;
    private final SemilleroRepository semilleroRepository;
    private final SolicitudCatalogoService solicitudCatalogoService;
    private final SolicitudRetiroService solicitudRetiroService;

    @Operation(summary = "Cuántas solicitudes esperan respuesta del usuario de la sesión")
    @GetMapping
    public ResponseEntity<PendientesResponseDTO> obtener(Authentication authentication) {
        Usuario usuario = usuarioAutenticado.usuario(authentication.getName());
        if (usuarioAutenticado.esAdministrador(usuario)) {
            return ResponseEntity.ok(PendientesResponseDTO.builder()
                    .semillerosPorAprobar(semilleroRepository.findByNombreEstado(EstadosSemillero.PENDIENTE_APROBACION).size())
                    .solicitudesCatalogo(solicitudCatalogoService.contarPendientes())
                    .build());
        }
        return ResponseEntity.ok(PendientesResponseDTO.builder()
                .solicitudesRetiro(solicitudRetiroService.contarPendientes(
                        liderazgoSemilleroService.obtenerSemillerosLiderados(usuario)))
                .build());
    }
}
