package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ConvocatoriaRequestDTO;
import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstadoRepository;
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
     * Obtiene convocatorias activas con cupos disponibles
     */
    public List<ConvocatoriaResponseDTO> obtenerActivasConCupos() {
        // Obtener estado "Activa"
        Estado estadoActiva = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Activa"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Activa"));
        
        return convocatoriaRepository.findActivasConCupos(estadoActiva.getId())
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
        // Obtener estado inicial (Activa)
        Estado estado = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Activa"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Activa"));

        // Crear convocatoria
        Convocatoria convocatoria = Convocatoria.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .fechaInicio(request.getFechaInicio())
                .fechaCierre(request.getFechaCierre())
                .cuposDisponibles(request.getCuposTotales())
                .cuposTotales(request.getCuposTotales())
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

        // Actualizar campos
        convocatoria.setTitulo(request.getTitulo());
        convocatoria.setDescripcion(request.getDescripcion());
        convocatoria.setFechaInicio(request.getFechaInicio());
        convocatoria.setFechaCierre(request.getFechaCierre());
        convocatoria.setCuposTotales(request.getCuposTotales());

        Convocatoria convocatoriaActualizada = convocatoriaRepository.save(convocatoria);
        return mapeoAResponseDTO(convocatoriaActualizada);
    }

    /**
     * Elimina una convocatoria por su ID
     */
    public void eliminar(Integer id) {
        Convocatoria convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", id));
        convocatoriaRepository.delete(convocatoria);
    }

    /**
     * Mapea entidad Convocatoria a ResponseDTO
     */
    private ConvocatoriaResponseDTO mapeoAResponseDTO(Convocatoria convocatoria) {
        return ConvocatoriaResponseDTO.builder()
                .id(convocatoria.getId())
                .titulo(convocatoria.getTitulo())
                .descripcion(convocatoria.getDescripcion())
                .fechaInicio(convocatoria.getFechaInicio())
                .fechaCierre(convocatoria.getFechaCierre())
                .cuposTotales(convocatoria.getCuposTotales())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .nombreSemillero(convocatoria.getSemillero().getNombre())
                .estadoActual(convocatoria.getEstado().getNombre())
                .build();
    }
}
