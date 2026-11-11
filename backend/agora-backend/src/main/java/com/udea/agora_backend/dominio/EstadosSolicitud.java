package com.udea.agora_backend.dominio;

/**
 * Estados de una solicitud de retiro o de catálogo (categoría "Estado Solicitud", migración V4).
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosSolicitud {
    public static final String PENDIENTE = "Pendiente";
    public static final String APROBADA = "Aprobada";
    public static final String RECHAZADA = "Rechazada";

    private EstadosSolicitud() {
    }
}
