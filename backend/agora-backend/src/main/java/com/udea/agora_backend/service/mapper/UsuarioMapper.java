package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.UsuarioResponseDTO;
import com.udea.agora_backend.model.Usuario;
import org.springframework.stereotype.Component;

/** Convierte usuarios en DTOs de respuesta. */
@Component
public class UsuarioMapper {

    public UsuarioResponseDTO aResponse(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .nombreRol(usuario.getRol().getNombre())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
