package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/** Requisito de una convocatoria: habilidad del catálogo (idHabilidad) o texto libre (descripcion). */
@Data
@Builder
public class RequisitoResponseDTO {
    private Integer id;
    private Integer idHabilidad;
    private String nombreHabilidad;
    private String descripcion;
    private boolean obligatorio;
}
