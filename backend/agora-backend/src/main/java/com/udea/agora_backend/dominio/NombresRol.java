package com.udea.agora_backend.dominio;

/**
 * Nombres de los roles de cuenta (tbl_roles). SecurityConfig los usa en hasRole(...), así que
 * deben coincidir exactamente, incluida la capitalización.
 */
public final class NombresRol {

    public static final String ESTUDIANTE = "Estudiante";
    public static final String PROFESOR = "Profesor";
    public static final String ADMINISTRADOR = "Administrador";

    private NombresRol() {
    }
}
