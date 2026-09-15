package com.udea.agora_backend.controller;

import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.service.AreaEspecialidadService;
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
@RequestMapping("/api/areas-especialidad")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Áreas de Especialidad", description = "Endpoints para el catálogo de áreas de especialidad docente")
public class AreaEspecialidadController {

    private final AreaEspecialidadService areaEspecialidadService;

    @Operation(summary = "Obtener todas las áreas de especialidad")
    @ApiResponse(responseCode = "200", description = "Lista de áreas de especialidad obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<AreaEspecialidad>> obtenerTodas() {
        return ResponseEntity.ok(areaEspecialidadService.obtenerTodas());
    }

    @Operation(summary = "Obtener un área de especialidad por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Área de especialidad encontrada"),
            @ApiResponse(responseCode = "404", description = "Área de especialidad no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AreaEspecialidad> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(areaEspecialidadService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar áreas de especialidad por coincidencia en su nombre")
    @ApiResponse(responseCode = "200", description = "Áreas coincidentes obtenidas exitosamente")
    @GetMapping("/buscar")
    public ResponseEntity<List<AreaEspecialidad>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(areaEspecialidadService.obtenerPorNombreContiene(nombre));
    }

    @Operation(summary = "Crear una nueva área de especialidad")
    @ApiResponse(responseCode = "201", description = "Área de especialidad creada exitosamente")
    @PostMapping
    public ResponseEntity<AreaEspecialidad> crear(@RequestBody AreaEspecialidad area) {
        return new ResponseEntity<>(areaEspecialidadService.crear(area), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un área de especialidad existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Área de especialidad actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Área de especialidad no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AreaEspecialidad> actualizar(
            @PathVariable Integer id,
            @RequestBody AreaEspecialidad area) {
        return ResponseEntity.ok(areaEspecialidadService.actualizar(id, area));
    }

    @Operation(summary = "Eliminar un área de especialidad por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Área de especialidad eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Área de especialidad no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        areaEspecialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

