package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.response.analitica.AnaliticaSemilleroDTO;
import com.udea.agora_backend.dto.response.analitica.ConversionProyectoPublicacionDTO;
import com.udea.agora_backend.dto.response.analitica.DashboardResumenDTO;
import com.udea.agora_backend.dto.response.analitica.DistribucionImpactoReconocimientosDTO;
import com.udea.agora_backend.dto.response.analitica.HabilidadFrecuenciaDTO;
import com.udea.agora_backend.dto.response.analitica.InterdisciplinariedadDTO;
import com.udea.agora_backend.dto.response.analitica.ParticipacionEstudiantilDTO;
import com.udea.agora_backend.dto.response.analitica.ReferenciaInstitucionalDTO;
import com.udea.agora_backend.dto.response.analitica.TasaDemandaSemilleroDTO;
import com.udea.agora_backend.dto.response.analitica.TasaRetencionOfertasDTO;
import com.udea.agora_backend.dto.response.analitica.TiempoPromedioRespuestaDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.PublicacionUsuarioRepository;
import com.udea.agora_backend.repository.ReconocimientoSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.analitica.CalculadoraIndicadores;
import com.udea.agora_backend.service.analitica.DatosAnalitica;
import com.udea.agora_backend.util.Redondeo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * SERVICIO 3: analítica e indicadores de impacto.
 *
 * <p>Carga los datos de cada alcance y delega las fórmulas en {@link CalculadoraIndicadores}:
 * <ul>
 *   <li>Institucional (administrador): todos los semilleros.</li>
 *   <li>Por semillero (sus líderes y tutores): solo los datos de ese semillero, más la
 *       referencia institucional para comparar.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnaliticaService {

    private final CalculadoraIndicadores calculadora;
    private final SemilleroRepository semilleroRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionRepository postulacionRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final PublicacionUsuarioRepository publicacionUsuarioRepository;
    private final ReconocimientoSemilleroRepository reconocimientoRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final EstudianteRepository estudianteRepository;

    // ==========================================
    // VISTA INSTITUCIONAL (administrador)
    // ==========================================

    public DashboardResumenDTO obtenerResumenDashboard() {
        DatosAnalitica datos = datosInstitucionales();
        return DashboardResumenDTO.builder()
                .postulacionesActivas(calculadora.postulacionesActivas(datos))
                .tasasDemanda(calculadora.demandaPorSemillero(semillerosAprobados(), datos))
                .tiempoRespuesta(calculadora.tiempoPromedioRespuesta(datos))
                .tasaRetencion(calculadora.tasaRetencion(datos))
                .conversionProyectos(calculadora.conversionProyectoPublicacion(datos))
                .participacionEstudiantil(calculadora.participacionEstudiantil(datos))
                .impactoReconocimientos(calculadora.distribucionReconocimientos(datos))
                .habilidadesEmergentes(calculadora.habilidadesEmergentes(datos))
                .interdisciplinariedad(calculadora.interdisciplinariedad(datos))
                .build();
    }

    public List<TasaDemandaSemilleroDTO> calcularTasaDemandaPorSemillero() {
        return calculadora.demandaPorSemillero(semillerosAprobados(), datosInstitucionales());
    }

    public TiempoPromedioRespuestaDTO calcularTiempoPromedioRespuesta() {
        return calculadora.tiempoPromedioRespuesta(datosInstitucionales());
    }

    public TasaRetencionOfertasDTO calcularTasaRetencionRechazo() {
        return calculadora.tasaRetencion(datosInstitucionales());
    }

    public ConversionProyectoPublicacionDTO calcularConversionProyectoPublicacion() {
        return calculadora.conversionProyectoPublicacion(datosInstitucionales());
    }

    public ParticipacionEstudiantilDTO calcularParticipacionEstudiantil() {
        return calculadora.participacionEstudiantil(datosInstitucionales());
    }

    public DistribucionImpactoReconocimientosDTO calcularDistribucionImpactoReconocimientos() {
        return calculadora.distribucionReconocimientos(datosInstitucionales());
    }

    public List<HabilidadFrecuenciaDTO> calcularMapaHabilidadesEmergentes() {
        return calculadora.habilidadesEmergentes(datosInstitucionales());
    }

    public InterdisciplinariedadDTO calcularIndiceInterdisciplinariedad() {
        return calculadora.interdisciplinariedad(datosInstitucionales());
    }

    // ==========================================
    // VISTA DE UN SEMILLERO (líderes y tutores)
    // ==========================================

    public AnaliticaSemilleroDTO obtenerAnaliticaSemillero(Integer idSemillero) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        DatosAnalitica datos = datosDeSemillero(idSemillero);
        int integrantesActivos = (int) datos.vinculaciones().stream().filter(EstudianteSemillero::estaActiva).count();

        return AnaliticaSemilleroDTO.builder()
                .idSemillero(semillero.getId())
                .nombreSemillero(semillero.getNombre())
                .integrantesActivos(integrantesActivos)
                .integrantesRetirados(datos.vinculaciones().size() - integrantesActivos)
                .postulacionesActivas(calculadora.postulacionesActivas(datos))
                .totalPostulaciones((long) datos.postulaciones().size())
                .postulacionesPorEstado(calculadora.postulacionesPorEstado(datos))
                .tasaDemanda(calculadora.tasaDemandaGlobal(datos))
                .demandaPorConvocatoria(calculadora.demandaPorConvocatoria(datos))
                .tiempoRespuesta(calculadora.tiempoPromedioRespuesta(datos))
                .tasaRetencion(calculadora.tasaRetencion(datos))
                .conversionProyectos(calculadora.conversionProyectoPublicacion(datos))
                .participacionEstudiantil(calculadora.participacionEstudiantil(datos))
                .impactoReconocimientos(calculadora.distribucionReconocimientos(datos))
                .habilidadesEmergentes(calculadora.habilidadesEmergentes(datos))
                .interdisciplinariedad(calculadora.interdisciplinariedad(datos))
                .referenciaInstitucional(referenciaInstitucional())
                .build();
    }

    /**
     * Valores de toda la universidad para comparar. La tasa de demanda es el promedio de las
     * tasas por semillero, igual que en la vista del administrador.
     */
    private ReferenciaInstitucionalDTO referenciaInstitucional() {
        DatosAnalitica datos = datosInstitucionales();
        double demandaPromedio = calculadora.demandaPorSemillero(semillerosAprobados(), datos).stream()
                .mapToDouble(TasaDemandaSemilleroDTO::getTasaDemanda)
                .average()
                .orElse(0.0);
        return ReferenciaInstitucionalDTO.builder()
                .tasaDemandaPromedio(Redondeo.dosDecimales(demandaPromedio))
                .porcentajeRetencion(calculadora.tasaRetencion(datos).getPorcentajeRetencion())
                .promedioHorasRespuesta(calculadora.tiempoPromedioRespuesta(datos).getPromedioHoras())
                .promedioPublicacionesPorProyecto(
                        calculadora.conversionProyectoPublicacion(datos).getPromedioPublicacionesPorProyecto())
                .porcentajeParticipacionEstudiantil(
                        calculadora.participacionEstudiantil(datos).getPorcentajeParticipacionEstudiantil())
                .porcentajeInterdisciplinariedad(
                        calculadora.interdisciplinariedad(datos).getPorcentajeInterdisciplinariedad())
                .build();
    }

    // ==========================================
    // CARGA DE DATOS POR ALCANCE
    // ==========================================

    // Los semilleros pendientes de aprobación o rechazados no operan: no entran en la demanda
    private List<Semillero> semillerosAprobados() {
        return semilleroRepository.findAll().stream().filter(Semillero::estaAprobado).toList();
    }

    private DatosAnalitica datosInstitucionales() {
        return new DatosAnalitica(
                convocatoriaRepository.findAll(),
                postulacionRepository.findAll(),
                postulacionHabilidadRepository.findAll(),
                proyectoRepository.findAll(),
                publicacionRepository.findAll(),
                publicacionUsuarioRepository.findAll(),
                reconocimientoRepository.findAll(),
                estudianteSemilleroRepository.findAll(),
                new HashSet<>(estudianteRepository.findIdsUsuario()));
    }

    private DatosAnalitica datosDeSemillero(Integer idSemillero) {
        return new DatosAnalitica(
                convocatoriaRepository.findBySemilleroId(idSemillero),
                postulacionRepository.findByConvocatoriaSemilleroId(idSemillero),
                postulacionHabilidadRepository.findBySemilleroId(idSemillero),
                proyectoRepository.findBySemilleroId(idSemillero),
                publicacionRepository.findByProyectoSemilleroId(idSemillero),
                publicacionUsuarioRepository.findByPublicacionProyectoSemilleroId(idSemillero),
                reconocimientoRepository.findBySemilleroId(idSemillero),
                estudianteSemilleroRepository.findBySemilleroId(idSemillero),
                new HashSet<>(estudianteRepository.findIdsUsuario()));
    }
}
