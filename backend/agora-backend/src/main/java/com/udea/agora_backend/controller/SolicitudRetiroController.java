package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ComentarioRequestDTO;
import com.udea.agora_backend.dto.request.RechazoRequestDTO;
import com.udea.agora_backend.dto.request.SolicitudRetiroRequestDTO;
import com.udea.agora_backend.dto.response.SolicitudRetiroResponseDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.service.SolicitudRetiroService;
import com.udea.agora_backend.service.autorizacion.AutorizacionSemilleroService;
import com.udea.agora_backend.service.autorizacion.UsuarioAutenticadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-retiro")
@RequiredArgsConstructor
@Tag(name = "Retiro voluntario", description = "Un integrante solicita salir de un semillero y un líder lo aprueba o rechaza")
public class SolicitudRetiroController {

    private final SolicitudRetiroService solicitudRetiroService;
    private final UsuarioAutenticadoService usuarioAutenticado;
    private final AutorizacionSemilleroService autorizacionSemillero;

    @Operation(summary = "Solicitar el retiro de un semillero (integrante activo)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud enviada a los líderes del semillero"),
            @ApiResponse(responseCode = "422", description = "No es integrante activo, es el líder o ya tiene una solicitud pendiente")
    })
    @PostMapping
    public ResponseEntity<SolicitudRetiroResponseDTO> solicitar(@Valid @RequestBody SolicitudRetiroRequestDTO request,
                                                                Authentication authentication) {
        return new ResponseEntity<>(solicitudRetiroService.solicitar(
                idEstudianteActual(authentication), request.getIdSemillero(), request.getMotivo()), HttpStatus.CREATED);
    }

    @Operation(summary = "Mis solicitudes de retiro (estudiante de la sesión)")
    @GetMapping("/mias")
    public ResponseEntity<List<SolicitudRetiroResponseDTO>> mias(Authentication authentication) {
        return ResponseEntity.ok(solicitudRetiroService.delEstudiante(idEstudianteActual(authentication)));
    }

    @Operation(summary = "Solicitudes de retiro de un semillero (líderes, tutores y administrador)")
    @GetMapping("/semillero/{idSemillero}")
    public ResponseEntity<List<SolicitudRetiroResponseDTO>> delSemillero(@PathVariable Integer idSemillero,
                                                                         Authentication authentication) {
        autorizacionSemillero.validarLiderOAdministrador(authentication.getName(), idSemillero);
        return ResponseEntity.ok(solicitudRetiroService.delSemillero(idSemillero));
    }

    @Operation(summary = "Aprobar una solicitud de retiro: el integrante queda retirado (líder del semillero)")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudRetiroResponseDTO> aprobar(@PathVariable Integer id,
                                                              @Valid @RequestBody(required = false) ComentarioRequestDTO request,
                                                              Authentication authentication) {
        autorizacionSemillero.validarLiderSemillero(authentication.getName(), solicitudRetiroService.idSemilleroDe(id));
        return ResponseEntity.ok(solicitudRetiroService.aprobar(id, usuarioAutenticado.usuario(authentication.getName()),
                request != null ? request.getComentario() : null));
    }

    @Operation(summary = "Rechazar una solicitud de retiro con un comentario (líder del semillero)")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudRetiroResponseDTO> rechazar(@PathVariable Integer id,
                                                               @Valid @RequestBody RechazoRequestDTO request,
                                                               Authentication authentication) {
        autorizacionSemillero.validarLiderSemillero(authentication.getName(), solicitudRetiroService.idSemilleroDe(id));
        return ResponseEntity.ok(solicitudRetiroService.rechazar(id, usuarioAutenticado.usuario(authentication.getName()),
                request.getMotivo()));
    }

    private Integer idEstudianteActual(Authentication authentication) {
        return usuarioAutenticado.obtenerIdEstudiante(authentication.getName())
                .orElseThrow(() -> new ErrorLogicaNegocioException("Solo un estudiante puede solicitar su retiro."));
    }
}
