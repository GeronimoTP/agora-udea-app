package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import com.udea.agora_backend.dto.request.ConvocatoriaRequestDTO;
import com.udea.agora_backend.dto.response.ConvocatoriaEstadoCuposDTO;
import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.mapper.ConvocatoriaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de convocatorias.
 * Maneja operaciones CRUD y búsquedas relacionadas con convocatorias.
 * La lógica de cascada y cierre automático está en GestorConvocatoriasService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final BuscadorEstados buscadorEstados;
    private final SemilleroRepository semilleroRepository;
    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaMapper convocatoriaMapper;

    /**
     * Obtiene todas las convocatorias
     */
    public List<ConvocatoriaResponseDTO> obtenerTodas() {
        return convocatoriaRepository.findAll()
                .stream()
                .map(convocatoriaMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una convocatoria por su ID
     */
    public ConvocatoriaResponseDTO obtenerPorId(Integer id) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));
        return convocatoriaMapper.aResponse(convocatoria);
    }

    /**
     * Obtiene convocatorias por semillero
     */
    public List<ConvocatoriaResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return convocatoriaRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(convocatoriaMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene convocatorias activas con cupos disponibles
     */
    public List<ConvocatoriaResponseDTO> obtenerActivasConCupos() {
        Estado estadoAbierta = buscadorEstados.deConvocatoria(EstadosConvocatoria.ABIERTA);

        return convocatoriaRepository.findActivasConCupos(estadoAbierta.getId())
                .stream()
                .map(convocatoriaMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva convocatoria
     */
    public ConvocatoriaResponseDTO crear(ConvocatoriaRequestDTO request) {
        // Obtener semillero
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        if (!semillero.estaActivo()) {
            throw new ErrorLogicaNegocioException("Solo un semillero activo puede abrir convocatorias.");
        }
        if (request.getFechaInicio().isBefore(LocalDate.now())) {
            throw new ErrorLogicaNegocioException("La fecha de inicio no puede estar en el pasado.");
        }
        validarFechas(request.getFechaInicio(), request.getFechaCierre());

        Estado estado = buscadorEstados.deConvocatoria(EstadosConvocatoria.ABIERTA);

        // Crear convocatoria
        Convocatoria convocatoria = Convocatoria.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .fechaInicio(request.getFechaInicio())
                .fechaCierre(request.getFechaCierre())
                .cuposDisponibles(request.getCuposTotales())
                .cuposTotales(request.getCuposTotales())
                .semillero(semillero)
                .estado(estado)
                .createdAt(ZonedDateTime.now())
                .build();

        Convocatoria convocatoriaGuardada = convocatoriaRepository.save(convocatoria);
        return convocatoriaMapper.aResponse(convocatoriaGuardada);
    }

    /**
     * Actualiza una convocatoria existente
     */
    public ConvocatoriaResponseDTO actualizar(Integer id, ConvocatoriaRequestDTO request) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));

        if (convocatoria.getEstado().tieneNombre(EstadosConvocatoria.CANCELADA)) {
            throw new ErrorLogicaNegocioException("Una convocatoria cancelada no se puede editar.");
        }
        // Las postulaciones pertenecen al semillero original: una convocatoria no cambia de semillero
        if (!convocatoria.getSemillero().getId().equals(request.getIdSemillero())) {
            throw new ErrorLogicaNegocioException("Una convocatoria no puede cambiar de semillero.");
        }
        validarFechas(request.getFechaInicio(), request.getFechaCierre());

        // Los cupos ya ocupados (aceptaciones confirmadas) se conservan al cambiar el total
        int cuposOcupados = convocatoria.getCuposTotales() - convocatoria.getCuposDisponibles();
        if (request.getCuposTotales() < cuposOcupados) {
            throw new ErrorLogicaNegocioException(
                    "Los cupos totales no pueden ser menores que los " + cuposOcupados + " cupos ya asignados.");
        }

        convocatoria.setTitulo(request.getTitulo());
        convocatoria.setDescripcion(request.getDescripcion());
        convocatoria.setFechaInicio(request.getFechaInicio());
        convocatoria.setFechaCierre(request.getFechaCierre());
        convocatoria.setCuposTotales(request.getCuposTotales());
        convocatoria.setCuposDisponibles(request.getCuposTotales() - cuposOcupados);

        // Estado coherente con los cupos: sin cupos se cierra; si se liberan cupos en una
        // convocatoria cerrada solo por cupos y aún vigente, se reabre
        if (!convocatoria.tieneCuposDisponibles()) {
            convocatoria.setEstado(buscadorEstados.deConvocatoria(EstadosConvocatoria.CERRADA));
        } else if (convocatoria.getEstado().tieneNombre(EstadosConvocatoria.CERRADA)
                && !convocatoria.getFechaCierre().isBefore(LocalDate.now())) {
            convocatoria.setEstado(buscadorEstados.deConvocatoria(EstadosConvocatoria.ABIERTA));
        }

        Convocatoria convocatoriaActualizada = convocatoriaRepository.save(convocatoria);
        return convocatoriaMapper.aResponse(convocatoriaActualizada);
    }

    /**
     * Elimina una convocatoria por su ID
     */
    public void eliminar(Integer id) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));
        if (!postulacionRepository.findByConvocatoriaId(id).isEmpty()) {
            throw new ErrorLogicaNegocioException(
                    "No se puede eliminar una convocatoria con postulaciones; ciérrala en su lugar.");
        }
        convocatoriaRepository.delete(convocatoria);
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaCierre) {
        if (fechaCierre.isBefore(fechaInicio)) {
            throw new ErrorLogicaNegocioException("La fecha de cierre no puede ser anterior a la fecha de inicio.");
        }
        if (fechaCierre.isBefore(LocalDate.now())) {
            throw new ErrorLogicaNegocioException("La fecha de cierre ya pasó.");
        }
    }

    /** Ocupación de cupos de una convocatoria en tiempo real. */
    @Transactional(readOnly = true)
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
}
