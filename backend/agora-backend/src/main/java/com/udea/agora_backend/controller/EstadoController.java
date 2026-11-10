package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.EstadoRequestDTO;
import com.udea.agora_backend.dto.response.EstadoResponseDTO;
import com.udea.agora_backend.service.EstadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lectura para cualquier usuario autenticado; escritura solo para el administrador (SecurityConfig). */
@RestController
@RequestMapping("/api/estados")
@RequiredArgsConstructor
@Tag(name = "Estados", description = "Catálogo de estados del sistema, agrupados por categoría")
public class EstadoController {

    private final EstadoService estadoService;

    @Operation(summary = "Listar estados")
    @GetMapping
    public ResponseEntity<List<EstadoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(estadoService.obtenerTodos());
    }

    @Operation(summary = "Obtener por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EstadoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estadoService.obtenerPorId(id));
    }

    @Operation(summary = "Estados de una categoría (ej. 'Estado Semillero')")
    @GetMapping("/categoria")
    public ResponseEntity<List<EstadoResponseDTO>> obtenerPorCategoria(@RequestParam String nombre) {
        return ResponseEntity.ok(estadoService.obtenerPorCategoria(nombre));
    }

    @Operation(summary = "Estados con un nombre exacto (uno por categoría)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estados encontrados"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<List<EstadoResponseDTO>> obtenerPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(estadoService.obtenerPorNombre(nombre));
    }

    @Operation(summary = "Crear (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PostMapping
    public ResponseEntity<EstadoResponseDTO> crear(@Valid @RequestBody EstadoRequestDTO request) {
        return new ResponseEntity<>(estadoService.crear(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EstadoResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoRequestDTO request) {
        return ResponseEntity.ok(estadoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
