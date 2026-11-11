package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Si una postulación cumple un requisito de su convocatoria. Uno de habilidad se cumple si la
 * habilidad está entre las declaradas en la postulación; uno de texto, si el estudiante
 * declaró cumplirlo.
 */
@Data
@Builder
public class RequisitoEvaluadoDTO {
    private Integer idRequisito;
    private String texto;
    private boolean deHabilidad;
    private boolean obligatorio;
    private boolean cumple;
}
