package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.HabilidadRequestDTO;
import com.udea.agora_backend.dto.response.HabilidadResponseDTO;
import com.udea.agora_backend.service.HabilidadService;
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
@RequestMapping("/api/habilidades")
@RequiredArgsConstructor
@Tag(name = "Habilidades", description = "Catálogo de habilidades técnicas y blandas")
public class HabilidadController {

    private final HabilidadService habilidadService;

    @Operation(summary = "Listar habilidades")
    @GetMapping
    public ResponseEntity<List<HabilidadResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(habilidadService.obtenerTodas());
    }

    @Operation(summary = "Obtener por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<HabilidadResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(habilidadService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar habilidades por coincidencia de nombre (autocompletado)")
    @GetMapping("/buscar")
    public ResponseEntity<List<HabilidadResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(habilidadService.buscarPorNombre(nombre));
    }

    @Operation(summary = "Crear (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PostMapping
    public ResponseEntity<HabilidadResponseDTO> crear(@Valid @RequestBody HabilidadRequestDTO request) {
        return new ResponseEntity<>(habilidadService.crear(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<HabilidadResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody HabilidadRequestDTO request) {
        return ResponseEntity.ok(habilidadService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        habilidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
