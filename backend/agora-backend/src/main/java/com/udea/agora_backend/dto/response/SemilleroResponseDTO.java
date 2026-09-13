package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemilleroResponseDTO {
    private Integer id;
    private String codigoIdentificador;
    private String nombre;
    private String descripcion;
    private String nombreEstudianteLider;
    private String nombrePrograma;
    private String estadoActual;
}
