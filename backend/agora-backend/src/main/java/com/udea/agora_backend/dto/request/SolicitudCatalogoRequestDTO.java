package com.udea.agora_backend.dto.request;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Un estudiante o profesor pide agregar un elemento al catálogo. La descripción es obligatoria
 * salvo para habilidades; el área de especialidad, solo para líneas de investigación.
 */
@Data
public class SolicitudCatalogoRequestDTO {

    @NotNull(message = "Indica qué tipo de elemento solicitas")
    private TipoElementoCatalogo tipo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "Máximo 150 caracteres")
    private String nombre;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;

    private Integer idAreaEspecialidad;

    @NotBlank(message = "Explica por qué debería agregarse")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String justificacion;
}
