package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ConvocatoriaResponseDTO {
    private Integer id;
    private String titulo;
    private String descripcion;
    private Integer cuposTotales;
    private Integer cuposDisponibles;
    private LocalDate fechaInicio;
    private LocalDate fechaCierre;
    private String nombreSemillero;
    private String estadoActual;
}
