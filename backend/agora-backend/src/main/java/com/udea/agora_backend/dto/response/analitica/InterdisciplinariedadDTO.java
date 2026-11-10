package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vinculaciones de estudiantes de un programa distinto al del semillero. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterdisciplinariedadDTO {
    private Integer totalEstudiantesVinculados;
    private Long estudiantesInterdisciplinarios;
    private Double porcentajeInterdisciplinariedad;
}
