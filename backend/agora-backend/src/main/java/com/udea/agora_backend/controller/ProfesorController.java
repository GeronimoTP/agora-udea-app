package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ProfesorAreaEspecialidadRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorRequestDTO;
import com.udea.agora_backend.dto.response.ProfesorAreaEspecialidadResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorResumenDTO;
import com.udea.agora_backend.service.ProfesorService;
import com.udea.agora_backend.service.autorizacion.AutorizacionPerfilService;
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
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@Tag(name = "Profesores", description = "Endpoints para la gestión de perfiles de profesores y sus áreas de especialidad")
public class ProfesorController {

    private final ProfesorService profesorService;
    private final AutorizacionPerfilService autorizacionPerfil;
    private final UsuarioAutenticadoService usuarioAutenticado;

    @Operation(summary = "Listado de profesores sin correo, para elegir tutores")
    @ApiResponse(responseCode = "200", description = "Lista de profesores obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<ProfesorResumenDTO>> obtenerTodos() {
        return ResponseEntity.ok(profesorService.obtenerResumenes());
    }

    @Operation(summary = "Mi perfil de profesor (el de la sesión)")
    @GetMapping("/me")
    public ResponseEntity<ProfesorResponseDTO> miPerfil(Authentication authentication) {
        return ResponseEntity.ok(profesorService.obtenerPorId(usuarioAutenticado.idProfesorActual(authentication.getName())));
    }

    @Operation(summary = "Actualizar mi perfil de profesor (el de la sesión)")
    @PutMapping("/me")
    public ResponseEntity<ProfesorResponseDTO> actualizarMiPerfil(@Valid @RequestBody ProfesorRequestDTO request,
                                                                  Authentication authentication) {
        Integer idProfesor = usuarioAutenticado.idProfesorActual(authentication.getName());
        return ResponseEntity.ok(profesorService.actualizar(idProfesor, request));
    }

    @Operation(summary = "Perfil completo de un profesor (él mismo o el administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profesor encontrado"),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponseDTO> obtenerPorId(@PathVariable Integer id, Authentication authentication) {
        autorizacionPerfil.validarLecturaProfesor(authentication.getName(), id);
        return ResponseEntity.ok(profesorService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener un profesor por el ID de su usuario asociado (administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profesor encontrado"),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado para ese usuario")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<ProfesorResponseDTO> obtenerPorIdUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(profesorService.obtenerPorIdUsuario(idUsuario));
    }

    @Operation(summary = "Crear un perfil de profesor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profesor creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario o programa académico no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene un perfil de profesor asignado")
    })
    @PostMapping
    public ResponseEntity<ProfesorResponseDTO> crear(@Valid @RequestBody ProfesorRequestDTO request) {
        ProfesorResponseDTO creado = profesorService.crear(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }


    @Operation(summary = "Eliminar un perfil de profesor por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profesor eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, Authentication authentication) {
        autorizacionPerfil.validarProfesorActual(authentication.getName(), id);
        profesorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // ÁREAS DE ESPECIALIDAD
    // ==========================================

    @Operation(summary = "Obtener las áreas de especialidad de un profesor")
    @ApiResponse(responseCode = "200", description = "Lista de áreas de especialidad obtenida exitosamente")
    @GetMapping("/{id}/areas-especialidad")
    public ResponseEntity<List<ProfesorAreaEspecialidadResponseDTO>> obtenerAreasEspecialidad(
            @PathVariable Integer id, Authentication authentication) {
        autorizacionPerfil.validarLecturaProfesor(authentication.getName(), id);
        return ResponseEntity.ok(profesorService.obtenerAreasEspecialidad(id));
    }

    @Operation(summary = "Asociar un área de especialidad al profesor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Área de especialidad asociada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Profesor o área de especialidad no encontrada"),
            @ApiResponse(responseCode = "409", description = "El área ya estaba asociada a este profesor")
    })
    @PostMapping("/areas-especialidad")
    public ResponseEntity<ProfesorAreaEspecialidadResponseDTO> agregarAreaEspecialidad(
            @Valid @RequestBody ProfesorAreaEspecialidadRequestDTO request,
            Authentication authentication) {
        autorizacionPerfil.validarProfesorActual(authentication.getName(), request.getIdProfesor());
        ProfesorAreaEspecialidadResponseDTO creada = profesorService.agregarAreaEspecialidad(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Desasociar un área de especialidad del profesor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Área de especialidad removida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idProfesor}/areas-especialidad/{idArea}")
    public ResponseEntity<Void> eliminarAreaEspecialidad(
            @PathVariable Integer idProfesor,
            @PathVariable Integer idArea,
            Authentication authentication) {
        autorizacionPerfil.validarProfesorActual(authentication.getName(), idProfesor);
        profesorService.eliminarAreaEspecialidad(idProfesor, idArea);
        return ResponseEntity.noContent().build();
    }
}

