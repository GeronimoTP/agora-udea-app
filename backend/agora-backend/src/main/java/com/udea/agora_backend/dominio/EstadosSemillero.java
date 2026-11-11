package com.udea.agora_backend.dominio;

/**
 * Estados de un semillero (categoría "Estado Semillero").
 * Deben coincidir exactamente con los nombres sembrados en tbl_estados. Toda búsqueda de un
 * estado usa también su categoría ({@link CategoriasEstado}), porque los nombres se repiten.
 */
public final class EstadosSemillero {

    public static final String ACTIVO = "Activo";
    public static final String INACTIVO = "Inactivo";
    // Aprobación del administrador (migración V4): un semillero recién registrado no es visible
    // ni opera hasta que el administrador lo aprueba; si lo rechaza, sus líderes pueden
    // corregirlo y reenviarlo
    public static final String PENDIENTE_APROBACION = "Pendiente de aprobación";
    public static final String RECHAZADO = "Rechazado";

    private EstadosSemillero() {
    }
}
