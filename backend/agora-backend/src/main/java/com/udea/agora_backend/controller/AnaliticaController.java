package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.response.analitica.AnaliticaSemilleroDTO;
import com.udea.agora_backend.dto.response.analitica.ConversionProyectoPublicacionDTO;
import com.udea.agora_backend.dto.response.analitica.DashboardResumenDTO;
import com.udea.agora_backend.dto.response.analitica.DistribucionImpactoReconocimientosDTO;
import com.udea.agora_backend.dto.response.analitica.HabilidadFrecuenciaDTO;
import com.udea.agora_backend.dto.response.analitica.InterdisciplinariedadDTO;
import com.udea.agora_backend.dto.response.analitica.ParticipacionEstudiantilDTO;
import com.udea.agora_backend.dto.response.analitica.TasaDemandaSemilleroDTO;
import com.udea.agora_backend.dto.response.analitica.TasaRetencionOfertasDTO;
import com.udea.agora_backend.dto.response.analitica.TiempoPromedioRespuestaDTO;
import com.udea.agora_backend.service.AnaliticaService;
import com.udea.agora_backend.service.autorizacion.AutorizacionSemilleroService;
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
@RequestMapping("/api/analitica")
@RequiredArgsConstructor
@Tag(name = "Analítica y KPIs", description = "Servicio 3: indicadores institucionales (administrador) y de cada semillero (sus líderes y tutores)")
public class AnaliticaController {

    private final AnaliticaService analiticaService;
    private final AutorizacionSemilleroService autorizacionSemillero;

    @Operation(summary = "Obtener resumen consolidado del Dashboard con todos los KPIs institucionales")
    @ApiResponse(responseCode = "200", description = "Resumen del dashboard obtenido exitosamente")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResumenDTO> obtenerDashboard() {
        return ResponseEntity.ok(analiticaService.obtenerResumenDashboard());
    }

    @Operation(summary = "KPI: Tasa de demanda por semillero (Presión sobre cupos: Postulaciones recibidas / Cupos ofertados)")
    @ApiResponse(responseCode = "200", description = "Tasas de demanda calculadas exitosamente")
    @GetMapping("/tasa-demanda")
    public ResponseEntity<List<TasaDemandaSemilleroDTO>> calcularTasaDemanda() {
        return ResponseEntity.ok(analiticaService.calcularTasaDemandaPorSemillero());
    }

    @Operation(summary = "KPI: Tiempo promedio de respuesta del estudiante ante ofertas pre-aprobadas")
    @ApiResponse(responseCode = "200", description = "Tiempo promedio de respuesta calculado exitosamente")
    @GetMapping("/tiempo-respuesta")
    public ResponseEntity<TiempoPromedioRespuestaDTO> calcularTiempoRespuesta() {
        return ResponseEntity.ok(analiticaService.calcularTiempoPromedioRespuesta());
    }

    @Operation(summary = "KPI: Tasa de retención vs rechazo de ofertas de admisión")
    @ApiResponse(responseCode = "200", description = "Tasa de retención calculada exitosamente")
    @GetMapping("/tasa-retencion-rechazo")
    public ResponseEntity<TasaRetencionOfertasDTO> calcularTasaRetencion() {
        return ResponseEntity.ok(analiticaService.calcularTasaRetencionRechazo());
    }

    @Operation(summary = "KPI: Índice de conversión Proyecto-Publicación (Promedio de entregables por proyecto)")
    @ApiResponse(responseCode = "200", description = "Índice de conversión calculado exitosamente")
    @GetMapping("/conversion-proyecto-publicacion")
    public ResponseEntity<ConversionProyectoPublicacionDTO> calcularConversionProyectos() {
        return ResponseEntity.ok(analiticaService.calcularConversionProyectoPublicacion());
    }

    @Operation(summary = "KPI: Índice de participación estudiantil en autorías de publicaciones")
    @ApiResponse(responseCode = "200", description = "Índice de participación calculado exitosamente")
    @GetMapping("/participacion-estudiantil")
    public ResponseEntity<ParticipacionEstudiantilDTO> calcularParticipacionEstudiantil() {
        return ResponseEntity.ok(analiticaService.calcularParticipacionEstudiantil());
    }

    @Operation(summary = "KPI: Distribución del impacto y prestigio de reconocimientos (Por entidad otorgante y tipo)")
    @ApiResponse(responseCode = "200", description = "Distribución de reconocimientos calculada exitosamente")
    @GetMapping("/distribucion-reconocimientos")
    public ResponseEntity<DistribucionImpactoReconocimientosDTO> calcularDistribucionReconocimientos() {
        return ResponseEntity.ok(analiticaService.calcularDistribucionImpactoReconocimientos());
    }

    @Operation(summary = "KPI: Mapa de habilidades emergentes (Skill Trend de postulaciones aceptadas)")
    @ApiResponse(responseCode = "200", description = "Mapa de habilidades calculado exitosamente")
    @GetMapping("/mapa-habilidades")
    public ResponseEntity<List<HabilidadFrecuenciaDTO>> calcularMapaHabilidades() {
        return ResponseEntity.ok(analiticaService.calcularMapaHabilidadesEmergentes());
    }

    @Operation(summary = "KPI: Índice de interdisciplinariedad (% estudiantes vinculados fuera del programa del semillero)")
    @ApiResponse(responseCode = "200", description = "Índice de interdisciplinariedad calculado exitosamente")
    @GetMapping("/interdisciplinariedad")
    public ResponseEntity<InterdisciplinariedadDTO> calcularInterdisciplinariedad() {
        return ResponseEntity.ok(analiticaService.calcularIndiceInterdisciplinariedad());
    }

    @Operation(summary = "Indicadores de un semillero",
            description = "Los mismos indicadores del tablero institucional, calculados solo con los datos del "
                    + "semillero, más su embudo de postulaciones, la demanda por convocatoria y la referencia "
                    + "institucional para comparar. Solo para sus líderes, tutores y el administrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
            @ApiResponse(responseCode = "403", description = "No es líder ni tutor del semillero"),
            @ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    @GetMapping("/semillero/{idSemillero}")
    public ResponseEntity<AnaliticaSemilleroDTO> obtenerAnaliticaSemillero(
            @PathVariable Integer idSemillero, Authentication authentication) {
        autorizacionSemillero.validarLiderOAdministrador(authentication.getName(), idSemillero);
        return ResponseEntity.ok(analiticaService.obtenerAnaliticaSemillero(idSemillero));
    }
}
