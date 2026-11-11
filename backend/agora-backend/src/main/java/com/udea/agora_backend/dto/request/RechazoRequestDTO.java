package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Rechazo de una solicitud (semillero, retiro o catálogo): el motivo es obligatorio. */
@Data
public class RechazoRequestDTO {

    @NotBlank(message = "Escribe el motivo del rechazo")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String motivo;
}
