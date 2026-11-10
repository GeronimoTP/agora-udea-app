package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.PostulacionHabilidadResponseDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PostulacionHabilidad;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.service.PlazoOfertasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Convierte postulaciones y sus habilidades en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class PostulacionMapper {

    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final PlazoOfertasService plazoOfertasService;

    /** Incluye las habilidades declaradas y la fecha límite para responder una oferta. */
    public PostulacionResponseDTO aResponse(Postulacion postulacion) {
        List<String> habilidades = postulacionHabilidadRepository.findByPostulacionId(postulacion.getId()).stream()
                .map(ph -> ph.getHabilidad().getNombre())
                .toList();
        Optional<Estudiante> estudiante = Optional.ofNullable(postulacion.getEstudiante());
        Optional<Convocatoria> convocatoria = Optional.ofNullable(postulacion.getConvocatoria());

        return PostulacionResponseDTO.builder()
                .id(postulacion.getId())
                .idConvocatoria(convocatoria.map(Convocatoria::getId).orElse(null))
                .idEstudiante(estudiante.map(Estudiante::getId).orElse(null))
                .nombreEstudiante(estudiante.map(Estudiante::getUsuario).map(Usuario::getNombreCompleto).orElse(null))
                .programaEstudiante(estudiante.map(Estudiante::getPrograma).map(ProgramaAcademico::getNombre).orElse(null))
                .tituloConvocatoria(convocatoria.map(Convocatoria::getTitulo).orElse(null))
                .nombreSemillero(convocatoria.map(Convocatoria::getSemillero).map(Semillero::getNombre).orElse(null))
                .respuestaMotivacion(postulacion.getRespuestaMotivacion())
                .disponibilidadHorasSemana(postulacion.getDisponibilidadHorasSemana())
                .experienciaPrevia(postulacion.getExperienciaPrevia())
                .fechaPostulacion(postulacion.getFechaPostulacion())
                .fechaPreAprobacion(postulacion.getFechaPreAprobacion())
                .fechaLimiteRespuesta(plazoOfertasService.calcularFechaLimite(postulacion))
                .fechaDecisionEstudiante(postulacion.getFechaDecisionEstudiante())
                .estadoPostulacion(Optional.ofNullable(postulacion.getEstado()).map(Estado::getNombre).orElse(null))
                .habilidades(habilidades)
                .build();
    }

    public PostulacionHabilidadResponseDTO aHabilidad(PostulacionHabilidad asociacion) {
        return PostulacionHabilidadResponseDTO.builder()
                .id(asociacion.getId())
                .idPostulacion(asociacion.getPostulacion().getId())
                .nombreHabilidad(asociacion.getHabilidad().getNombre())
                .build();
    }
}
