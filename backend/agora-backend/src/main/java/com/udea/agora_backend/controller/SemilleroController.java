package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.EstudianteSemilleroRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorSemilleroRequestDTO;
import com.udea.agora_backend.dto.request.SemilleroLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.SemilleroRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.service.SemilleroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/semilleros")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Semilleros", description = "Endpoints para administración de semilleros, líneas, co-tutoría docente y vinculación estudiantil")
public class SemilleroController {

    private final SemilleroService semilleroService;

    @Operation(summary = "Obtener todos los semilleros")
    @ApiResponse(responseCode = "200", description = "Lista de semilleros obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<SemilleroResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(semilleroService.obtenerTodos());
    }

    @Operation(summary = "Obtener un semillero por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Semillero encontrado"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SemilleroResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(semilleroService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener un semillero por su código identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Semillero encontrado"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<SemilleroResponseDTO> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(semilleroService.obtenerPorCodigo(codigo));
    }

    @Operation(summary = "Obtener semilleros por programa académico")
    @ApiResponse(responseCode = "200", description = "Lista de semilleros obtenida exitosamente")
    @GetMapping("/programa/{idPrograma}")
    public ResponseEntity<List<SemilleroResponseDTO>> obtenerPorPrograma(@PathVariable Integer idPrograma) {
        return ResponseEntity.ok(semilleroService.obtenerPorPrograma(idPrograma));
    }

    @Operation(summary = "Obtener semilleros por estado")
    @ApiResponse(responseCode = "200", description = "Lista de semilleros obtenida exitosamente")
    @GetMapping("/estado/{idEstado}")
    public ResponseEntity<List<SemilleroResponseDTO>> obtenerPorEstado(@PathVariable Integer idEstado) {
        return ResponseEntity.ok(semilleroService.obtenerPorEstado(idEstado));
    }

    @Operation(summary = "Crear un nuevo semillero de investigación")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Semillero creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudiante líder o programa no encontrado"),
            @ApiResponse(responseCode = "409", description = "Código identificador ya registrado")
    })
    @PostMapping
    public ResponseEntity<SemilleroResponseDTO> crear(@Valid @RequestBody SemilleroRequestDTO request) {
        SemilleroResponseDTO creado = semilleroService.crear(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar información de un semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Semillero actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado"),
            @ApiResponse(responseCode = "409", description = "Código identificador ya en uso")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SemilleroResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody SemilleroRequestDTO request) {
        return ResponseEntity.ok(semilleroService.actualizar(id, request));
    }

    @Operation(summary = "Actualizar el estado de un semillero (Activo, En pausa, etc.)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del semillero actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Semillero o estado no encontrado")
    })
    @PatchMapping("/{id}/estado/{idEstado}")
    public ResponseEntity<SemilleroResponseDTO> actualizarEstado(
            @PathVariable Integer id,
            @PathVariable Integer idEstado) {
        return ResponseEntity.ok(semilleroService.actualizarEstado(id, idEstado));
    }

    @Operation(summary = "Eliminar un semillero por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Semillero eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        semilleroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // LÍNEAS DE INVESTIGACIÓN DEL SEMILLERO
    // ==========================================

    @Operation(summary = "Obtener las líneas de investigación asociadas a un semillero")
    @ApiResponse(responseCode = "200", description = "Lista de líneas de investigación obtenida exitosamente")
    @GetMapping("/{id}/lineas-investigacion")
    public ResponseEntity<List<SemilleroLineaInvestigacionResponseDTO>> obtenerLineasInvestigacion(@PathVariable Integer id) {
        return ResponseEntity.ok(semilleroService.obtenerLineasInvestigacion(id));
    }

    @Operation(summary = "Asociar una línea de investigación al semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Línea de investigación asociada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero o línea no encontrada"),
            @ApiResponse(responseCode = "409", description = "La línea ya estaba asociada a este semillero")
    })
    @PostMapping("/lineas-investigacion")
    public ResponseEntity<SemilleroLineaInvestigacionResponseDTO> agregarLineaInvestigacion(
            @Valid @RequestBody SemilleroLineaInvestigacionRequestDTO request) {
        SemilleroLineaInvestigacionResponseDTO creada = semilleroService.agregarLineaInvestigacion(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Desasociar una línea de investigación del semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Línea removida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idSemillero}/lineas-investigacion/{idLinea}")
    public ResponseEntity<Void> eliminarLineaInvestigacion(
            @PathVariable Integer idSemillero,
            @PathVariable Integer idLinea) {
        semilleroService.eliminarLineaInvestigacion(idSemillero, idLinea);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // CO-TUTORÍA Y GOBERNANZA (PROFESORES)
    // ==========================================

    @Operation(summary = "Obtener profesores tutores y co-tutores de un semillero")
    @ApiResponse(responseCode = "200", description = "Lista de profesores tutores obtenida exitosamente")
    @GetMapping("/{id}/profesores")
    public ResponseEntity<List<ProfesorSemilleroResponseDTO>> obtenerProfesores(@PathVariable Integer id) {
        return ResponseEntity.ok(semilleroService.obtenerProfesores(id));
    }

    @Operation(summary = "Asignar un profesor como tutor o co-tutor del semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profesor asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero o profesor no encontrado"),
            @ApiResponse(responseCode = "409", description = "El profesor ya está asignado a este semillero")
    })
    @PostMapping("/profesores")
    public ResponseEntity<ProfesorSemilleroResponseDTO> asignarProfesor(
            @Valid @RequestBody ProfesorSemilleroRequestDTO request) {
        ProfesorSemilleroResponseDTO creada = semilleroService.asignarProfesor(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Remover un profesor de la tutoría del semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profesor desvinculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idSemillero}/profesores/{idProfesor}")
    public ResponseEntity<Void> removerProfesor(
            @PathVariable Integer idSemillero,
            @PathVariable Integer idProfesor) {
        semilleroService.removerProfesor(idSemillero, idProfesor);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // VINCULACIÓN DE ESTUDIANTES AL SEMILLERO
    // ==========================================

    @Operation(summary = "Obtener los estudiantes vinculados a un semillero")
    @ApiResponse(responseCode = "200", description = "Lista de estudiantes vinculados obtenida exitosamente")
    @GetMapping("/{id}/estudiantes")
    public ResponseEntity<List<EstudianteSemilleroResponseDTO>> obtenerEstudiantes(@PathVariable Integer id) {
        return ResponseEntity.ok(semilleroService.obtenerEstudiantes(id));
    }

    @Operation(summary = "Vincular un estudiante a un semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante vinculado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero, estudiante o estado no encontrado"),
            @ApiResponse(responseCode = "409", description = "El estudiante ya está vinculado a este semillero")
    })
    @PostMapping("/estudiantes")
    public ResponseEntity<EstudianteSemilleroResponseDTO> vincularEstudiante(
            @Valid @RequestBody EstudianteSemilleroRequestDTO request) {
        EstudianteSemilleroResponseDTO vinculacion = semilleroService.vincularEstudiante(request);
        return new ResponseEntity<>(vinculacion, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar estado y/o fecha de salida de vinculación de un estudiante en un semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vinculación actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vinculación no encontrada")
    })
    @PutMapping("/{idSemillero}/estudiantes/{idEstudiante}")
    public ResponseEntity<EstudianteSemilleroResponseDTO> actualizarVinculacionEstudiante(
            @PathVariable Integer idSemillero,
            @PathVariable Integer idEstudiante,
            @RequestParam Integer idEstado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {
        return ResponseEntity.ok(semilleroService.actualizarVinculacionEstudiante(idSemillero, idEstudiante, idEstado, fechaSalida));
    }
}

