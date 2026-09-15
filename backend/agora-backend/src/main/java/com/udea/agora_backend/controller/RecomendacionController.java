package com.udea.agora_backend.controller;

import com.udea.agora_backend.service.MotorRecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Motor de Recomendación", description = "Servicio 1: Algoritmo de afinidad inteligente y matchmaking de semilleros (0% - 100%)")
public class RecomendacionController {

    private final MotorRecomendacionService motorRecomendacionService;

    @Operation(
            summary = "Calcular ranking de afinidad de semilleros para un estudiante",
            description = "Calcula la afinidad inteligente evaluando 4 factores: Coincidencia de habilidades (40%), Afinidad áreas tutor (25%), Disponibilidad de cupos (20%) y Proximidad temática (15%)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de afinidad calculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<MotorRecomendacionService.SemilleroRecomendadoDTO>> calcularAfinidad(
            @PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(motorRecomendacionService.calcularAfinidad(idEstudiante));
    }
}

