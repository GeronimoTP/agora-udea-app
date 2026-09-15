package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.response.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para consultar diccionarios y catálogos estandarizados del sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final FacultadRepository facultadRepository;
    private final ProgramaAcademicoRepository programaAcademicoRepository;
    private final RolRepository rolRepository;
    private final RolAutorRepository rolAutorRepository;
    private final RolEstudianteProyectoRepository rolEstudianteProyectoRepository;
    private final TipoReconocimientoRepository tipoReconocimientoRepository;
    private final EntidadOtorganteRepository entidadOtorganteRepository;
    private final CategoriaEstadoRepository categoriaEstadoRepository;
    private final OauthProveedorRepository oauthProveedorRepository;

    public List<FacultadResponseDTO> obtenerFacultades() {
        return facultadRepository.findAll()
                .stream()
                .map(f -> FacultadResponseDTO.builder()
                        .id(f.getId())
                        .nombre(f.getNombre())
                        .descripcion(f.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ProgramaAcademicoResponseDTO> obtenerProgramasAcademicos() {
        return programaAcademicoRepository.findAll()
                .stream()
                .map(p -> ProgramaAcademicoResponseDTO.builder()
                        .id(p.getId())
                        .nombre(p.getNombre())
                        .nombreFacultad(p.getFacultad() != null ? p.getFacultad().getNombre() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public List<ProgramaAcademicoResponseDTO> obtenerProgramasPorFacultad(Integer idFacultad) {
        return programaAcademicoRepository.findByFacultadId(idFacultad)
                .stream()
                .map(p -> ProgramaAcademicoResponseDTO.builder()
                        .id(p.getId())
                        .nombre(p.getNombre())
                        .nombreFacultad(p.getFacultad() != null ? p.getFacultad().getNombre() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public List<RolResponseDTO> obtenerRoles() {
        return rolRepository.findAll()
                .stream()
                .map(r -> RolResponseDTO.builder()
                        .id(r.getId())
                        .nombre(r.getNombre())
                        .descripcion(r.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RolAutorResponseDTO> obtenerRolesAutor() {
        return rolAutorRepository.findAll()
                .stream()
                .map(ra -> RolAutorResponseDTO.builder()
                        .id(ra.getId())
                        .nombre(ra.getNombre())
                        .descripcion(ra.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RolEstudianteProyectoResponseDTO> obtenerRolesEstudianteProyecto() {
        return rolEstudianteProyectoRepository.findAll()
                .stream()
                .map(rep -> RolEstudianteProyectoResponseDTO.builder()
                        .id(rep.getId())
                        .nombre(rep.getNombre())
                        .descripcion(rep.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<TipoReconocimientoResponseDTO> obtenerTiposReconocimiento() {
        return tipoReconocimientoRepository.findAll()
                .stream()
                .map(tr -> TipoReconocimientoResponseDTO.builder()
                        .id(tr.getId())
                        .nombre(tr.getNombre())
                        .descripcion(tr.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<EntidadOtorganteResponseDTO> obtenerEntidadesOtorgantes() {
        return entidadOtorganteRepository.findAll()
                .stream()
                .map(eo -> EntidadOtorganteResponseDTO.builder()
                        .id(eo.getId())
                        .nombre(eo.getNombre())
                        .descripcion(eo.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<CategoriaEstadoResponseDTO> obtenerCategoriasEstado() {
        return categoriaEstadoRepository.findAll()
                .stream()
                .map(ce -> CategoriaEstadoResponseDTO.builder()
                        .id(ce.getId())
                        .nombre(ce.getNombre())
                        .descripcion(ce.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<OauthProveedorResponseDTO> obtenerProveedoresOAuth() {
        return oauthProveedorRepository.findAll()
                .stream()
                .map(op -> OauthProveedorResponseDTO.builder()
                        .id(op.getId())
                        .nombre(op.getNombre())
                        .descripcion(op.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }
}

