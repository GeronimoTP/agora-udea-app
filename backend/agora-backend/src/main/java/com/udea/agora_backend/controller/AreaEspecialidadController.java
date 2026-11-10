package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.AreaEspecialidadRequestDTO;
import com.udea.agora_backend.dto.response.AreaEspecialidadResponseDTO;
import com.udea.agora_backend.service.AreaEspecialidadService;
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
@RequestMapping("/api/areas-especialidad")
@RequiredArgsConstructor
@Tag(name = "Áreas de Especialidad", description = "Catálogo de áreas de especialidad docente")
public class AreaEspecialidadController {

    private final AreaEspecialidadService areaService;

    @Operation(summary = "Listar áreas de especialidad")
    @GetMapping
    public ResponseEntity<List<AreaEspecialidadResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(areaService.obtenerTodas());
    }

    @Operation(summary = "Obtener por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AreaEspecialidadResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar áreas por coincidencia de nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<AreaEspecialidadResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(areaService.buscarPorNombre(nombre));
    }

    @Operation(summary = "Crear (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PostMapping
    public ResponseEntity<AreaEspecialidadResponseDTO> crear(@Valid @RequestBody AreaEspecialidadRequestDTO request) {
        return new ResponseEntity<>(areaService.crear(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AreaEspecialidadResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody AreaEspecialidadRequestDTO request) {
        return ResponseEntity.ok(areaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        areaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
