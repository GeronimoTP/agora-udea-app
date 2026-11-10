package com.udea.agora_backend.dominio;

/**
 * Estados de la vinculación de un integrante a un semillero (usa la categoría "Estado Usuario").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosVinculacion {

    public static final String ACTIVA = "Activo";
    public static final String INACTIVA = "Inactivo";

    private EstadosVinculacion() {
    }
}
