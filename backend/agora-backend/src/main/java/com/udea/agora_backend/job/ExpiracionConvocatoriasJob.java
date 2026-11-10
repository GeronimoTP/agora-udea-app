package com.udea.agora_backend.job;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.service.BuscadorEstados;
import com.udea.agora_backend.service.ExpiracionPostulacionesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tareas programadas del flujo de convocatorias. Las expresiones cron son configurables:
 * <ul>
 *   <li>{@code app.scheduler.ofertas-vencidas.cron} (por defecto, cada hora).</li>
 *   <li>{@code app.scheduler.convocatorias-expiradas.cron} (por defecto, 1:00 AM todos los días).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpiracionConvocatoriasJob {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final BuscadorEstados buscadorEstados;
    private final ExpiracionPostulacionesService expiracionPostulaciones;

    /** Expira las ofertas cuyo plazo de respuesta venció y libera el espacio para otro aspirante. */
    @Scheduled(cron = "${app.scheduler.ofertas-vencidas.cron:0 0 * * * *}")
    public void procesarOfertasVencidas() {
        int expiradas = expiracionPostulaciones.expirarOfertasVencidas();
        if (expiradas > 0) {
            log.info("{} ofertas expiradas por vencimiento del plazo de respuesta", expiradas);
        }
    }

    /**
     * Cierra las convocatorias cuya fecha de cierre pasó y expira todo lo que quedó sin resolver
     * (postulaciones en Pendiente o Pre-aprobada). El administrador también puede lanzarlo a mano.
     */
    @Scheduled(cron = "${app.scheduler.convocatorias-expiradas.cron:0 0 1 * * *}")
    public void procesarConvocatoriasExpiradas() {
        log.info("=== INICIANDO PROCESAMIENTO DE CONVOCATORIAS EXPIRADAS ===");
        procesarOfertasVencidas();

        List<Convocatoria> expiradas = convocatoriaRepository.findConvocatoriasExpiradas();
        Estado cerrada = buscadorEstados.deConvocatoria(EstadosConvocatoria.CERRADA);
        for (Convocatoria convocatoria : expiradas) {
            log.info("Cerrando convocatoria expirada: {} (fecha_cierre: {})",
                    convocatoria.getTitulo(), convocatoria.getFechaCierre());
            convocatoria.setEstado(cerrada);
            convocatoriaRepository.save(convocatoria);
            expiracionPostulaciones.expirarSinResolver(convocatoria.getId());
        }

        // Convocatorias que ya estaban cerradas pero conservan postulaciones sin resolver
        expiracionPostulaciones.expirarSinResolverEnConvocatoriasCerradas();

        log.info("=== PROCESAMIENTO COMPLETADO: {} convocatorias cerradas ===", expiradas.size());
    }
}
