package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Publicaciones promedio por proyecto. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionProyectoPublicacionDTO {
    private Long totalProyectos;
    private Long totalPublicaciones;
    private Double promedioPublicacionesPorProyecto;
}
