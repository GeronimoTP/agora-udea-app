package com.udea.agora_backend.dominio;

/**
 * Estados de una postulación (categoría "Estado Postulación").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosPostulacion {

    public static final String PENDIENTE = "Pendiente";
    public static final String PRE_APROBADA = "Pre-aprobada";
    public static final String ACEPTADA = "Aceptada";
    public static final String RECHAZADA = "Rechazada";
    public static final String RECHAZADA_POR_ESTUDIANTE = "Rechazada por Estudiante";
    public static final String CANCELADA_EN_CASCADA = "Cancelada en cascada";
    public static final String EXPIRADA = "Expirada";

    private EstadosPostulacion() {
    }
}
