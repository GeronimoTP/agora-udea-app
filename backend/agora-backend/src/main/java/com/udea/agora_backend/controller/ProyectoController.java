package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ProyectoRequestDTO;
import com.udea.agora_backend.dto.request.VinculacionProyectoRequestDTO;
import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.dto.response.VinculacionProyectoResponseDTO;
import com.udea.agora_backend.service.ProyectoService;
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
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Proyectos", description = "Endpoints para la gestión de proyectos de investigación y vinculación de estudiantes con roles específicos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    @Operation(summary = "Obtener todos los proyectos de investigación")
    @ApiResponse(responseCode = "200", description = "Lista de proyectos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<ProyectoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(proyectoService.obtenerTodos());
    }

    @Operation(summary = "Obtener un proyecto por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto encontrado"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener proyectos de un semillero específico")
    @ApiResponse(responseCode = "200", description = "Lista de proyectos del semillero obtenida exitosamente")
    @GetMapping("/semillero/{idSemillero}")
    public ResponseEntity<List<ProyectoResponseDTO>> obtenerPorSemillero(@PathVariable Integer idSemillero) {
        return ResponseEntity.ok(proyectoService.obtenerPorSemillero(idSemillero));
    }

    @Operation(summary = "Crear un nuevo proyecto de investigación")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @PostMapping
    public ResponseEntity<ProyectoResponseDTO> crear(@Valid @RequestBody ProyectoRequestDTO request) {
        ProyectoResponseDTO creado = proyectoService.crear(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un proyecto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Proyecto o semillero no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProyectoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProyectoRequestDTO request) {
        return ResponseEntity.ok(proyectoService.actualizar(id, request));
    }

    @Operation(summary = "Actualizar el estado de un proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del proyecto actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto o estado no encontrado")
    })
    @PatchMapping("/{id}/estado/{idEstado}")
    public ResponseEntity<ProyectoResponseDTO> actualizarEstado(
            @PathVariable Integer id,
            @PathVariable Integer idEstado) {
        return ResponseEntity.ok(proyectoService.actualizarEstado(id, idEstado));
    }

    @Operation(summary = "Eliminar un proyecto por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proyecto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        proyectoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // VINCULACIÓN DE ESTUDIANTES AL PROYECTO
    // ==========================================

    @Operation(summary = "Obtener los estudiantes vinculados a un proyecto con sus roles de trabajo")
    @ApiResponse(responseCode = "200", description = "Lista de estudiantes vinculados obtenida exitosamente")
    @GetMapping("/{id}/estudiantes")
    public ResponseEntity<List<VinculacionProyectoResponseDTO>> obtenerEstudiantesVinculados(@PathVariable Integer id) {
        return ResponseEntity.ok(proyectoService.obtenerEstudiantesVinculados(id));
    }

    @Operation(summary = "Vincular un estudiante a un proyecto de investigación con un rol específico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante vinculado exitosamente al proyecto"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Proyecto, estudiante o rol no encontrado"),
            @ApiResponse(responseCode = "409", description = "El estudiante ya está vinculado a este proyecto")
    })
    @PostMapping("/estudiantes")
    public ResponseEntity<VinculacionProyectoResponseDTO> vincularEstudiante(
            @Valid @RequestBody VinculacionProyectoRequestDTO request) {
        VinculacionProyectoResponseDTO vinculacion = proyectoService.vincularEstudiante(request);
        return new ResponseEntity<>(vinculacion, HttpStatus.CREATED);
    }

    @Operation(summary = "Desvincular un estudiante de un proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estudiante desvinculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vinculación no encontrada")
    })
    @DeleteMapping("/{idProyecto}/estudiantes/{idEstudiante}")
    public ResponseEntity<Void> desvincularEstudiante(
            @PathVariable Integer idProyecto,
            @PathVariable Integer idEstudiante) {
        proyectoService.desvincularEstudiante(idProyecto, idEstudiante);
        return ResponseEntity.noContent().build();
    }
}

