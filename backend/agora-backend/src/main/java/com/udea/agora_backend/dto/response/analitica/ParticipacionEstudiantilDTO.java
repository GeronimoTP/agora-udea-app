package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Porcentaje de autorías de publicaciones que corresponden a estudiantes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacionEstudiantilDTO {
    private Long totalAutoresRegistrados;
    private Long totalAutoresEstudiantes;
    private Double porcentajeParticipacionEstudiantil;
}
