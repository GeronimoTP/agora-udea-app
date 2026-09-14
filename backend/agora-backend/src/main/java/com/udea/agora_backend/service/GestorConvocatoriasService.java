package com.udea.agora_backend.service;

import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * SERVICIO CRÍTICO: Gestor de Convocatorias
 * 
 * Implementa la lógica compleja de gestión de convocatorias:
 * 1. Postulación Concurrente: Permite múltiples postulaciones activas por estudiante
 * 2. Cascada de Rechazos: Rechaza automáticamente otras postulaciones al aceptar una
 * 3. Gestión Dinámica de Cupos: Decrementa cupos solo con confirmación
 * 4. Cierre Automático: Marca como cerrada cuando cupos llegan a 0
 * 5. Expiración de Ofertas: Procesa convocatorias expiradas (via @Scheduled)
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GestorConvocatoriasService {

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EstadoRepository estadoRepository;

    /**
     * LÓGICA CRÍTICA 1 + 2: Aceptar postulación con cascada de rechazos
     */
    public void aceptarPostulacion(Integer idPostulacion) {
        log.info("=== INICIANDO ACEPTACIÓN DE POSTULACIÓN: {} ===", idPostulacion);

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));

        if (!postulacion.getEstado().getNombre().equals("Pendiente")) {
            throw new ErrorLogicaNegocioException(
                    "Solo se pueden aceptar postulaciones en estado 'Pendiente'. Estado actual: " + 
                    postulacion.getEstado().getNombre()
            );
        }

        Convocatoria convocatoria = postulacion.getConvocatoria();

        if (convocatoria.getCuposDisponibles() <= 0) {
            throw new ErrorLogicaNegocioException(
                    "No hay cupos disponibles en la convocatoria: " + convocatoria.getTitulo()
            );
        }

        // Cambiar estado a Aceptada
        Estado estadoAceptada = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Aceptada"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Aceptada"));
        
        postulacion.setEstado(estadoAceptada);
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());

        // Decrementar cupos
        convocatoria.setCuposDisponibles(convocatoria.getCuposDisponibles() - 1);
        log.info("Cupos decrementados de {} a {}", 
                convocatoria.getCuposDisponibles() + 1, 
                convocatoria.getCuposDisponibles());

        // Cierre automático si cupos = 0
        if (convocatoria.getCuposDisponibles() == 0) {
            Estado estadoCerrada = estadoRepository.findAll().stream()
                    .filter(e -> e.getNombre().equalsIgnoreCase("Cerrada"))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Cerrada"));
            convocatoria.setEstado(estadoCerrada);
            log.info("Convocatoria marcada como CERRADA (sin cupos)");
        }

        postulacionRepository.save(postulacion);
        convocatoriaRepository.save(convocatoria);

        // CASCADA DE RECHAZOS
        ejecutarCascadaRechazo(postulacion.getEstudiante().getId(), idPostulacion);

        log.info("=== ACEPTACIÓN COMPLETADA ===");
    }

    /**
     * LÓGICA DE CASCADA: Rechaza automáticamente todas las postulaciones activas
     */
    private void ejecutarCascadaRechazo(Integer idEstudiante, Integer idPostulacionAceptada) {
        log.info("Iniciando cascada de rechazos para estudiante {}", idEstudiante);

        Estado estadoRechazada = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Rechazada"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Rechazada"));

        List<Postulacion> postulacionesActivas = postulacionRepository
                .findPostulacionesActivasByEstudiante(idEstudiante, "Pendiente", "Activa");

        int contadoRechazadas = 0;
        for (Postulacion p : postulacionesActivas) {
            if (!p.getId().equals(idPostulacionAceptada)) {
                log.info("Rechazando postulación {} para convocatoria '{}'", 
                        p.getId(), p.getConvocatoria().getTitulo());
                
                p.setEstado(estadoRechazada);
                p.setFechaDecisionEstudiante(ZonedDateTime.now());
                postulacionRepository.save(p);
                contadoRechazadas++;
            }
        }

        log.info("Cascada completada: {} postulaciones rechazadas", contadoRechazadas);
    }

    /**
     * Rechazar una postulación específica
     */
    public void rechazarPostulacion(Integer idPostulacion) {
        log.info("Rechazando postulación: {}", idPostulacion);

        Postulacion postulacion = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));

        if (!postulacion.getEstado().getNombre().equals("Pendiente")) {
            throw new ErrorLogicaNegocioException(
                    "Solo se pueden rechazar postulaciones en estado 'Pendiente'. Estado actual: " + 
                    postulacion.getEstado().getNombre()
            );
        }

        Estado estadoRechazada = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Rechazada"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Rechazada"));

        postulacion.setEstado(estadoRechazada);
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());
        postulacionRepository.save(postulacion);

        log.info("Postulación {} rechazada exitosamente", idPostulacion);
    }

    /**
     * LÓGICA 5: Procesa convocatorias expiradas
     */
    public void procesarConvocatoriasExpiradas() {
        log.info("=== INICIANDO PROCESAMIENTO DE CONVOCATORIAS EXPIRADAS ===");

        LocalDate hoy = LocalDate.now();

        List<Convocatoria> convocatoriasExpiradas = convocatoriaRepository
                .findConvocatoriasExpiradas();

        Estado estadoCerrada = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Cerrada"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Cerrada"));

        Estado estadoExpirada = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Expirada"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Expirada"));

        int convocatoriasActualizadas = 0;
        for (Convocatoria conv : convocatoriasExpiradas) {
            if (!conv.getEstado().getNombre().equals("Cerrada")) {
                log.info("Cerrando convocatoria expirada: {} (fecha_cierre: {})", 
                        conv.getTitulo(), conv.getFechaCierre());
                
                conv.setEstado(estadoCerrada);
                convocatoriaRepository.save(conv);
                convocatoriasActualizadas++;

                // Procesar postulaciones pendientes
                List<Postulacion> postulacionesPendientes = 
                        postulacionRepository.findPostulacionesByConvocatoriaAndEstado(
                                conv.getId(), 
                                "Pendiente"
                        );

                for (Postulacion post : postulacionesPendientes) {
                    if (post.getFechaDecisionEstudiante() == null) {
                        log.info("Marcando postulación {} como expirada", post.getId());
                        post.setEstado(estadoExpirada);
                        post.setFechaDecisionEstudiante(ZonedDateTime.now());
                        postulacionRepository.save(post);
                    }
                }
            }
        }

        log.info("=== PROCESAMIENTO COMPLETADO: {} convocatorias cerradas ===", convocatoriasActualizadas);
    }

    /**
     * Obtiene el estado de cupos de una convocatoria
     */
    public ConvocatoriaEstadoCuposDTO obtenerEstadoCupos(Integer idConvocatoria) {
        Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));

        int cuposUtilizados = convocatoria.getCuposTotales() - convocatoria.getCuposDisponibles();

        return ConvocatoriaEstadoCuposDTO.builder()
                .idConvocatoria(idConvocatoria)
                .nombreConvocatoria(convocatoria.getTitulo())
                .cuposOriginales(convocatoria.getCuposTotales())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .cuposUtilizados(cuposUtilizados)
                .porcentajeOcupacion((cuposUtilizados * 100.0) / convocatoria.getCuposTotales())
                .build();
    }

    /**
     * DTO para respuesta de estado de cupos
     */
    @lombok.Data
    @lombok.Builder
    public static class ConvocatoriaEstadoCuposDTO {
        private Integer idConvocatoria;
        private String nombreConvocatoria;
        private Integer cuposOriginales;
        private Integer cuposDisponibles;
        private Integer cuposUtilizados;
        private Double porcentajeOcupacion;
    }
}
