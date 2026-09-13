package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReconocimientoRequestDTO {

    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;

    @NotNull(message = "El tipo de reconocimiento es obligatorio")
    private Integer idTipoReconocimiento;

    @NotBlank(message = "El título del reconocimiento es obligatorio")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "La entidad otorgante es obligatoria")
    private Integer idEntidadOtorgante;

    @NotNull(message = "La fecha de obtención es obligatoria")
    @PastOrPresent(message = "La fecha de obtención no puede ser en el futuro")
    private LocalDate fechaObtencion;

    @NotBlank(message = "La URL del certificado es obligatoria")
    private String urlCertificado;
}
