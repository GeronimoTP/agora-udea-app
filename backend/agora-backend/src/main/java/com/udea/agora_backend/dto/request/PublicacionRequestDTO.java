package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PublicacionRequestDTO {

    @NotBlank(message = "El título de la publicación es obligatorio")
    private String titulo;

    @NotBlank(message = "La URL del documento es obligatoria")
    @Pattern(regexp = "^https?://.*", message = "La URL debe comenzar con http:// o https://")
    private String urlDocumento;

    @NotNull(message = "La fecha de publicación es obligatoria")
    private LocalDate fechaPublicacion;

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Integer idProyecto;
}
