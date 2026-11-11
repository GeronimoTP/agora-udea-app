package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.RequisitoRequestDTO;
import com.udea.agora_backend.dto.response.RequisitoEvaluadoDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PostulacionRequisito;
import com.udea.agora_backend.model.RequisitoConvocatoria;
import com.udea.agora_backend.repository.HabilidadRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.PostulacionRequisitoRepository;
import com.udea.agora_backend.repository.RequisitoConvocatoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Requisitos de las convocatorias (decisión de la reunión con el profesor).
 *
 * <p>El líder define, al abrir la convocatoria, requisitos obligatorios u opcionales: habilidades
 * del catálogo o textos libres. Incumplir uno obligatorio NO impide postularse: el líder ve qué
 * falta y la priorización ubica al aspirante después de los que cumplen todos.
 * <ul>
 *   <li>Requisito de habilidad: se cumple si la habilidad está entre las declaradas en la postulación.</li>
 *   <li>Requisito de texto: se cumple si el estudiante declara cumplirlo al postularse.</li>
 * </ul>
 * Los requisitos no cambian después de la primera postulación, para que todos los aspirantes
 * se evalúen con las mismas reglas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequisitosConvocatoriaService {

    private final RequisitoConvocatoriaRepository requisitoRepository;
    private final PostulacionRequisitoRepository postulacionRequisitoRepository;
    private final PostulacionRepository postulacionRepository;
    private final HabilidadRepository habilidadRepository;

    /** Define los requisitos de una convocatoria nueva. */
    public void definir(Convocatoria convocatoria, List<RequisitoRequestDTO> requisitos) {
        if (requisitos == null) {
            return;
        }
        validar(requisitos);
        requisitos.forEach(r -> requisitoRepository.save(nuevo(convocatoria, r)));
    }

    /**
     * Reemplaza los requisitos al editar. Si la convocatoria ya recibió postulaciones solo se
     * acepta la misma lista (el formulario de edición siempre la envía).
     */
    public void reemplazar(Convocatoria convocatoria, List<RequisitoRequestDTO> requisitos) {
        if (requisitos == null) {
            return;
        }
        validar(requisitos);
        List<RequisitoConvocatoria> actuales = requisitoRepository.findByConvocatoriaId(convocatoria.getId());
        if (claves(actuales).equals(clavesSolicitadas(requisitos))) {
            return;
        }
        if (!postulacionRepository.findByConvocatoriaId(convocatoria.getId()).isEmpty()) {
            throw new ErrorLogicaNegocioException(
                    "La convocatoria ya recibió postulaciones: sus requisitos no se pueden cambiar.");
        }
        requisitoRepository.deleteAll(actuales);
        requisitoRepository.flush();
        requisitos.forEach(r -> requisitoRepository.save(nuevo(convocatoria, r)));
    }

    /** Al eliminar una convocatoria (sin postulaciones) se borran sus requisitos. */
    public void eliminarDe(Integer idConvocatoria) {
        requisitoRepository.deleteAll(requisitoRepository.findByConvocatoriaId(idConvocatoria));
    }

    /**
     * Guarda los requisitos de texto que el estudiante declara cumplir, reemplazando los
     * anteriores. Solo se aceptan requisitos de texto de la convocatoria de la postulación.
     */
    public void registrarDeclaraciones(Postulacion postulacion, List<Integer> idRequisitosCumplidos) {
        Set<Integer> declarados = idRequisitosCumplidos == null ? Set.of()
                : idRequisitosCumplidos.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        List<RequisitoConvocatoria> deTexto = requisitoRepository
                .findByConvocatoriaId(postulacion.getConvocatoria().getId()).stream()
                .filter(r -> !r.esDeHabilidad())
                .toList();
        Set<Integer> validos = deTexto.stream().map(RequisitoConvocatoria::getId).collect(Collectors.toSet());
        if (!validos.containsAll(declarados)) {
            throw new ErrorLogicaNegocioException("Uno de los requisitos declarados no pertenece a la convocatoria.");
        }
        postulacionRequisitoRepository.deleteAll(postulacionRequisitoRepository.findByPostulacionId(postulacion.getId()));
        postulacionRequisitoRepository.flush();
        deTexto.stream()
                .filter(r -> declarados.contains(r.getId()))
                .forEach(r -> postulacionRequisitoRepository.save(PostulacionRequisito.builder()
                        .postulacion(postulacion)
                        .requisito(r)
                        .build()));
    }

    /** Al retirar una postulación se borran sus declaraciones. */
    public void eliminarDeclaraciones(Integer idPostulacion) {
        postulacionRequisitoRepository.deleteAll(postulacionRequisitoRepository.findByPostulacionId(idPostulacion));
    }

    /**
     * Evalúa cada requisito para una postulación. Es pura: recibe lo que ya se cargó en lote.
     *
     * @param habilidadesDeclaradas IDs de las habilidades declaradas en la postulación
     * @param requisitosDeclarados  IDs de los requisitos de texto que el estudiante declaró cumplir
     */
    public static List<RequisitoEvaluadoDTO> evaluar(List<RequisitoConvocatoria> requisitos,
                                                     Collection<Integer> habilidadesDeclaradas,
                                                     Collection<Integer> requisitosDeclarados) {
        return requisitos.stream()
                .map(r -> RequisitoEvaluadoDTO.builder()
                        .idRequisito(r.getId())
                        .texto(r.texto())
                        .deHabilidad(r.esDeHabilidad())
                        .obligatorio(r.isObligatorio())
                        .cumple(r.esDeHabilidad()
                                ? habilidadesDeclaradas.contains(r.getHabilidad().getId())
                                : requisitosDeclarados.contains(r.getId()))
                        .build())
                .toList();
    }

    public static boolean cumpleObligatorios(List<RequisitoEvaluadoDTO> evaluados) {
        return evaluados.stream().allMatch(r -> !r.isObligatorio() || r.isCumple());
    }

    // ==========================================
    // VALIDACIÓN
    // ==========================================

    private void validar(List<RequisitoRequestDTO> requisitos) {
        Set<Integer> habilidades = new HashSet<>();
        Set<String> textos = new HashSet<>();
        for (RequisitoRequestDTO r : requisitos) {
            boolean conHabilidad = r.getIdHabilidad() != null;
            boolean conTexto = r.getDescripcion() != null && !r.getDescripcion().isBlank();
            if (conHabilidad == conTexto) {
                throw new ErrorLogicaNegocioException(
                        "Cada requisito debe ser una habilidad del catálogo o un texto, no ambos ni ninguno.");
            }
            boolean repetido = conHabilidad
                    ? !habilidades.add(r.getIdHabilidad())
                    : !textos.add(r.getDescripcion().trim().toLowerCase());
            if (repetido) {
                throw new ErrorLogicaNegocioException("Hay un requisito repetido.");
            }
        }
    }

    private RequisitoConvocatoria nuevo(Convocatoria convocatoria, RequisitoRequestDTO r) {
        Habilidad habilidad = r.getIdHabilidad() == null ? null
                : habilidadRepository.findById(r.getIdHabilidad())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", r.getIdHabilidad()));
        return RequisitoConvocatoria.builder()
                .convocatoria(convocatoria)
                .habilidad(habilidad)
                .descripcion(habilidad == null ? r.getDescripcion().trim() : null)
                .obligatorio(Boolean.TRUE.equals(r.getObligatorio()))
                .build();
    }

    // Identidad de un requisito para comparar la lista actual con la enviada
    private record Clave(Integer idHabilidad, String descripcion, boolean obligatorio) {
    }

    private static Set<Clave> claves(List<RequisitoConvocatoria> requisitos) {
        return requisitos.stream()
                .map(r -> new Clave(r.esDeHabilidad() ? r.getHabilidad().getId() : null,
                        r.esDeHabilidad() ? null : r.getDescripcion(), r.isObligatorio()))
                .collect(Collectors.toSet());
    }

    private static Set<Clave> clavesSolicitadas(List<RequisitoRequestDTO> requisitos) {
        return requisitos.stream()
                .map(r -> new Clave(r.getIdHabilidad(),
                        r.getIdHabilidad() == null ? r.getDescripcion().trim() : null,
                        Boolean.TRUE.equals(r.getObligatorio())))
                .collect(Collectors.toSet());
    }
}
