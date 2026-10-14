package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ConvocatoriaRequestDTO;
import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstadoRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.SemilleroRepository;
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
    private final EstadoRepository estadoRepository;
    private final SemilleroRepository semilleroRepository;
    private final PostulacionRepository postulacionRepository;

    private static final String ESTADO_ABIERTA = "Abierta";
    private static final String ESTADO_CERRADA = "Cerrada";
    private static final String ESTADO_SEMILLERO_ACTIVO = "Activo";

    /**
     * Obtiene todas las convocatorias
     */
    public List<ConvocatoriaResponseDTO> obtenerTodas() {
        return convocatoriaRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una convocatoria por su ID
     */
    public ConvocatoriaResponseDTO obtenerPorId(Integer id) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));
        return mapeoAResponseDTO(convocatoria);
    }

    /**
     * Obtiene convocatorias por semillero
     */
    public List<ConvocatoriaResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return convocatoriaRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene convocatorias activas con cupos disponibles
     */
    public List<ConvocatoriaResponseDTO> obtenerActivasConCupos() {
        Estado estadoAbierta = buscarEstado(ESTADO_ABIERTA);

        return convocatoriaRepository.findActivasConCupos(estadoAbierta.getId())
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene convocatorias expiradas (para procesamiento)
     */
    public List<ConvocatoriaResponseDTO> obtenerConvocatoriasExpiradas() {
        return convocatoriaRepository.findConvocatoriasExpiradas()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene convocatorias cuya fecha de cierre es anterior a la especificada
     */
    public List<ConvocatoriaResponseDTO> obtenerPorFechaCierreBefore(LocalDate fecha) {
        return convocatoriaRepository.findByFechaCierreBefore(fecha)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene convocatorias con cupos disponibles mayores a 0
     */
    public List<ConvocatoriaResponseDTO> obtenerConCuposDisponibles() {
        return convocatoriaRepository.findByCuposDisponiblesGreaterThan(0)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva convocatoria
     */
    public ConvocatoriaResponseDTO crear(ConvocatoriaRequestDTO request) {
        // Obtener semillero
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        if (!ESTADO_SEMILLERO_ACTIVO.equalsIgnoreCase(semillero.getEstado().getNombre())) {
            throw new ErrorLogicaNegocioException("Solo un semillero activo puede abrir convocatorias.");
        }
        if (request.getFechaInicio().isBefore(LocalDate.now())) {
            throw new ErrorLogicaNegocioException("La fecha de inicio no puede estar en el pasado.");
        }
        validarFechas(request.getFechaInicio(), request.getFechaCierre());

        Estado estado = buscarEstado(ESTADO_ABIERTA);

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
        return mapeoAResponseDTO(convocatoriaGuardada);
    }

    /**
     * Actualiza una convocatoria existente
     */
    public ConvocatoriaResponseDTO actualizar(Integer id, ConvocatoriaRequestDTO request) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));

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
        String estadoActual = convocatoria.getEstado().getNombre();
        if (convocatoria.getCuposDisponibles() == 0) {
            convocatoria.setEstado(buscarEstado(ESTADO_CERRADA));
        } else if (ESTADO_CERRADA.equalsIgnoreCase(estadoActual)
                && !convocatoria.getFechaCierre().isBefore(LocalDate.now())) {
            convocatoria.setEstado(buscarEstado(ESTADO_ABIERTA));
        }

        Convocatoria convocatoriaActualizada = convocatoriaRepository.save(convocatoria);
        return mapeoAResponseDTO(convocatoriaActualizada);
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

    private Estado buscarEstado(String nombre) {
        return estadoRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", nombre));
    }

    /**
     * Mapea entidad Convocatoria a ResponseDTO
     */
    private ConvocatoriaResponseDTO mapeoAResponseDTO(Convocatoria convocatoria) {
        return ConvocatoriaResponseDTO.builder()
                .id(convocatoria.getId())
                .idSemillero(convocatoria.getSemillero() != null ? convocatoria.getSemillero().getId() : null)
                .titulo(convocatoria.getTitulo())
                .descripcion(convocatoria.getDescripcion())
                .fechaInicio(convocatoria.getFechaInicio())
                .fechaCierre(convocatoria.getFechaCierre())
                .cuposTotales(convocatoria.getCuposTotales())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .nombreSemillero(convocatoria.getSemillero() != null ? convocatoria.getSemillero().getNombre() : null)
                .estadoActual(convocatoria.getEstado() != null ? convocatoria.getEstado().getNombre() : null)
                .build();
    }
}
