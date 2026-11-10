package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.EstudianteSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.ProfesorSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        Integer id = semillero.getId();
        Optional<Usuario> usuarioLider = Optional.ofNullable(semillero.getEstudianteLider())
                .map(Estudiante::getUsuario);
        List<String> lineasInvestigacion = semilleroLineaRepository.findBySemilleroId(id).stream()
                .map(sli -> sli.getLineaInvestigacion().getNombre())
                .toList();
        LocalDate hoy = LocalDate.now();

        return SemilleroResponseDTO.builder()
                .id(id)
                .codigoIdentificador(semillero.getCodigoIdentificador())
                .nombre(semillero.getNombre())
                .descripcion(semillero.getDescripcion())
                .idEstudianteLider(semillero.getEstudianteLider() != null ? semillero.getEstudianteLider().getId() : null)
                .idUsuarioLider(usuarioLider.map(Usuario::getId).orElse(null))
                .nombreEstudianteLider(usuarioLider.map(Usuario::getNombreCompleto).orElse(null))
                .idPrograma(semillero.getPrograma() != null ? semillero.getPrograma().getId() : null)
                .nombrePrograma(semillero.getPrograma() != null ? semillero.getPrograma().getNombre() : null)
                .estadoActual(semillero.getEstado() != null ? semillero.getEstado().getNombre() : null)
                .lineasInvestigacion(lineasInvestigacion)
                .nombreFacultad(semillero.getPrograma() != null && semillero.getPrograma().getFacultad() != null
                        ? semillero.getPrograma().getFacultad().getNombre() : null)
                .totalProyectos(proyectoRepository.findBySemilleroId(id).size())
                .totalPublicaciones(publicacionRepository.findByProyectoSemilleroId(id).size())
                .totalIntegrantes(estudianteSemilleroRepository.findBySemilleroIdAndFechaSalidaIsNull(id).size())
                .convocatoriaAbierta(convocatoriaRepository.findBySemilleroId(id).stream()
                        .anyMatch(c -> c.estaVigente(hoy) && c.tieneCuposDisponibles()))
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
