package com.udea.agora_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Pesos del motor de recomendación (Servicio 1). Los cinco factores suman 1, y también los dos
 * componentes de la proximidad temática. Se pueden ajustar sin recompilar con las propiedades
 * {@code app.recomendacion.pesos.*}.
 *
 * @param habilidades            factor 1: habilidades del estudiante que el semillero ya usa
 *                               (las de sus postulaciones aceptadas)
 * @param habilidadesIntegrantes factor 5: habilidades del estudiante que ya tiene el equipo
 *                               (integrantes activos y estudiante líder)
 * @param areasTutores           factor 2: áreas de especialidad de los tutores
 * @param convocatoriaAbierta  factor 3: el semillero recibe postulaciones hoy
 * @param proximidadTematica   factor 4: líneas de investigación en común y mismo programa
 * @param lineasEnProximidad   peso de las líneas dentro del factor 4
 * @param programaEnProximidad peso del mismo programa dentro del factor 4
 */
@ConfigurationProperties("app.recomendacion.pesos")
public record PesosRecomendacion(
        @DefaultValue("0.30") double habilidades,
        @DefaultValue("0.15") double habilidadesIntegrantes,
        @DefaultValue("0.20") double areasTutores,
        @DefaultValue("0.20") double convocatoriaAbierta,
        @DefaultValue("0.15") double proximidadTematica,
        @DefaultValue("0.75") double lineasEnProximidad,
        @DefaultValue("0.25") double programaEnProximidad) {

    public PesosRecomendacion {
        Ponderacion.validarSumaUno("app.recomendacion.pesos",
                habilidades, habilidadesIntegrantes, areasTutores, convocatoriaAbierta, proximidadTematica);
        Ponderacion.validarSumaUno("app.recomendacion.pesos (proximidad temática)",
                lineasEnProximidad, programaEnProximidad);
    }
}
