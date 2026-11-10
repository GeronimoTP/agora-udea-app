package com.udea.agora_backend.util;

/** Redondeo de los valores que la API devuelve (porcentajes, promedios, distancias). */
public final class Redondeo {

    private Redondeo() {
    }

    public static double unDecimal(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    public static double dosDecimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static double tresDecimales(double valor) {
        return Math.round(valor * 1000.0) / 1000.0;
    }
}
