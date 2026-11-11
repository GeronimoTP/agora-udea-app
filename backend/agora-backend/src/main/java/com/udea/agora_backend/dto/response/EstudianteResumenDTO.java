package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Estudiante en el listado de búsqueda (elegir líder de un semillero nuevo). Sin correo ni
 * datos personales: el perfil completo solo lo ven él, el administrador y sus líderes.
 */
@Data
@Builder
public class EstudianteResumenDTO {
    private Integer id;
    private String nombreCompleto;
    private String nombrePrograma;
    private Integer semestre;
    // Ya lidera un semillero (aprobado, pendiente o rechazado): no puede liderar otro
    private boolean lideraSemillero;
}
