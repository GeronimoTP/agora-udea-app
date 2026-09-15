package com.udea.agora_backend.controller;

import com.udea.agora_backend.service.AnaliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analitica")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Analítica y KPIs", description = "Servicio 3: Tableros de datos, métricas institucionales y KPIs de rendimiento para la administración")
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @Operation(summary = "Obtener resumen consolidado del Dashboard con todos los KPIs institucionales")
    @ApiResponse(responseCode = "200", description = "Resumen del dashboard obtenido exitosamente")
    @GetMapping("/dashboard")
    public ResponseEntity<AnaliticaService.DashboardResumenDTO> obtenerDashboard() {
        return ResponseEntity.ok(analiticaService.obtenerResumenDashboard());
    }

    @Operation(summary = "KPI: Tasa de demanda por semillero (Presión sobre cupos: Postulaciones recibidas / Cupos ofertados)")
    @ApiResponse(responseCode = "200", description = "Tasas de demanda calculadas exitosamente")
    @GetMapping("/tasa-demanda")
    public ResponseEntity<List<AnaliticaService.TasaDemandaSemilleroDTO>> calcularTasaDemanda() {
        return ResponseEntity.ok(analiticaService.calcularTasaDemandaPorSemillero());
    }

    @Operation(summary = "KPI: Tiempo promedio de respuesta del estudiante ante ofertas pre-aprobadas")
    @ApiResponse(responseCode = "200", description = "Tiempo promedio de respuesta calculado exitosamente")
    @GetMapping("/tiempo-respuesta")
    public ResponseEntity<AnaliticaService.TiempoPromedioRespuestaDTO> calcularTiempoRespuesta() {
        return ResponseEntity.ok(analiticaService.calcularTiempoPromedioRespuesta());
    }

    @Operation(summary = "KPI: Tasa de retención vs rechazo de ofertas de admisión")
    @ApiResponse(responseCode = "200", description = "Tasa de retención calculada exitosamente")
    @GetMapping("/tasa-retencion-rechazo")
    public ResponseEntity<AnaliticaService.TasaRetencionOfertasDTO> calcularTasaRetencion() {
        return ResponseEntity.ok(analiticaService.calcularTasaRetencionRechazo());
    }

    @Operation(summary = "KPI: Índice de conversión Proyecto-Publicación (Promedio de entregables por proyecto)")
    @ApiResponse(responseCode = "200", description = "Índice de conversión calculado exitosamente")
    @GetMapping("/conversion-proyecto-publicacion")
    public ResponseEntity<AnaliticaService.ConversionProyectoPublicacionDTO> calcularConversionProyectos() {
        return ResponseEntity.ok(analiticaService.calcularConversionProyectoPublicacion());
    }

    @Operation(summary = "KPI: Índice de participación estudiantil en autorías de publicaciones")
    @ApiResponse(responseCode = "200", description = "Índice de participación calculado exitosamente")
    @GetMapping("/participacion-estudiantil")
    public ResponseEntity<AnaliticaService.ParticipacionEstudiantilDTO> calcularParticipacionEstudiantil() {
        return ResponseEntity.ok(analiticaService.calcularParticipacionEstudiantil());
    }

    @Operation(summary = "KPI: Distribución del impacto y prestigio de reconocimientos (Por entidad otorgante y tipo)")
    @ApiResponse(responseCode = "200", description = "Distribución de reconocimientos calculada exitosamente")
    @GetMapping("/distribucion-reconocimientos")
    public ResponseEntity<AnaliticaService.DistribucionImpactoReconocimientosDTO> calcularDistribucionReconocimientos() {
        return ResponseEntity.ok(analiticaService.calcularDistribucionImpactoReconocimientos());
    }

    @Operation(summary = "KPI: Mapa de habilidades emergentes (Skill Trend de postulaciones aceptadas)")
    @ApiResponse(responseCode = "200", description = "Mapa de habilidades calculado exitosamente")
    @GetMapping("/mapa-habilidades")
    public ResponseEntity<List<AnaliticaService.HabilidadFrecuenciaDTO>> calcularMapaHabilidades() {
        return ResponseEntity.ok(analiticaService.calcularMapaHabilidadesEmergentes());
    }

    @Operation(summary = "KPI: Índice de interdisciplinariedad (% estudiantes vinculados fuera del programa del semillero)")
    @ApiResponse(responseCode = "200", description = "Índice de interdisciplinariedad calculado exitosamente")
    @GetMapping("/interdisciplinariedad")
    public ResponseEntity<AnaliticaService.InterdisciplinariedadDTO> calcularInterdisciplinariedad() {
        return ResponseEntity.ok(analiticaService.calcularIndiceInterdisciplinariedad());
    }
}

