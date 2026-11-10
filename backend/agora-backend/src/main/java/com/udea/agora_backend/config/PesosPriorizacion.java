package com.udea.agora_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Pesos de la priorización de candidatos (complemento del Servicio 2). Suman 1. Se pueden
 * ajustar con las propiedades {@code app.priorizacion.pesos.*}.
 *
 * @param tematica       líneas de investigación del estudiante que coinciden con las del semillero
 * @param habilidades    habilidades de la postulación que el semillero ya usa
 * @param disponibilidad horas semanales declaradas
 * @param compromiso     cómo respondió el estudiante a ofertas anteriores
 */
@ConfigurationProperties("app.priorizacion.pesos")
public record PesosPriorizacion(
        @DefaultValue("0.35") double tematica,
        @DefaultValue("0.25") double habilidades,
        @DefaultValue("0.20") double disponibilidad,
        @DefaultValue("0.20") double compromiso) {

    public PesosPriorizacion {
        Ponderacion.validarSumaUno("app.priorizacion.pesos", tematica, habilidades, disponibilidad, compromiso);
    }
}
