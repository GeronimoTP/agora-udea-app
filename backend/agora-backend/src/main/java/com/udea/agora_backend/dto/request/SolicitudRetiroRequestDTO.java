package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Un integrante pide salir de un semillero. El estudiante sale del token, no del body. */
@Data
public class SolicitudRetiroRequestDTO {

    @NotNull(message = "Indica el semillero")
    private Integer idSemillero;

    @NotBlank(message = "Cuéntale al líder por qué quieres retirarte")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String motivo;
}
