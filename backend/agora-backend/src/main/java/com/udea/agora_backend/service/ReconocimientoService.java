package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ReconocimientoRequestDTO;
import com.udea.agora_backend.dto.response.ReconocimientoResponseDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.EntidadOtorgante;
import com.udea.agora_backend.model.ReconocimientoSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.TipoReconocimiento;
import com.udea.agora_backend.repository.EntidadOtorganteRepository;
import com.udea.agora_backend.repository.ReconocimientoSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.repository.TipoReconocimientoRepository;
import com.udea.agora_backend.service.mapper.ReconocimientoMapper;
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
    private final ReconocimientoMapper reconocimientoMapper;

    /**
     * Obtiene todos los reconocimientos
     */
    public List<ReconocimientoResponseDTO> obtenerTodos() {
        return reconocimientoRepository.findAll()
                .stream()
                .map(reconocimientoMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un reconocimiento por su ID
     */
    public ReconocimientoResponseDTO obtenerPorId(Integer id) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));
        return reconocimientoMapper.aResponse(reconocimiento);
    }

    /**
     * Obtiene reconocimientos por semillero
     */
    public List<ReconocimientoResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return reconocimientoRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(reconocimientoMapper::aResponse)
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
        return reconocimientoMapper.aResponse(guardado);
    }

    /**
     * Actualiza un reconocimiento existente
     */
    public ReconocimientoResponseDTO actualizar(Integer id, ReconocimientoRequestDTO request) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));

        if (!reconocimiento.getSemillero().getId().equals(request.getIdSemillero())) {
            throw new ErrorLogicaNegocioException("Un reconocimiento no puede cambiar de semillero.");
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
        return reconocimientoMapper.aResponse(actualizado);
    }

    /**
     * Elimina un reconocimiento por su ID
     */
    public void eliminar(Integer id) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ReconocimientoSemillero", id));
        reconocimientoRepository.delete(reconocimiento);
    }
}
