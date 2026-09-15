package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ConvocatoriaRequestDTO;
import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.service.ConvocatoriaService;
import com.udea.agora_backend.service.GestorConvocatoriasService;
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
@RequestMapping("/api/convocatorias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Convocatorias", description = "Endpoints para la gestión del ciclo de vida de las convocatorias de semilleros")
public class ConvocatoriaController {

    private final ConvocatoriaService convocatoriaService;
    private final GestorConvocatoriasService gestorConvocatoriasService;

    @Operation(summary = "Obtener todas las convocatorias")
    @ApiResponse(responseCode = "200", description = "Lista de convocatorias obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<ConvocatoriaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(convocatoriaService.obtenerTodas());
    }

    @Operation(summary = "Obtener una convocatoria por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConvocatoriaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(convocatoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener convocatorias activas con cupos disponibles")
    @ApiResponse(responseCode = "200", description = "Lista de convocatorias activas obtenida exitosamente")
    @GetMapping("/activas")
    public ResponseEntity<List<ConvocatoriaResponseDTO>> obtenerActivasConCupos() {
        return ResponseEntity.ok(convocatoriaService.obtenerActivasConCupos());
    }

    @Operation(summary = "Obtener convocatorias de un semillero específico")
    @ApiResponse(responseCode = "200", description = "Lista de convocatorias del semillero obtenida exitosamente")
    @GetMapping("/semillero/{idSemillero}")
    public ResponseEntity<List<ConvocatoriaResponseDTO>> obtenerPorSemillero(@PathVariable Integer idSemillero) {
        return ResponseEntity.ok(convocatoriaService.obtenerPorSemillero(idSemillero));
    }

    @Operation(summary = "Consultar el estado dinámico de cupos de una convocatoria en tiempo real")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de cupos obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada")
    })
    @GetMapping("/{id}/cupos")
    public ResponseEntity<GestorConvocatoriasService.ConvocatoriaEstadoCuposDTO> obtenerEstadoCupos(@PathVariable Integer id) {
        return ResponseEntity.ok(gestorConvocatoriasService.obtenerEstadoCupos(id));
    }

    @Operation(summary = "Crear una nueva convocatoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convocatoria creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @PostMapping
    public ResponseEntity<ConvocatoriaResponseDTO> crear(@Valid @RequestBody ConvocatoriaRequestDTO request) {
        ConvocatoriaResponseDTO creada = convocatoriaService.crear(request);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una convocatoria existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Convocatoria o semillero no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ConvocatoriaResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ConvocatoriaRequestDTO request) {
        return ResponseEntity.ok(convocatoriaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar una convocatoria por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convocatoria eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        convocatoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ejecutar manualmente el proceso de cierre de convocatorias expiradas y cancelación de ofertas vencidas")
    @ApiResponse(responseCode = "200", description = "Procesamiento de convocatorias expiradas ejecutado exitosamente")
    @PostMapping("/procesar-expiradas")
    public ResponseEntity<String> procesarConvocatoriasExpiradas() {
        gestorConvocatoriasService.procesarConvocatoriasExpiradas();
        return ResponseEntity.ok("Procesamiento de convocatorias expiradas completado exitosamente.");
    }
}

