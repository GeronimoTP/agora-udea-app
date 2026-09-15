package com.udea.agora_backend.controller;

import com.udea.agora_backend.model.LineaInvestigacion;
import com.udea.agora_backend.service.LineaInvestigacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lineas-investigacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Líneas de Investigación", description = "Endpoints para el catálogo de líneas de investigación institucionales")
public class LineaInvestigacionController {

    private final LineaInvestigacionService lineaInvestigacionService;

    @Operation(summary = "Obtener todas las líneas de investigación")
    @ApiResponse(responseCode = "200", description = "Lista de líneas de investigación obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<LineaInvestigacion>> obtenerTodas() {
        return ResponseEntity.ok(lineaInvestigacionService.obtenerTodas());
    }

    @Operation(summary = "Obtener una línea de investigación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Línea de investigación encontrada"),
            @ApiResponse(responseCode = "404", description = "Línea de investigación no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LineaInvestigacion> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(lineaInvestigacionService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar líneas de investigación por coincidencia en su nombre")
    @ApiResponse(responseCode = "200", description = "Líneas coincidentes obtenidas exitosamente")
    @GetMapping("/buscar")
    public ResponseEntity<List<LineaInvestigacion>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(lineaInvestigacionService.obtenerPorNombreContiene(nombre));
    }

    @Operation(summary = "Crear una nueva línea de investigación")
    @ApiResponse(responseCode = "201", description = "Línea de investigación creada exitosamente")
    @PostMapping
    public ResponseEntity<LineaInvestigacion> crear(@RequestBody LineaInvestigacion linea) {
        return new ResponseEntity<>(lineaInvestigacionService.crear(linea), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una línea de investigación existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Línea de investigación actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Línea de investigación no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<LineaInvestigacion> actualizar(
            @PathVariable Integer id,
            @RequestBody LineaInvestigacion linea) {
        return ResponseEntity.ok(lineaInvestigacionService.actualizar(id, linea));
    }

    @Operation(summary = "Eliminar una línea de investigación por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Línea de investigación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Línea de investigación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        lineaInvestigacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

