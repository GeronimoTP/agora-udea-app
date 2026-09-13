package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PublicacionUsuarioRequestDTO {
    
    @NotNull(message = "El ID de la publicación es obligatorio")
    private Integer idPublicacion;
    
    @NotNull(message = "El ID del usuario es obligatorio")
    private Integer idUsuario;
    
    @NotNull(message = "El rol del autor es obligatorio")
    private Integer idRolAutor;
    
    @NotNull(message = "La fecha de asociación es obligatoria")
    private LocalDate fechaAsociacion;
}
