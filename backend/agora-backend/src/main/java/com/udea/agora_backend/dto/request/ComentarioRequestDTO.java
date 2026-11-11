package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Comentario opcional al aprobar una solicitud. */
@Data
public class ComentarioRequestDTO {

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String comentario;
}
