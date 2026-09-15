package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ReconocimientoRequestDTO;
import com.udea.agora_backend.dto.response.ReconocimientoResponseDTO;
import com.udea.agora_backend.service.ReconocimientoService;
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
@RequestMapping("/api/reconocimientos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Reconocimientos", description = "Endpoints para el registro y trazabilidad de premios, galardones y logros de semilleros")
public class ReconocimientoController {

    private final ReconocimientoService reconocimientoService;

    @Operation(summary = "Obtener todos los reconocimientos")
    @ApiResponse(responseCode = "200", description = "Lista de reconocimientos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<ReconocimientoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(reconocimientoService.obtenerTodos());
    }

    @Operation(summary = "Obtener un reconocimiento por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconocimiento encontrado"),
            @ApiResponse(responseCode = "404", description = "Reconocimiento no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReconocimientoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(reconocimientoService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener reconocimientos otorgados a un semillero específico")
    @ApiResponse(responseCode = "200", description = "Lista de reconocimientos del semillero obtenida exitosamente")
    @GetMapping("/semillero/{idSemillero}")
    public ResponseEntity<List<ReconocimientoResponseDTO>> obtenerPorSemillero(@PathVariable Integer idSemillero) {
        return ResponseEntity.ok(reconocimientoService.obtenerPorSemillero(idSemillero));
    }

    @Operation(summary = "Registrar un nuevo reconocimiento para un semillero")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reconocimiento registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Semillero, tipo de reconocimiento o entidad otorgante no encontrada")
    })
    @PostMapping
    public ResponseEntity<ReconocimientoResponseDTO> crear(@Valid @RequestBody ReconocimientoRequestDTO request) {
        ReconocimientoResponseDTO creado = reconocimientoService.crear(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar información de un reconocimiento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconocimiento actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Reconocimiento, semillero o entidad no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReconocimientoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ReconocimientoRequestDTO request) {
        return ResponseEntity.ok(reconocimientoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un reconocimiento por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reconocimiento eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reconocimiento no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        reconocimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

