package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.SemilleroRequestDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProgramaAcademicoRepository;
import com.udea.agora_backend.repository.EstadoRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de semilleros de investigación.
 * Maneja operaciones CRUD y búsquedas relacionadas con semilleros.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SemilleroService {

    private final SemilleroRepository semilleroRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final EstadoRepository estadoRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;

    /**
     * Obtiene todos los semilleros
     */
    public List<SemilleroResponseDTO> obtenerTodos() {
        return semilleroRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un semillero por su ID
     */
    public SemilleroResponseDTO obtenerPorId(Integer id) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));
        return mapeoAResponseDTO(semillero);
    }

    /**
     * Obtiene un semillero por código identificador
     */
    public SemilleroResponseDTO obtenerPorCodigo(String codigo) {
        Semillero semillero = semilleroRepository.findByCodigoIdentificador(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", "codigo", codigo));
        return mapeoAResponseDTO(semillero);
    }

    /**
     * Obtiene semilleros por programa
     */
    public List<SemilleroResponseDTO> obtenerPorPrograma(Integer idPrograma) {
        return semilleroRepository.findByProgramaId(idPrograma)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene semilleros por estado
     */
    public List<SemilleroResponseDTO> obtenerPorEstado(Integer idEstado) {
        return semilleroRepository.findByEstadoId(idEstado)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo semillero
     */
    public SemilleroResponseDTO crear(SemilleroRequestDTO request) {
        // Verificar código duplicado
        if (semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener estudiante líder
        Estudiante estudianteLider = estudianteRepository.findById(request.getIdEstudianteLider())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante Líder", request.getIdEstudianteLider()));

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));

        // Obtener estado inicial (Activo)
        Estado estado = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Activo"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Activo"));

        // Crear semillero
        Semillero semillero = Semillero.builder()
                .codigoIdentificador(request.getCodigoIdentificador())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .estudianteLider(estudianteLider)
                .programa(programa)
                .estado(estado)
                .createdAt(ZonedDateTime.now())
                .build();

        Semillero semilleroGuardado = semilleroRepository.save(semillero);
        return mapeoAResponseDTO(semilleroGuardado);
    }

    /**
     * Actualiza un semillero existente
     */
    public SemilleroResponseDTO actualizar(Integer id, SemilleroRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));

        // Verificar código duplicado si cambió
        if (!semillero.getCodigoIdentificador().equals(request.getCodigoIdentificador()) &&
                semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener programa si cambió
        if (!semillero.getPrograma().getId().equals(request.getIdPrograma())) {
            ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));
            semillero.setPrograma(programa);
        }

        // Estado no se actualiza desde RequestDTO (se gestiona externamente)

        // Actualizar campos
        semillero.setCodigoIdentificador(request.getCodigoIdentificador());
        semillero.setNombre(request.getNombre());
        semillero.setDescripcion(request.getDescripcion());

        Semillero semilleroActualizado = semilleroRepository.save(semillero);
        return mapeoAResponseDTO(semilleroActualizado);
    }

    /**
     * Elimina un semillero por su ID
     */
    public void eliminar(Integer id) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));
        semilleroRepository.delete(semillero);
    }

    /**
     * Mapea entidad Semillero a ResponseDTO
     */
    private SemilleroResponseDTO mapeoAResponseDTO(Semillero semillero) {
        // Obtener líneas de investigación
        List<String> lineasInvestigacion = semilleroLineaRepository.findBySemilleroId(semillero.getId())
                .stream()
                .map(sli -> sli.getLineaInvestigacion().getNombre())
                .collect(Collectors.toList());

        return SemilleroResponseDTO.builder()
                .id(semillero.getId())
                .codigoIdentificador(semillero.getCodigoIdentificador())
                .nombre(semillero.getNombre())
                .descripcion(semillero.getDescripcion())
                .nombreEstudianteLider(semillero.getEstudianteLider().getUsuario().getNombreCompleto())
                .nombrePrograma(semillero.getPrograma().getNombre())
                .estadoActual(semillero.getEstado().getNombre())
                .lineasInvestigacion(lineasInvestigacion)
                .build();
    }
}
