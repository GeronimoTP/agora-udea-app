package com.udea.agora_backend.service;

/**
 * Nombres de las categorías de tbl_categorias_estado.
 * Los nombres de estado se repiten entre categorías ("Activo", "Expirada"...), por lo que
 * todo estado se busca siempre por categoría y nombre.
 */
public final class CategoriasEstado {

    public static final String USUARIO = "Estado Usuario";
    public static final String POSTULACION = "Estado Postulación";
    public static final String CONVOCATORIA = "Estado Convocatoria";
    public static final String SEMILLERO = "Estado Semillero";
    public static final String PROYECTO = "Estado Proyecto";

    // La vinculación estudiante-semillero no tiene categoría propia: usa los estados de usuario
    public static final String VINCULACION = USUARIO;

    private CategoriasEstado() {
    }
}
