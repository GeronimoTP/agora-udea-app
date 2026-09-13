package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ProyectoResponseDTO {
    private Integer id;
    private String titulo;
    private String descripcion;
    private String nombreSemillero;
    private Integer presupuestoAsignado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estadoActual;
}
