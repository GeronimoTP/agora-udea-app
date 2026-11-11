package com.udea.agora_backend.dominio;

/**
 * Elementos del catálogo que estudiantes y profesores pueden solicitar al administrador.
 * El nombre del valor se guarda en tbl_solicitudes_catalogo.tipo.
 */
public enum TipoElementoCatalogo {
    HABILIDAD("Habilidad", false),
    LINEA_INVESTIGACION("Línea de investigación", true),
    AREA_ESPECIALIDAD("Área de especialidad", true),
    ENTIDAD_OTORGANTE("Entidad otorgante", true),
    TIPO_RECONOCIMIENTO("Tipo de reconocimiento", true);

    private final String etiqueta;
    // Las tablas de estos catálogos tienen descripción obligatoria; la de habilidades no
    private final boolean requiereDescripcion;

    TipoElementoCatalogo(String etiqueta, boolean requiereDescripcion) {
        this.etiqueta = etiqueta;
        this.requiereDescripcion = requiereDescripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean requiereDescripcion() {
        return requiereDescripcion;
    }
}
