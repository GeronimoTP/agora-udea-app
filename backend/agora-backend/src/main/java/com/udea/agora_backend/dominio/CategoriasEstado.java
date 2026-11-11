package com.udea.agora_backend.dominio;

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
    // Solicitudes de retiro y de catálogo (migración V4)
    public static final String SOLICITUD = "Estado Solicitud";

    // La vinculación estudiante-semillero no tiene categoría propia: usa los estados de usuario
    public static final String VINCULACION = USUARIO;

    private CategoriasEstado() {
    }
}
