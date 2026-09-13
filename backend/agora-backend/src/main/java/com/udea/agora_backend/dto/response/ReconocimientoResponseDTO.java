package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ReconocimientoResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private String tipoReconocimiento;
    private String titulo;
    private String descripcion;
    private String entidadOtorgante;
    private LocalDate fechaObtencion;
    private String urlCertificado;
}
