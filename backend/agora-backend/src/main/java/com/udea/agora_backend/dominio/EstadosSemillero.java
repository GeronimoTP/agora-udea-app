package com.udea.agora_backend.dominio;

/**
 * Estados de un semillero (categoría "Estado Semillero").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosSemillero {

    public static final String ACTIVO = "Activo";
    public static final String INACTIVO = "Inactivo";

    private EstadosSemillero() {
    }
}
