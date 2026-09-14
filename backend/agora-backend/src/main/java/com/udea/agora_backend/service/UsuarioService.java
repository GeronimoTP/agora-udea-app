package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.UsuarioRequestDTO;
import com.udea.agora_backend.dto.response.UsuarioResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.model.OauthProveedor;
import com.udea.agora_backend.model.Rol;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.repository.OauthProveedorRepository;
import com.udea.agora_backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de usuarios.
 * Maneja operaciones CRUD y búsquedas relacionadas con usuarios.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final OauthProveedorRepository oauthProveedorRepository;
    private final RolRepository rolRepository;

    /**
     * Obtiene todos los usuarios del sistema
     */
    public List<UsuarioResponseDTO> obtenerTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un usuario por su ID
     */
    public UsuarioResponseDTO obtenerPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        return mapeoAResponseDTO(usuario);
    }

    /**
     * Obtiene un usuario por email
     */
    public UsuarioResponseDTO obtenerPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "email", email));
        return mapeoAResponseDTO(usuario);
    }

    /**
     * Obtiene un usuario por OAuth ID
     */
    public UsuarioResponseDTO obtenerPorOauthId(String oauthId) {
        Usuario usuario = usuarioRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "oauthId", oauthId));
        return mapeoAResponseDTO(usuario);
    }

    /**
     * Crea un nuevo usuario
     * Valida que no exista usuario con email o oauthId duplicados
     */
    public UsuarioResponseDTO crear(UsuarioRequestDTO request) {
        // Verificar email duplicado
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictoException("Usuario", "email", request.getEmail());
        }

        // Verificar oauthId duplicado
        if (usuarioRepository.findByOauthId(request.getOauthId()).isPresent()) {
            throw new ConflictoException("Usuario", "oauthId", request.getOauthId());
        }

        // Obtener proveedor OAuth
        OauthProveedor proveedor = oauthProveedorRepository.findById(request.getIdProveedorOauth())
                .orElseThrow(() -> new RecursoNoEncontradoException("OauthProveedor", request.getIdProveedorOauth()));

        // Obtener rol
        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", request.getIdRol()));

        // Crear usuario
        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto())
                .email(request.getEmail())
                .proveedorOauth(proveedor)
                .oauthId(request.getOauthId())
                .rol(rol)
                .createdAt(ZonedDateTime.now())
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return mapeoAResponseDTO(usuarioGuardado);
    }

    /**
     * Actualiza un usuario existente
     */
    public UsuarioResponseDTO actualizar(Integer id, UsuarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        // Validar email no duplicado (si cambió)
        if (!usuario.getEmail().equals(request.getEmail()) &&
                usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictoException("Usuario", "email", request.getEmail());
        }

        // Obtener nuevos proveedor y rol
        OauthProveedor proveedor = oauthProveedorRepository.findById(request.getIdProveedorOauth())
                .orElseThrow(() -> new RecursoNoEncontradoException("OauthProveedor", request.getIdProveedorOauth()));

        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", request.getIdRol()));

        // Actualizar campos
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setEmail(request.getEmail());
        usuario.setProveedorOauth(proveedor);
        usuario.setRol(rol);

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return mapeoAResponseDTO(usuarioActualizado);
    }

    /**
     * Elimina un usuario por su ID
     */
    public void eliminar(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        usuarioRepository.delete(usuario);
    }

    /**
     * Mapea entidad Usuario a ResponseDTO
     */
    private UsuarioResponseDTO mapeoAResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .nombreRol(usuario.getRol().getNombre())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
