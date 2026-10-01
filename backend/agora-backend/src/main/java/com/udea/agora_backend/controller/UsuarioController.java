package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.UsuarioRequestDTO;
import com.udea.agora_backend.dto.response.UsuarioResponseDTO;
import com.udea.agora_backend.dto.response.UsuarioSesionResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.service.UsuarioService;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Usuarios", description = "Endpoints para la gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;

    @GetMapping("/me")
    public ResponseEntity<UsuarioSesionResponseDTO> obtenerSesion(Authentication authentication) {
    Usuario usuario = usuarioRepository.findWithRolByEmail(authentication.getName())
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "email", authentication.getName()));

    return ResponseEntity.ok(UsuarioSesionResponseDTO.builder()
        .idUsuario(usuario.getId())
        .nombreCompleto(usuario.getNombreCompleto())
        .email(usuario.getEmail())
        .rol(usuario.getRol().getNombre())
        .idEstudiante(estudianteRepository.findByUsuarioId(usuario.getId())
            .map(estudiante -> estudiante.getId()).orElse(null))
        .idProfesor(profesorRepository.findByUsuarioId(usuario.getId())
            .map(profesor -> profesor.getId()).orElse(null))
        .idSemillerosLiderados(profesorRepository.findByUsuarioId(usuario.getId())
            .map(profesor -> profesorSemilleroRepository.findSemilleroIdsByProfesorId(profesor.getId()))
            .orElseGet(List::of))
        .build());
    }

    @Operation(summary = "Obtener todos los usuarios")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @Operation(summary = "Obtener un usuario por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener un usuario por su correo electrónico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorEmail(@PathVariable String email) {
        return ResponseEntity.ok(usuarioService.obtenerPorEmail(email));
    }

    @Operation(summary = "Obtener un usuario por su ID de OAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/oauth/{oauthId}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorOauthId(@PathVariable String oauthId) {
        return ResponseEntity.ok(usuarioService.obtenerPorOauthId(oauthId));
    }

    @Operation(summary = "Crear un nuevo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto: email u oauthId ya registrado")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
        UsuarioResponseDTO usuarioCreado = usuarioService.crear(request);
        return new ResponseEntity<>(usuarioCreado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto: email ya en uso por otro usuario")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un usuario por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

