package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Requisito de una convocatoria: una habilidad del catálogo O un texto libre. */
@Data
public class RequisitoRequestDTO {

    private Integer idHabilidad;

    @Size(max = 300, message = "Un requisito tiene máximo 300 caracteres")
    private String descripcion;

    @NotNull(message = "Indica si el requisito es obligatorio")
    private Boolean obligatorio;
}
