package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.AprobacionCatalogoRequestDTO;
import com.udea.agora_backend.dto.request.RechazoRequestDTO;
import com.udea.agora_backend.dto.request.SolicitudCatalogoRequestDTO;
import com.udea.agora_backend.dto.response.SolicitudCatalogoResponseDTO;
import com.udea.agora_backend.service.SolicitudCatalogoService;
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

/** Rutas de administrador (listar, aprobar, rechazar) protegidas en SecurityConfig. */
@RestController
@RequestMapping("/api/solicitudes-catalogo")
@RequiredArgsConstructor
@Tag(name = "Solicitudes de catálogo", description = "Estudiantes y profesores proponen elementos del catálogo; el administrador decide")
public class SolicitudCatalogoController {

    private final SolicitudCatalogoService solicitudCatalogoService;
    private final UsuarioAutenticadoService usuarioAutenticado;

    @Operation(summary = "Solicitar un elemento nuevo del catálogo (estudiante o profesor)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud enviada al administrador"),
            @ApiResponse(responseCode = "409", description = "Ya existe en el catálogo o ya hay una solicitud pendiente igual")
    })
    @PostMapping
    public ResponseEntity<SolicitudCatalogoResponseDTO> crear(@Valid @RequestBody SolicitudCatalogoRequestDTO request,
                                                              Authentication authentication) {
        return new ResponseEntity<>(solicitudCatalogoService.crear(
                usuarioAutenticado.usuario(authentication.getName()), request), HttpStatus.CREATED);
    }

    @Operation(summary = "Mis solicitudes de catálogo (usuario de la sesión)")
    @GetMapping("/mias")
    public ResponseEntity<List<SolicitudCatalogoResponseDTO>> mias(Authentication authentication) {
        return ResponseEntity.ok(solicitudCatalogoService.delUsuario(usuarioAutenticado.usuario(authentication.getName())));
    }

    @Operation(summary = "Solicitudes de catálogo (administrador): pendientes por defecto, o todas")
    @GetMapping
    public ResponseEntity<List<SolicitudCatalogoResponseDTO>> listar(
            @RequestParam(defaultValue = "true") boolean soloPendientes) {
        return ResponseEntity.ok(solicitudCatalogoService.listar(soloPendientes));
    }

    @Operation(summary = "Aprobar una solicitud: crea el elemento en el catálogo (administrador)")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudCatalogoResponseDTO> aprobar(@PathVariable Integer id,
                                                                @Valid @RequestBody(required = false) AprobacionCatalogoRequestDTO request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(solicitudCatalogoService.aprobar(
                id, usuarioAutenticado.usuario(authentication.getName()), request));
    }

    @Operation(summary = "Rechazar una solicitud con un motivo (administrador)")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudCatalogoResponseDTO> rechazar(@PathVariable Integer id,
                                                                 @Valid @RequestBody RechazoRequestDTO request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(solicitudCatalogoService.rechazar(
                id, usuarioAutenticado.usuario(authentication.getName()), request.getMotivo()));
    }
}
