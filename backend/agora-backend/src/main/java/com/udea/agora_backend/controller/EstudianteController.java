package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.EstudianteLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.EstudianteRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.service.EstudianteService;
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
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Estudiantes", description = "Endpoints para la gestión de estudiantes y sus líneas de investigación de interés")
public class EstudianteController {

    private final EstudianteService estudianteService;

    @Operation(summary = "Obtener todos los estudiantes")
    @ApiResponse(responseCode = "200", description = "Lista de estudiantes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<EstudianteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(estudianteService.obtenerTodos());
    }

    @Operation(summary = "Obtener un estudiante por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante encontrado"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EstudianteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener un estudiante por el ID de su usuario asociado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante encontrado"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado para ese usuario")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<EstudianteResponseDTO> obtenerPorIdUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(estudianteService.obtenerPorIdUsuario(idUsuario));
    }

    @Operation(summary = "Crear un perfil de estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario o programa académico no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene un perfil de estudiante asignado")
    })
    @PostMapping
    public ResponseEntity<EstudianteResponseDTO> crear(@Valid @RequestBody EstudianteRequestDTO request) {
        EstudianteResponseDTO creado = estudianteService.crear(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar información de un estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudiante o programa académico no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EstudianteResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EstudianteRequestDTO request) {
        return ResponseEntity.ok(estudianteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un perfil de estudiante por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estudiante eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // LÍNEAS DE INVESTIGACIÓN DE INTERÉS
    // ==========================================

    @Operation(summary = "Obtener las líneas de investigación de interés de un estudiante")
    @ApiResponse(responseCode = "200", description = "Lista de líneas de investigación obtenida exitosamente")
    @GetMapping("/{id}/lineas-investigacion")
    public ResponseEntity<List<EstudianteLineaInvestigacionResponseDTO>> obtenerLineasInvestigacion(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerLineasInvestigacion(id));
    }

    @Operation(summary = "Asociar una línea de investigación de interés al perfil del estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Línea de investigación asociada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudiante o línea de investigación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La línea de investigación ya estaba asociada a este estudiante")
    })
    @PostMapping("/lineas-investigacion")
    public ResponseEntity<EstudianteLineaInvestigacionResponseDTO> agregarLineaInvestigacion(
            @Valid @RequestBody EstudianteLineaInvestigacionRequestDTO request) {
        EstudianteLineaInvestigacionResponseDTO creada = estudianteService.agregarLineaInvestigacion(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Desasociar una línea de investigación del perfil del estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Línea de investigación removida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idEstudiante}/lineas-investigacion/{idLinea}")
    public ResponseEntity<Void> eliminarLineaInvestigacion(
            @PathVariable Integer idEstudiante,
            @PathVariable Integer idLinea) {
        estudianteService.eliminarLineaInvestigacion(idEstudiante, idLinea);
        return ResponseEntity.noContent().build();
    }
}

