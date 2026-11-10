package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Frecuencia de una habilidad en postulaciones aceptadas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabilidadFrecuenciaDTO {
    private String habilidad;
    private Long frecuencia;
}
