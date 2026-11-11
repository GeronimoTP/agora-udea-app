package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Solicitudes que esperan una respuesta del usuario de la sesión: los avisos del menú lateral.
 * El administrador ve semilleros y catálogo; los líderes, los retiros de sus semilleros.
 */
@Data
@Builder
public class PendientesResponseDTO {
    private long semillerosPorAprobar;
    private long solicitudesCatalogo;
    private long solicitudesRetiro;
}
