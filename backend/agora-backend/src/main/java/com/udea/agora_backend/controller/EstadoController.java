package com.udea.agora_backend.controller;

import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.service.EstadoService;
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
@RequestMapping("/api/estados")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Estados", description = "Endpoints para el catálogo de estados del sistema")
public class EstadoController {

    private final EstadoService estadoService;

    @Operation(summary = "Obtener todos los estados del sistema")
    @ApiResponse(responseCode = "200", description = "Lista de estados obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Estado>> obtenerTodos() {
        return ResponseEntity.ok(estadoService.obtenerTodos());
    }

    @Operation(summary = "Obtener un estado por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado encontrado"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Estado> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estadoService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener un estado por su nombre exacto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado encontrado"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<Estado> obtenerPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(estadoService.obtenerPorNombre(nombre));
    }

    @Operation(summary = "Crear un nuevo estado")
    @ApiResponse(responseCode = "201", description = "Estado creado exitosamente")
    @PostMapping
    public ResponseEntity<Estado> crear(@RequestBody Estado estado) {
        return new ResponseEntity<>(estadoService.crear(estado), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un estado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Estado> actualizar(
            @PathVariable Integer id,
            @RequestBody Estado estado) {
        return ResponseEntity.ok(estadoService.actualizar(id, estado));
    }

    @Operation(summary = "Eliminar un estado por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estado eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

