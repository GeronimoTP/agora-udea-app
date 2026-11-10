package com.udea.agora_backend.service.analitica;

import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.dto.response.analitica.ConversionProyectoPublicacionDTO;
import com.udea.agora_backend.dto.response.analitica.DemandaConvocatoriaDTO;
import com.udea.agora_backend.dto.response.analitica.DistribucionImpactoReconocimientosDTO;
import com.udea.agora_backend.dto.response.analitica.HabilidadFrecuenciaDTO;
import com.udea.agora_backend.dto.response.analitica.InterdisciplinariedadDTO;
import com.udea.agora_backend.dto.response.analitica.ParticipacionEstudiantilDTO;
import com.udea.agora_backend.dto.response.analitica.TasaDemandaSemilleroDTO;
import com.udea.agora_backend.dto.response.analitica.TasaRetencionOfertasDTO;
import com.udea.agora_backend.dto.response.analitica.TiempoPromedioRespuestaDTO;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PublicacionUsuario;
import com.udea.agora_backend.model.ReconocimientoSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.util.Redondeo;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fórmulas de los indicadores del Servicio 3. Son funciones puras sobre {@link DatosAnalitica}:
 * no consultan la base de datos ni saben si los datos son de toda la universidad o de un
 * semillero, así que el mismo cálculo sirve para ambas vistas.
 */
@Component
public class CalculadoraIndicadores {

    private static final String SIN_ENTIDAD = "Sin Entidad";
    private static final String SIN_TIPO = "Sin Tipo";
    private static final String ROL_AUTOR_ESTUDIANTE = "estudiante";
    private static final List<String> ESTADOS_EN_PROCESO =
            List.of(EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA);
    // Orden del embudo de postulaciones, del ingreso al desenlace
    private static final List<String> ORDEN_ESTADOS_POSTULACION = List.of(
            EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA, EstadosPostulacion.ACEPTADA,
            EstadosPostulacion.RECHAZADA, EstadosPostulacion.RECHAZADA_POR_ESTUDIANTE,
            EstadosPostulacion.CANCELADA_EN_CASCADA, EstadosPostulacion.EXPIRADA);

    /** Postulaciones que siguen en revisión del líder o con una oferta sin responder. */
    public long postulacionesActivas(DatosAnalitica datos) {
        return datos.postulaciones().stream().filter(this::estaEnProceso).count();
    }

    /** Cuántas postulaciones hay en cada estado, en el orden del flujo. */
    public Map<String, Long> postulacionesPorEstado(DatosAnalitica datos) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        ORDEN_ESTADOS_POSTULACION.forEach(estado -> conteo.put(estado, 0L));
        for (Postulacion postulacion : datos.postulaciones()) {
            if (postulacion.getEstado() != null) {
                conteo.merge(postulacion.getEstado().getNombre(), 1L, Long::sum);
            }
        }
        return conteo;
    }

    /** 1. Tasa de demanda por semillero: postulaciones recibidas / cupos ofertados. */
    public List<TasaDemandaSemilleroDTO> demandaPorSemillero(List<Semillero> semilleros, DatosAnalitica datos) {
        Map<Integer, Integer> cuposPorSemillero = datos.convocatorias().stream()
                .collect(Collectors.groupingBy(c -> c.getSemillero().getId(),
                        Collectors.summingInt(Convocatoria::getCuposTotales)));
        Map<Integer, Long> postulacionesPorSemillero = datos.postulaciones().stream()
                .collect(Collectors.groupingBy(p -> p.getConvocatoria().getSemillero().getId(),
                        Collectors.counting()));

        return semilleros.stream()
                .map(semillero -> {
                    int cupos = cuposPorSemillero.getOrDefault(semillero.getId(), 0);
                    long recibidas = postulacionesPorSemillero.getOrDefault(semillero.getId(), 0L);
                    return TasaDemandaSemilleroDTO.builder()
                            .idSemillero(semillero.getId())
                            .nombreSemillero(semillero.getNombre())
                            .cuposOfertados(cupos)
                            .postulacionesRecibidas(recibidas)
                            .tasaDemanda(Redondeo.dosDecimales(razon(recibidas, cupos)))
                            .build();
                })
                .toList();
    }

    /** Demanda de cada convocatoria, de la más reciente a la más antigua. */
    public List<DemandaConvocatoriaDTO> demandaPorConvocatoria(DatosAnalitica datos) {
        Map<Integer, Long> postulacionesPorConvocatoria = datos.postulaciones().stream()
                .collect(Collectors.groupingBy(p -> p.getConvocatoria().getId(), Collectors.counting()));

        return datos.convocatorias().stream()
                .sorted(Comparator.comparing(Convocatoria::getFechaInicio).reversed())
                .map(convocatoria -> {
                    long recibidas = postulacionesPorConvocatoria.getOrDefault(convocatoria.getId(), 0L);
                    return DemandaConvocatoriaDTO.builder()
                            .idConvocatoria(convocatoria.getId())
                            .titulo(convocatoria.getTitulo())
                            .estado(convocatoria.getEstado() != null ? convocatoria.getEstado().getNombre() : null)
                            .cuposTotales(convocatoria.getCuposTotales())
                            .cuposDisponibles(convocatoria.getCuposDisponibles())
                            .postulacionesRecibidas(recibidas)
                            .tasaDemanda(Redondeo.dosDecimales(razon(recibidas, convocatoria.getCuposTotales())))
                            .build();
                })
                .toList();
    }

    /** Postulaciones recibidas por cada cupo ofertado, sumando todas las convocatorias. */
    public double tasaDemandaGlobal(DatosAnalitica datos) {
        int cupos = datos.convocatorias().stream().mapToInt(Convocatoria::getCuposTotales).sum();
        return Redondeo.dosDecimales(razon(datos.postulaciones().size(), cupos));
    }

    /**
     * 2. Tiempo promedio de respuesta del estudiante: desde la oferta del líder
     * (fechaPreAprobacion) hasta que la acepta o la declina. No incluye rechazos del líder,
     * cancelaciones en cascada ni expiraciones, porque en esos casos el estudiante no decidió.
     */
    public TiempoPromedioRespuestaDTO tiempoPromedioRespuesta(DatosAnalitica datos) {
        List<Postulacion> decididas = datos.postulaciones().stream()
                .filter(p -> p.getFechaPreAprobacion() != null && p.getFechaDecisionEstudiante() != null)
                .filter(p -> p.tieneEstado(EstadosPostulacion.ACEPTADA)
                        || p.tieneEstado(EstadosPostulacion.RECHAZADA_POR_ESTUDIANTE))
                .toList();

        double promedioHoras = decididas.stream()
                .mapToLong(p -> Math.max(0, Duration.between(p.getFechaPreAprobacion(),
                        p.getFechaDecisionEstudiante()).toHours()))
                .average()
                .orElse(0.0);

        return TiempoPromedioRespuestaDTO.builder()
                .totalPostulacionesEvaluadas(decididas.size())
                .promedioHoras(Redondeo.dosDecimales(promedioHoras))
                .promedioDias(Redondeo.dosDecimales(promedioHoras / 24.0))
                .build();
    }

    /**
     * 3. Retención vs. rechazo de ofertas. Solo cuenta ofertas reales (postulaciones que el
     * líder pre-aprobó): aceptadas; rechazadas (declinadas por el estudiante o canceladas en
     * cascada después de recibir la oferta) y expiradas con oferta emitida.
     */
    public TasaRetencionOfertasDTO tasaRetencion(DatosAnalitica datos) {
        List<Postulacion> postulaciones = datos.postulaciones();
        long aceptadas = postulaciones.stream()
                .filter(p -> p.tieneEstado(EstadosPostulacion.ACEPTADA))
                .count();
        long rechazadas = postulaciones.stream()
                .filter(p -> p.tieneEstado(EstadosPostulacion.RECHAZADA_POR_ESTUDIANTE)
                        || (p.tieneEstado(EstadosPostulacion.CANCELADA_EN_CASCADA) && recibioOferta(p)))
                .count();
        long expiradas = postulaciones.stream()
                .filter(p -> p.tieneEstado(EstadosPostulacion.EXPIRADA) && recibioOferta(p))
                .count();
        long total = aceptadas + rechazadas + expiradas;

        return TasaRetencionOfertasDTO.builder()
                .ofertasAceptadas(aceptadas)
                .ofertasRechazadasPorEstudiante(rechazadas)
                .ofertasExpiradas(expiradas)
                .totalOfertasProcesadas(total)
                .porcentajeRetencion(Redondeo.dosDecimales(porcentaje(aceptadas, total)))
                .porcentajeRechazoYExpiracion(Redondeo.dosDecimales(porcentaje(rechazadas + expiradas, total)))
                .build();
    }

    /** 4. Índice de conversión proyecto-publicación. */
    public ConversionProyectoPublicacionDTO conversionProyectoPublicacion(DatosAnalitica datos) {
        long proyectos = datos.proyectos().size();
        long publicaciones = datos.publicaciones().size();
        return ConversionProyectoPublicacionDTO.builder()
                .totalProyectos(proyectos)
                .totalPublicaciones(publicaciones)
                .promedioPublicacionesPorProyecto(Redondeo.dosDecimales(razon(publicaciones, proyectos)))
                .build();
    }

    /** 5. Participación estudiantil en autorías de publicaciones. */
    public ParticipacionEstudiantilDTO participacionEstudiantil(DatosAnalitica datos) {
        long total = datos.autorias().size();
        long deEstudiantes = datos.autorias().stream()
                .filter(autoria -> esAutoriaDeEstudiante(autoria, datos))
                .count();
        return ParticipacionEstudiantilDTO.builder()
                .totalAutoresRegistrados(total)
                .totalAutoresEstudiantes(deEstudiantes)
                .porcentajeParticipacionEstudiantil(Redondeo.dosDecimales(porcentaje(deEstudiantes, total)))
                .build();
    }

    /** 6. Distribución de reconocimientos por entidad otorgante y por tipo. */
    public DistribucionImpactoReconocimientosDTO distribucionReconocimientos(DatosAnalitica datos) {
        List<ReconocimientoSemillero> reconocimientos = datos.reconocimientos();
        return DistribucionImpactoReconocimientosDTO.builder()
                .totalReconocimientos(reconocimientos.size())
                .distribucionPorEntidad(contarPor(reconocimientos, r -> r.getEntidadOtorgante() != null
                        ? r.getEntidadOtorgante().getNombre() : SIN_ENTIDAD))
                .distribucionPorTipo(contarPor(reconocimientos, r -> r.getTipoReconocimiento() != null
                        ? r.getTipoReconocimiento().getNombre() : SIN_TIPO))
                .build();
    }

    /** 7. Habilidades más frecuentes en postulaciones aceptadas. */
    public List<HabilidadFrecuenciaDTO> habilidadesEmergentes(DatosAnalitica datos) {
        Map<String, Long> conteo = datos.habilidadesPostuladas().stream()
                .filter(ph -> ph.getPostulacion() != null
                        && ph.getPostulacion().tieneEstado(EstadosPostulacion.ACEPTADA))
                .collect(Collectors.groupingBy(ph -> ph.getHabilidad().getNombre(), Collectors.counting()));

        return conteo.entrySet().stream()
                .map(entrada -> HabilidadFrecuenciaDTO.builder()
                        .habilidad(entrada.getKey())
                        .frecuencia(entrada.getValue())
                        .build())
                .sorted(Comparator.comparingLong(HabilidadFrecuenciaDTO::getFrecuencia).reversed())
                .toList();
    }

    /** 8. Interdisciplinariedad: vinculaciones de estudiantes de otro programa que el del semillero. */
    public InterdisciplinariedadDTO interdisciplinariedad(DatosAnalitica datos) {
        List<EstudianteSemillero> vinculaciones = datos.vinculaciones();
        long interdisciplinarios = vinculaciones.stream().filter(this::esInterdisciplinaria).count();
        return InterdisciplinariedadDTO.builder()
                .totalEstudiantesVinculados(vinculaciones.size())
                .estudiantesInterdisciplinarios(interdisciplinarios)
                .porcentajeInterdisciplinariedad(
                        Redondeo.dosDecimales(porcentaje(interdisciplinarios, vinculaciones.size())))
                .build();
    }

    private boolean estaEnProceso(Postulacion postulacion) {
        return ESTADOS_EN_PROCESO.stream().anyMatch(postulacion::tieneEstado);
    }

    private static boolean recibioOferta(Postulacion postulacion) {
        return postulacion.getFechaPreAprobacion() != null;
    }

    private static boolean esAutoriaDeEstudiante(PublicacionUsuario autoria, DatosAnalitica datos) {
        return datos.idsUsuariosEstudiantes().contains(autoria.getUsuario().getId())
                || (autoria.getRolAutor() != null
                && autoria.getRolAutor().getNombre().toLowerCase().contains(ROL_AUTOR_ESTUDIANTE));
    }

    private boolean esInterdisciplinaria(EstudianteSemillero vinculacion) {
        if (vinculacion.getEstudiante() == null || vinculacion.getEstudiante().getPrograma() == null
                || vinculacion.getSemillero() == null || vinculacion.getSemillero().getPrograma() == null) {
            return false;
        }
        return !vinculacion.getEstudiante().getPrograma().getId()
                .equals(vinculacion.getSemillero().getPrograma().getId());
    }

    private static <T> Map<String, Long> contarPor(List<T> elementos, Function<T, String> clasificador) {
        return elementos.stream().collect(Collectors.groupingBy(clasificador, Collectors.counting()));
    }

    private static double razon(long numerador, long denominador) {
        return denominador > 0 ? (double) numerador / denominador : 0.0;
    }

    private static double porcentaje(long parte, long total) {
        return razon(parte, total) * 100.0;
    }
}
