package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Profesor en el listado de búsqueda (elegir tutores). Sin correo. */
@Data
@Builder
public class ProfesorResumenDTO {
    private Integer id;
    private String nombreCompleto;
    private Integer idPrograma;
    private String nombrePrograma;
    private Integer idFacultad;
    private String nombreFacultad;
    private List<String> areasEspecialidad;
}
