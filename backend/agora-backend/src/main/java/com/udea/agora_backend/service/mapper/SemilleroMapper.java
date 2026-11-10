package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.EstudianteSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Facultad;
import com.udea.agora_backend.model.ProfesorSemillero;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.ConteoPorSemillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository.LineaDeSemillero;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte las entidades del semillero y sus asociaciones en DTOs de respuesta. Separa la
 * forma en que la API presenta los datos de las reglas de negocio de los servicios.
 */
@Component
@RequiredArgsConstructor
public class SemilleroMapper {

    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ConvocatoriaRepository convocatoriaRepository;

    /** Semillero con sus líneas y los totales que muestran las tarjetas y el detalle. */
    public SemilleroResponseDTO aResponse(Semillero semillero) {
        return aResponses(List.of(semillero)).getFirst();
    }

    /**
     * Convierte varios semilleros con un número fijo de consultas (líneas, proyectos,
     * publicaciones, integrantes y convocatorias), sin importar cuántos sean. Antes se hacían
     * cinco consultas por semillero: con 65 semilleros, más de 300.
     */
    public List<SemilleroResponseDTO> aResponses(List<Semillero> semilleros) {
        if (semilleros.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = semilleros.stream().map(Semillero::getId).toList();
        Map<Integer, List<String>> lineas = semilleroLineaRepository.findLineasDeSemilleros(ids).stream()
                .collect(Collectors.groupingBy(LineaDeSemillero::getIdSemillero,
                        Collectors.mapping(LineaDeSemillero::getNombreLinea, Collectors.toList())));
        Map<Integer, Long> proyectos = porSemillero(proyectoRepository.contarPorSemillero(ids));
        Map<Integer, Long> publicaciones = porSemillero(publicacionRepository.contarPorSemillero(ids));
        Map<Integer, Long> integrantes =
                porSemillero(estudianteSemilleroRepository.contarIntegrantesActivosPorSemillero(ids));
        LocalDate hoy = LocalDate.now();
        Set<Integer> conConvocatoriaAbierta = convocatoriaRepository.findBySemilleroIdIn(ids).stream()
                .filter(c -> c.estaVigente(hoy) && c.tieneCuposDisponibles())
                .map(c -> c.getSemillero().getId())
                .collect(Collectors.toSet());

        return semilleros.stream()
                .map(semillero -> {
                    Integer id = semillero.getId();
                    return aResponse(semillero,
                            lineas.getOrDefault(id, List.of()),
                            proyectos.getOrDefault(id, 0L).intValue(),
                            publicaciones.getOrDefault(id, 0L).intValue(),
                            integrantes.getOrDefault(id, 0L).intValue(),
                            conConvocatoriaAbierta.contains(id));
                })
                .toList();
    }

    private static Map<Integer, Long> porSemillero(List<ConteoPorSemillero> conteos) {
        return conteos.stream()
                .collect(Collectors.toMap(ConteoPorSemillero::getIdSemillero, ConteoPorSemillero::getTotal));
    }

    private SemilleroResponseDTO aResponse(Semillero semillero, List<String> lineasInvestigacion,
                                           int totalProyectos, int totalPublicaciones,
                                           int totalIntegrantes, boolean convocatoriaAbierta) {
        Optional<Usuario> usuarioLider = Optional.ofNullable(semillero.getEstudianteLider())
                .map(Estudiante::getUsuario);
        Optional<ProgramaAcademico> programa = Optional.ofNullable(semillero.getPrograma());

        return SemilleroResponseDTO.builder()
                .id(semillero.getId())
                .codigoIdentificador(semillero.getCodigoIdentificador())
                .nombre(semillero.getNombre())
                .descripcion(semillero.getDescripcion())
                .idEstudianteLider(semillero.getEstudianteLider() != null ? semillero.getEstudianteLider().getId() : null)
                .idUsuarioLider(usuarioLider.map(Usuario::getId).orElse(null))
                .nombreEstudianteLider(usuarioLider.map(Usuario::getNombreCompleto).orElse(null))
                .idPrograma(programa.map(ProgramaAcademico::getId).orElse(null))
                .nombrePrograma(programa.map(ProgramaAcademico::getNombre).orElse(null))
                .estadoActual(semillero.getEstado() != null ? semillero.getEstado().getNombre() : null)
                .lineasInvestigacion(lineasInvestigacion)
                .nombreFacultad(programa.map(ProgramaAcademico::getFacultad).map(Facultad::getNombre).orElse(null))
                .totalProyectos(totalProyectos)
                .totalPublicaciones(totalPublicaciones)
                .totalIntegrantes(totalIntegrantes)
                .convocatoriaAbierta(convocatoriaAbierta)
                .build();
    }

    public EstudianteSemilleroResponseDTO aVinculacion(EstudianteSemillero vinculacion) {
        Estudiante estudiante = vinculacion.getEstudiante();
        return EstudianteSemilleroResponseDTO.builder()
                .id(vinculacion.getId())
                .idSemillero(vinculacion.getSemillero().getId())
                .nombreSemillero(vinculacion.getSemillero().getNombre())
                .idEstudiante(estudiante.getId())
                .idUsuario(estudiante.getUsuario().getId())
                .nombreEstudiante(estudiante.getUsuario().getNombreCompleto())
                .email(estudiante.getUsuario().getEmail())
                .nombrePrograma(estudiante.getPrograma() != null ? estudiante.getPrograma().getNombre() : null)
                .semestre(estudiante.getSemestre())
                .fechaIngreso(vinculacion.getFechaIngreso())
                .fechaSalida(vinculacion.getFechaSalida())
                .estadoVinculacion(vinculacion.getEstado() != null ? vinculacion.getEstado().getNombre() : null)
                .build();
    }

    public ProfesorSemilleroResponseDTO aTutor(ProfesorSemillero tutoria) {
        Usuario usuario = tutoria.getProfesor().getUsuario();
        return ProfesorSemilleroResponseDTO.builder()
                .id(tutoria.getId())
                .nombreSemillero(tutoria.getSemillero().getNombre())
                .idProfesor(tutoria.getProfesor().getId())
                .idUsuario(usuario.getId())
                .nombreProfesor(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .fechaAsignacion(tutoria.getFechaAsignacion())
                .build();
    }

    public SemilleroLineaInvestigacionResponseDTO aLinea(SemilleroLineaInvestigacion asociacion) {
        return SemilleroLineaInvestigacionResponseDTO.builder()
                .id(asociacion.getId())
                .nombreSemillero(asociacion.getSemillero().getNombre())
                .idLineaInvestigacion(asociacion.getLineaInvestigacion().getId())
                .nombreLineaInvestigacion(asociacion.getLineaInvestigacion().getNombre())
                .build();
    }
}
