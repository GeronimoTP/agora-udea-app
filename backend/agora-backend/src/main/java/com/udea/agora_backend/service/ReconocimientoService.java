package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ReconocimientoRequestDTO;
import com.udea.agora_backend.dto.response.ReconocimientoResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.EntidadOtorgante;
import com.udea.agora_backend.model.ReconocimientoSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.TipoReconocimiento;
import com.udea.agora_backend.repository.EntidadOtorganteRepository;
import com.udea.agora_backend.repository.ReconocimientoSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.repository.TipoReconocimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de reconocimientos y logros de semilleros.
 * Maneja operaciones CRUD y consultas de galardones obtenidos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReconocimientoService {

    private final ReconocimientoSemilleroRepository reconocimientoRepository;
    private final SemilleroRepository semilleroRepository;
    private final TipoReconocimientoRepository tipoReconocimientoRepository;
    private final EntidadOtorganteRepository entidadOtorganteRepository;

    /**
     * Obtiene todos los reconocimientos
     */
    public List<ReconocimientoResponseDTO> obtenerTodos() {
        return reconocimientoRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un reconocimiento por su ID
     */
    public ReconocimientoResponseDTO obtenerPorId(Integer id) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));
        return mapeoAResponseDTO(reconocimiento);
    }

    /**
     * Obtiene reconocimientos por semillero
     */
    public List<ReconocimientoResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return reconocimientoRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo reconocimiento
     */
    public ReconocimientoResponseDTO crear(ReconocimientoRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        TipoReconocimiento tipo = tipoReconocimientoRepository.findById(request.getIdTipoReconocimiento())
                .orElseThrow(() -> new RecursoNoEncontradoException("TipoReconocimiento", request.getIdTipoReconocimiento()));

        EntidadOtorgante entidad = entidadOtorganteRepository.findById(request.getIdEntidadOtorgante())
                .orElseThrow(() -> new RecursoNoEncontradoException("EntidadOtorgante", request.getIdEntidadOtorgante()));

        ReconocimientoSemillero reconocimiento = ReconocimientoSemillero.builder()
                .semillero(semillero)
                .tipoReconocimiento(tipo)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .entidadOtorgante(entidad)
                .fechaObtencion(request.getFechaObtencion())
                .urlCertificado(request.getUrlCertificado())
                .createdAt(ZonedDateTime.now())
                .build();

        ReconocimientoSemillero guardado = reconocimientoRepository.save(reconocimiento);
        return mapeoAResponseDTO(guardado);
    }

    /**
     * Actualiza un reconocimiento existente
     */
    public ReconocimientoResponseDTO actualizar(Integer id, ReconocimientoRequestDTO request) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));

        if (!reconocimiento.getSemillero().getId().equals(request.getIdSemillero())) {
            Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));
            reconocimiento.setSemillero(semillero);
        }

        if (!reconocimiento.getTipoReconocimiento().getId().equals(request.getIdTipoReconocimiento())) {
            TipoReconocimiento tipo = tipoReconocimientoRepository.findById(request.getIdTipoReconocimiento())
                    .orElseThrow(() -> new RecursoNoEncontradoException("TipoReconocimiento", request.getIdTipoReconocimiento()));
            reconocimiento.setTipoReconocimiento(tipo);
        }

        if (!reconocimiento.getEntidadOtorgante().getId().equals(request.getIdEntidadOtorgante())) {
            EntidadOtorgante entidad = entidadOtorganteRepository.findById(request.getIdEntidadOtorgante())
                    .orElseThrow(() -> new RecursoNoEncontradoException("EntidadOtorgante", request.getIdEntidadOtorgante()));
            reconocimiento.setEntidadOtorgante(entidad);
        }

        reconocimiento.setTitulo(request.getTitulo());
        reconocimiento.setDescripcion(request.getDescripcion());
        reconocimiento.setFechaObtencion(request.getFechaObtencion());
        reconocimiento.setUrlCertificado(request.getUrlCertificado());

        ReconocimientoSemillero actualizado = reconocimientoRepository.save(reconocimiento);
        return mapeoAResponseDTO(actualizado);
    }

    /**
     * Elimina un reconocimiento por su ID
     */
    public void eliminar(Integer id) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));
        reconocimientoRepository.delete(reconocimiento);
    }

    /**
     * Mapea entidad ReconocimientoSemillero a ResponseDTO
     */
    private ReconocimientoResponseDTO mapeoAResponseDTO(ReconocimientoSemillero reconocimiento) {
        return ReconocimientoResponseDTO.builder()
                .id(reconocimiento.getId())
                .nombreSemillero(reconocimiento.getSemillero() != null ? reconocimiento.getSemillero().getNombre() : null)
                .tipoReconocimiento(reconocimiento.getTipoReconocimiento() != null ? reconocimiento.getTipoReconocimiento().getNombre() : null)
                .titulo(reconocimiento.getTitulo())
                .descripcion(reconocimiento.getDescripcion())
                .entidadOtorgante(reconocimiento.getEntidadOtorgante() != null ? reconocimiento.getEntidadOtorgante().getNombre() : null)
                .fechaObtencion(reconocimiento.getFechaObtencion())
                .urlCertificado(reconocimiento.getUrlCertificado())
                .build();
    }
}

