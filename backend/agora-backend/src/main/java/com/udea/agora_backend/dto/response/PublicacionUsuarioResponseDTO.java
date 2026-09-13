package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class PublicacionUsuarioResponseDTO {
    private Integer id;
    private String nombrePublicacion;
    private String nombreUsuario;
    private String rolAutor;
    private LocalDate fechaAsociacion;
}
