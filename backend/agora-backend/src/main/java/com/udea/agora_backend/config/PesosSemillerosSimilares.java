package com.udea.agora_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Peso de cada semillero del historial del estudiante al calcular el punto de consulta del
 * árbol KD ("Semilleros similares a los tuyos"). Se ajustan con
 * {@code app.recomendacion.kd.pesos.*}.
 *
 * @param actual   semilleros en los que el estudiante está hoy (integrante activo o líder)
 * @param anterior semilleros de los que se retiró; debe pesar menos que uno actual
 */
@ConfigurationProperties("app.recomendacion.kd.pesos")
public record PesosSemillerosSimilares(
        @DefaultValue("1.0") double actual,
        @DefaultValue("0.5") double anterior) {

    public PesosSemillerosSimilares {
        if (anterior <= 0 || anterior >= actual) {
            throw new IllegalArgumentException(
                    "app.recomendacion.kd.pesos: se requiere 0 < anterior < actual (anterior="
                            + anterior + ", actual=" + actual + ").");
        }
    }
}
