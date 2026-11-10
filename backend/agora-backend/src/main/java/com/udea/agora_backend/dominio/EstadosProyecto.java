package com.udea.agora_backend.dominio;

/**
 * Estados de un proyecto (categoría "Estado Proyecto").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados.
 */
public final class EstadosProyecto {

    public static final String EN_EJECUCION = "En ejecución";
    public static final String FINALIZADO = "Finalizado";
    public static final String CANCELADO = "Cancelado";

    private EstadosProyecto() {
    }
}
