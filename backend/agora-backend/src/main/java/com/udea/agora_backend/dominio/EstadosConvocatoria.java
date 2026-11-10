package com.udea.agora_backend.dominio;

/**
 * Estados de una convocatoria (categoría "Estado Convocatoria").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosConvocatoria {

    public static final String ABIERTA = "Abierta";
    public static final String CERRADA = "Cerrada";
    public static final String CANCELADA = "Cancelada";

    private EstadosConvocatoria() {
    }
}
