package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProfesorRequestDTO {

    @NotNull(message = "El ID del usuario base es obligatorio")
    private Integer idUsuario;

    @NotNull(message = "El ID del programa académico es obligatorio")
    private Integer idPrograma;

    @NotEmpty(message = "Debe seleccionar al menos un área de especialidad")
    private List<Integer> idAreasEspecialidad;
}
