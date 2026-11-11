package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.PostulacionHabilidadResponseDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.dto.response.RequisitoEvaluadoDTO;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PostulacionHabilidad;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.RequisitoConvocatoria;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.PostulacionRequisitoRepository.RequisitoDeclarado;
import com.udea.agora_backend.repository.PostulacionRequisitoRepository;
import com.udea.agora_backend.repository.RequisitoConvocatoriaRepository;
import com.udea.agora_backend.service.PlazoOfertasService;
import com.udea.agora_backend.service.RequisitosConvocatoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Convierte postulaciones y sus habilidades en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class PostulacionMapper {

    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final PlazoOfertasService plazoOfertasService;
    private final RequisitoConvocatoriaRepository requisitoRepository;
    private final PostulacionRequisitoRepository postulacionRequisitoRepository;

    /** Incluye las habilidades declaradas y la fecha límite para responder una oferta. */
    public PostulacionResponseDTO aResponse(Postulacion postulacion) {
        return aResponses(List.of(postulacion)).getFirst();
    }

    /**
     * Convierte varias postulaciones con tres consultas en total: las habilidades declaradas,
     * los requisitos de sus convocatorias y los requisitos de texto que declararon cumplir.
     */
    public List<PostulacionResponseDTO> aResponses(List<Postulacion> postulaciones) {
        if (postulaciones.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = postulaciones.stream().map(Postulacion::getId).toList();
        Map<Integer, List<PostulacionHabilidad>> habilidades = postulacionHabilidadRepository
                .findByPostulacionIdIn(ids).stream()
                .collect(Collectors.groupingBy(ph -> ph.getPostulacion().getId()));
        Map<Integer, List<RequisitoConvocatoria>> requisitos = requisitoRepository
                .findByConvocatoriaIdIn(postulaciones.stream().map(p -> p.getConvocatoria().getId()).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(r -> r.getConvocatoria().getId()));
        Map<Integer, Set<Integer>> declarados = postulacionRequisitoRepository.findDeclarados(ids).stream()
                .collect(Collectors.groupingBy(RequisitoDeclarado::getIdPostulacion,
                        Collectors.mapping(RequisitoDeclarado::getIdRequisito, Collectors.toSet())));
        return postulaciones.stream()
                .map(p -> {
                    List<PostulacionHabilidad> declaradas = habilidades.getOrDefault(p.getId(), List.of());
                    List<RequisitoEvaluadoDTO> evaluados = RequisitosConvocatoriaService.evaluar(
                            requisitos.getOrDefault(p.getConvocatoria().getId(), List.of()),
                            declaradas.stream().map(ph -> ph.getHabilidad().getId()).collect(Collectors.toSet()),
                            declarados.getOrDefault(p.getId(), Set.of()));
                    return aResponse(p, declaradas.stream().map(ph -> ph.getHabilidad().getNombre()).toList(), evaluados);
                })
                .toList();
    }

    private PostulacionResponseDTO aResponse(Postulacion postulacion, List<String> habilidades,
                                             List<RequisitoEvaluadoDTO> requisitos) {
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
                .requisitos(requisitos)
                .cumpleObligatorios(RequisitosConvocatoriaService.cumpleObligatorios(requisitos))
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
