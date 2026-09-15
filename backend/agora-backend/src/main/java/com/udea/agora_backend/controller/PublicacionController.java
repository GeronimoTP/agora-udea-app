package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.PublicacionRequestDTO;
import com.udea.agora_backend.dto.request.PublicacionUsuarioRequestDTO;
import com.udea.agora_backend.dto.response.PublicacionResponseDTO;
import com.udea.agora_backend.dto.response.PublicacionUsuarioResponseDTO;
import com.udea.agora_backend.service.PublicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Publicaciones", description = "Endpoints para el registro de producción científica, papers, enlaces DOI y autorías normalizadas")
public class PublicacionController {

    private final PublicacionService publicacionService;

    @Operation(summary = "Obtener todas las publicaciones académicas")
    @ApiResponse(responseCode = "200", description = "Lista de publicaciones obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(publicacionService.obtenerTodas());
    }

    @Operation(summary = "Obtener una publicación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación encontrada"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(publicacionService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener publicaciones generadas por un proyecto de investigación")
    @ApiResponse(responseCode = "200", description = "Lista de publicaciones del proyecto obtenida exitosamente")
    @GetMapping("/proyecto/{idProyecto}")
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerPorProyecto(@PathVariable Integer idProyecto) {
        return ResponseEntity.ok(publicacionService.obtenerPorProyecto(idProyecto));
    }

    @Operation(summary = "Registrar una nueva publicación académica")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PostMapping
    public ResponseEntity<PublicacionResponseDTO> crear(@Valid @RequestBody PublicacionRequestDTO request) {
        PublicacionResponseDTO creada = publicacionService.crear(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar información de una publicación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Publicación o proyecto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PublicacionRequestDTO request) {
        return ResponseEntity.ok(publicacionService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar una publicación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        publicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // GESTIÓN DE AUTORES
    // ==========================================

    @Operation(summary = "Obtener autores asociados a una publicación")
    @ApiResponse(responseCode = "200", description = "Lista de autores obtenida exitosamente")
    @GetMapping("/{id}/autores")
    public ResponseEntity<List<PublicacionUsuarioResponseDTO>> obtenerAutores(@PathVariable Integer id) {
        return ResponseEntity.ok(publicacionService.obtenerAutores(id));
    }

    @Operation(summary = "Asociar un autor a una publicación con su rol de autoría (ej. Autor Principal, Coautor)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autor asociado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Publicación, usuario o rol de autor no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya está asociado como autor de esta publicación")
    })
    @PostMapping("/autores")
    public ResponseEntity<PublicacionUsuarioResponseDTO> asociarAutor(
            @Valid @RequestBody PublicacionUsuarioRequestDTO request) {
        PublicacionUsuarioResponseDTO asociacion = publicacionService.asociarAutor(request);
        return new ResponseEntity<>(asociacion, HttpStatus.CREATED);
    }

    @Operation(summary = "Remover un autor de una publicación")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Autor removido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idPublicacion}/autores/{idUsuario}")
    public ResponseEntity<Void> removerAutor(
            @PathVariable Integer idPublicacion,
            @PathVariable Integer idUsuario) {
        publicacionService.removerAutor(idPublicacion, idUsuario);
        return ResponseEntity.noContent().build();
    }
}

