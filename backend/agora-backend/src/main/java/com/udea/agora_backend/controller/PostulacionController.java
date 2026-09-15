package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.PostulacionHabilidadRequestDTO;
import com.udea.agora_backend.dto.request.PostulacionRequestDTO;
import com.udea.agora_backend.dto.response.PostulacionHabilidadResponseDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.service.GestorConvocatoriasService;
import com.udea.agora_backend.service.PostulacionService;
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
@RequestMapping("/api/postulaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Postulaciones", description = "Endpoints para el motor de postulaciones concurrentes, habilidades y flujo de selección de doble vía")
public class PostulacionController {

    private final PostulacionService postulacionService;
    private final GestorConvocatoriasService gestorConvocatoriasService;

    @Operation(summary = "Obtener todas las postulaciones")
    @ApiResponse(responseCode = "200", description = "Lista de postulaciones obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<PostulacionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(postulacionService.obtenerTodas());
    }

    @Operation(summary = "Obtener una postulación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación encontrada"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PostulacionResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(postulacionService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener todas las postulaciones de un estudiante")
    @ApiResponse(responseCode = "200", description = "Lista de postulaciones del estudiante obtenida exitosamente")
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<PostulacionResponseDTO>> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(postulacionService.obtenerPorEstudiante(idEstudiante));
    }

    @Operation(summary = "Obtener postulaciones activas de un estudiante (Pendiente o Pre-aprobada)")
    @ApiResponse(responseCode = "200", description = "Lista de postulaciones activas obtenida exitosamente")
    @GetMapping("/estudiante/{idEstudiante}/activas")
    public ResponseEntity<List<PostulacionResponseDTO>> obtenerActivasPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(postulacionService.obtenerPostulacionesActivasByEstudiante(idEstudiante));
    }

    @Operation(summary = "Obtener postulaciones de una convocatoria")
    @ApiResponse(responseCode = "200", description = "Lista de postulaciones de la convocatoria obtenida exitosamente")
    @GetMapping("/convocatoria/{idConvocatoria}")
    public ResponseEntity<List<PostulacionResponseDTO>> obtenerPorConvocatoria(@PathVariable Integer idConvocatoria) {
        return ResponseEntity.ok(postulacionService.obtenerPorConvocatoria(idConvocatoria));
    }

    @Operation(summary = "Crear una nueva postulación a una convocatoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Postulación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudiante o convocatoria no encontrada"),
            @ApiResponse(responseCode = "409", description = "El estudiante ya tiene una postulación registrada para esta convocatoria")
    })
    @PostMapping
    public ResponseEntity<PostulacionResponseDTO> crear(@Valid @RequestBody PostulacionRequestDTO request) {
        PostulacionResponseDTO creada = postulacionService.crear(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar información de una postulación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PostulacionResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PostulacionRequestDTO request) {
        return ResponseEntity.ok(postulacionService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar una postulación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Postulación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        postulacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // HABILIDADES ASOCIADAS A LA POSTULACIÓN
    // ==========================================

    @Operation(summary = "Obtener las habilidades declaradas en una postulación")
    @ApiResponse(responseCode = "200", description = "Lista de habilidades obtenida exitosamente")
    @GetMapping("/{id}/habilidades")
    public ResponseEntity<List<PostulacionHabilidadResponseDTO>> obtenerHabilidades(@PathVariable Integer id) {
        return ResponseEntity.ok(postulacionService.obtenerHabilidades(id));
    }

    @Operation(summary = "Agregar una habilidad a la postulación")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Habilidad asociada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Postulación o habilidad no encontrada"),
            @ApiResponse(responseCode = "409", description = "La habilidad ya estaba asociada a esta postulación")
    })
    @PostMapping("/habilidades")
    public ResponseEntity<PostulacionHabilidadResponseDTO> agregarHabilidad(
            @Valid @RequestBody PostulacionHabilidadRequestDTO request) {
        PostulacionHabilidadResponseDTO creada = postulacionService.agregarHabilidad(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Remover una habilidad de la postulación")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habilidad removida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asociación no encontrada")
    })
    @DeleteMapping("/{idPostulacion}/habilidades/{idHabilidad}")
    public ResponseEntity<Void> eliminarHabilidad(
            @PathVariable Integer idPostulacion,
            @PathVariable Integer idHabilidad) {
        postulacionService.eliminarHabilidad(idPostulacion, idHabilidad);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // FLUJO DE SELECCIÓN DE DOBLE VÍA Y CASCADA
    // ==========================================

    @Operation(summary = "Acción del Líder: Pre-aprobar postulación para ofrecer el cupo al estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación pre-aprobada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "La postulación no se encuentra en estado 'Pendiente'")
    })
    @PostMapping("/{id}/pre-aprobar")
    public ResponseEntity<String> preAprobarPostulacion(@PathVariable Integer id) {
        gestorConvocatoriasService.preAprobarPostulacion(id);
        return ResponseEntity.ok("Postulación pre-aprobada con éxito. En espera de confirmación del estudiante.");
    }

    @Operation(summary = "Acción del Líder: Rechazar postulación en revisión inicial")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación rechazada por el líder"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "La postulación no se encuentra en estado 'Pendiente'")
    })
    @PostMapping("/{id}/rechazar-lider")
    public ResponseEntity<String> rechazarPostulacionLider(@PathVariable Integer id) {
        gestorConvocatoriasService.rechazarPostulacion(id);
        return ResponseEntity.ok("Postulación rechazada por el líder.");
    }

    @Operation(summary = "Acción del Estudiante: Confirmar aceptación de la oferta de admisión (Dispara decremento de cupos y cascada de rechazo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aceptación confirmada exitosamente y cascada ejecutada"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "La postulación no está pre-aprobada o no hay cupos disponibles")
    })
    @PostMapping("/{id}/confirmar-aceptacion")
    public ResponseEntity<String> confirmarAceptacion(@PathVariable Integer id) {
        gestorConvocatoriasService.confirmarAceptacion(id);
        return ResponseEntity.ok("Admisión aceptada exitosamente. Se ha descontado el cupo y liberado las demás postulaciones activas.");
    }

    @Operation(summary = "Acción del Estudiante: Rechazar oferta de admisión pre-aprobada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oferta rechazada por el estudiante"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "La postulación no se encuentra en estado 'Pre-aprobada'")
    })
    @PostMapping("/{id}/rechazar-estudiante")
    public ResponseEntity<String> rechazarOfertaEstudiante(@PathVariable Integer id) {
        gestorConvocatoriasService.rechazarOfertaEstudiante(id);
        return ResponseEntity.ok("Oferta rechazada por el estudiante exitosamente.");
    }
}

