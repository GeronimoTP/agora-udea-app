package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.LineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.response.LineaInvestigacionResponseDTO;
import com.udea.agora_backend.service.LineaInvestigacionService;
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
@RequestMapping("/api/lineas-investigacion")
@RequiredArgsConstructor
@Tag(name = "Líneas de Investigación", description = "Catálogo de líneas de investigación institucionales")
public class LineaInvestigacionController {

    private final LineaInvestigacionService lineaService;

    @Operation(summary = "Listar líneas de investigación")
    @GetMapping
    public ResponseEntity<List<LineaInvestigacionResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(lineaService.obtenerTodas());
    }

    @Operation(summary = "Obtener por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LineaInvestigacionResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(lineaService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar líneas por coincidencia de nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<LineaInvestigacionResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(lineaService.buscarPorNombre(nombre));
    }

    @Operation(summary = "Crear (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PostMapping
    public ResponseEntity<LineaInvestigacionResponseDTO> crear(@Valid @RequestBody LineaInvestigacionRequestDTO request) {
        return new ResponseEntity<>(lineaService.crear(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<LineaInvestigacionResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody LineaInvestigacionRequestDTO request) {
        return ResponseEntity.ok(lineaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar (solo administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        lineaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
