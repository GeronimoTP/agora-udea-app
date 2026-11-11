package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import com.udea.agora_backend.dto.request.HabilidadRequestDTO;
import com.udea.agora_backend.dto.response.HabilidadResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.repository.HabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catálogo de habilidades técnicas y blandas que declaran estudiantes y postulaciones. */
@Service
@RequiredArgsConstructor
@Transactional
public class HabilidadService {

    private final HabilidadRepository habilidadRepository;
    private final NombresCatalogo nombresCatalogo;

    @Transactional(readOnly = true)
    public List<HabilidadResponseDTO> obtenerTodas() {
        return habilidadRepository.findAll().stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public HabilidadResponseDTO obtenerPorId(Integer id) {
        return aResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<HabilidadResponseDTO> buscarPorNombre(String nombre) {
        return habilidadRepository.findByNombreContainingIgnoreCaseOrderByNombre(nombre).stream()
                .map(this::aResponse)
                .toList();
    }

    public HabilidadResponseDTO crear(HabilidadRequestDTO request) {
        nombresCatalogo.validarLibre(TipoElementoCatalogo.HABILIDAD, request.getNombre());
        return aResponse(habilidadRepository.save(Habilidad.builder().nombre(request.getNombre()).build()));
    }

    public HabilidadResponseDTO actualizar(Integer id, HabilidadRequestDTO request) {
        Habilidad habilidad = buscar(id);
        nombresCatalogo.validarRenombre(TipoElementoCatalogo.HABILIDAD, habilidad.getNombre(), request.getNombre());
        habilidad.setNombre(request.getNombre());
        return aResponse(habilidadRepository.save(habilidad));
    }

    public void eliminar(Integer id) {
        habilidadRepository.delete(buscar(id));
    }

    private Habilidad buscar(Integer id) {
        return habilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", id));
    }

    private HabilidadResponseDTO aResponse(Habilidad habilidad) {
        return HabilidadResponseDTO.builder()
                .id(habilidad.getId())
                .nombre(habilidad.getNombre())
                .build();
    }
}
