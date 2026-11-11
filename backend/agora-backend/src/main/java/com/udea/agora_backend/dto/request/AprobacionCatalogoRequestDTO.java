package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Aprobación de una solicitud de catálogo. El administrador puede ajustar el nombre, la
 * descripción o el área antes de crear el elemento; lo que venga vacío se toma de la solicitud.
 */
@Data
public class AprobacionCatalogoRequestDTO {

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String nombre;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;

    private Integer idAreaEspecialidad;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String comentario;
}
