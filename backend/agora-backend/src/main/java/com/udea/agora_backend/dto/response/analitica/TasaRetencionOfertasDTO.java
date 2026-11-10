package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ofertas aceptadas frente a ofertas declinadas o expiradas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TasaRetencionOfertasDTO {
    private Long ofertasAceptadas;
    private Long ofertasRechazadasPorEstudiante;
    private Long ofertasExpiradas;
    private Long totalOfertasProcesadas;
    private Double porcentajeRetencion;
    private Double porcentajeRechazoYExpiracion;
}
