package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.response.SemilleroRecomendadoDTO;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO;
import com.udea.agora_backend.service.MotorRecomendacionService;
import com.udea.agora_backend.service.SemillerosSimilaresService;
import com.udea.agora_backend.service.autorizacion.UsuarioAutenticadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
@RequiredArgsConstructor
@Tag(name = "Motor de Recomendación", description = "Servicio 1: Algoritmo de afinidad inteligente y matchmaking de semilleros (0% - 100%)")
public class RecomendacionController {

    private final MotorRecomendacionService motorRecomendacionService;
    private final SemillerosSimilaresService semillerosSimilaresService;
    private final UsuarioAutenticadoService usuarioAutenticado;

    @Operation(
            summary = "Calcular ranking de afinidad de semilleros para un estudiante",
            description = "Calcula la afinidad inteligente evaluando 4 factores: Coincidencia de habilidades (40%), Afinidad áreas tutor (25%), Disponibilidad de cupos (20%) y Proximidad temática (15%)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de afinidad calculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @GetMapping("/mias")
    public ResponseEntity<List<SemilleroRecomendadoDTO>> calcularAfinidad(Authentication authentication) {
        return ResponseEntity.ok(motorRecomendacionService.calcularAfinidad(
                usuarioAutenticado.idEstudianteActual(authentication.getName())));
    }

    @Operation(
            summary = "Semilleros similares a los tuyos (árbol KD)",
            description = "Busca los semilleros activos más cercanos, en un árbol KD indexado por áreas de "
                    + "especialidad, al centroide de los semilleros en los que el estudiante está (peso 1.0) "
                    + "o estuvo (peso 0.5). Sin historial usa las líneas de su perfil. Excluye los semilleros "
                    + "en los que ya está, estuvo o lidera."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Semilleros similares calculados"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @GetMapping("/mias/similares")
    public ResponseEntity<SemillerosSimilaresResponseDTO> semillerosSimilares(
            @RequestParam(defaultValue = "5") int limite,
            Authentication authentication) {
        return ResponseEntity.ok(semillerosSimilaresService.buscar(
                usuarioAutenticado.idEstudianteActual(authentication.getName()), limite));
    }
}
