package com.udea.agora_backend.controller;

import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.service.HabilidadService;
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
@RequestMapping("/api/habilidades")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Habilidades", description = "Endpoints para el catálogo vivo de habilidades técnicas y blandas (Folksonomía)")
public class HabilidadController {

    private final HabilidadService habilidadService;

    @Operation(summary = "Obtener todas las habilidades del catálogo")
    @ApiResponse(responseCode = "200", description = "Lista de habilidades obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Habilidad>> obtenerTodas() {
        return ResponseEntity.ok(habilidadService.obtenerTodas());
    }

    @Operation(summary = "Obtener una habilidad por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habilidad encontrada"),
            @ApiResponse(responseCode = "404", description = "Habilidad no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Habilidad> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(habilidadService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar habilidades por coincidencia de nombre (Predictivo / Autocompletado)")
    @ApiResponse(responseCode = "200", description = "Habilidades coincidentes obtenidas exitosamente")
    @GetMapping("/buscar")
    public ResponseEntity<List<Habilidad>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(habilidadService.obtenerPorNombreContiene(nombre));
    }

    @Operation(summary = "Crear una nueva habilidad en el catálogo")
    @ApiResponse(responseCode = "201", description = "Habilidad creada exitosamente")
    @PostMapping
    public ResponseEntity<Habilidad> crear(@RequestBody Habilidad habilidad) {
        return new ResponseEntity<>(habilidadService.crear(habilidad), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una habilidad existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habilidad actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Habilidad no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Habilidad> actualizar(@PathVariable Integer id, @RequestBody Habilidad habilidad) {
        return ResponseEntity.ok(habilidadService.actualizar(id, habilidad));
    }

    @Operation(summary = "Eliminar una habilidad por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habilidad eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Habilidad no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        habilidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

