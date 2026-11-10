package com.udea.agora_backend.config;

import java.util.Arrays;

/** Validaciones comunes de los pesos configurables de los algoritmos. */
final class Ponderacion {

    private static final double TOLERANCIA = 1e-6;

    private Ponderacion() {
    }

    /** Falla al arrancar si algún peso es negativo o si no suman 1. */
    static void validarSumaUno(String propiedad, double... pesos) {
        if (Arrays.stream(pesos).anyMatch(peso -> peso < 0)) {
            throw new IllegalArgumentException("Los pesos de '" + propiedad + "' no pueden ser negativos.");
        }
        double suma = Arrays.stream(pesos).sum();
        if (Math.abs(suma - 1.0) > TOLERANCIA) {
            throw new IllegalArgumentException(
                    "Los pesos de '" + propiedad + "' deben sumar 1; suman " + suma + ".");
        }
    }
}
