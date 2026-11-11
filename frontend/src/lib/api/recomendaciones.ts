/**
 * SERVICIO 1: ranking ponderado de semilleros (motor de recomendación) y "Semilleros similares
 * a los tuyos" (vecinos más cercanos en el árbol KD del backend).
 */

import { apiRequest } from "./client";
import type { ResultadoMatch, SemillerosSimilares } from "@/types/api";

export const matchingService = {
  // El estudiante sale del token: no se envía su ID
  recomendaciones: async () => {
    const recomendaciones = await apiRequest<RecomendacionApi[]>(
      "/api/recomendaciones/mias",
    );
    return recomendaciones.map(
      (recomendacion) =>
        ({
          idSemillero: recomendacion.idSemillero,
          nombre: recomendacion.nombreSemillero,
          porcentajeMatch: recomendacion.porcentajeMatch,
          factoresCoincidencia: [
            ["Habilidades", recomendacion.factor1Habilidades],
            ["Equipo", recomendacion.factor5HabilidadesIntegrantes],
            ["Especialidad", recomendacion.factor2AreaEspecialidad],
            ["Cupos", recomendacion.factor3DisponibilidadConvocatorias],
            ["Temática", recomendacion.factor4ProximidadTematica],
          ]
            .filter(([, value]) => typeof value === "number" && value > 0)
            .map(([label, value]) => `${label}: ${Math.round(Number(value))}%`),
          desglose: {
            habilidades: recomendacion.factor1Habilidades ?? 0,
            habilidadesEquipo: recomendacion.factor5HabilidadesIntegrantes ?? 0,
            areasEspecialidad: recomendacion.factor2AreaEspecialidad ?? 0,
            convocatoriaAbierta:
              recomendacion.factor3DisponibilidadConvocatorias ?? 0,
            proximidadTematica: recomendacion.factor4ProximidadTematica ?? 0,
          },
          convocatoriaAbierta:
            (recomendacion.factor3DisponibilidadConvocatorias ?? 0) > 0,
          facultad: recomendacion.nombrePrograma ?? "",
        }) satisfies ResultadoMatch,
    );
  },
};

export const semillerosSimilaresService = {
  // Vecinos más cercanos en el árbol KD del backend al historial del estudiante
  buscar: async (limite = 5) =>
    mapearSemillerosSimilares(
      await apiRequest<SemillerosSimilaresApi>(
        `/api/recomendaciones/mias/similares?limite=${limite}`,
      ),
    ),
};

interface RecomendacionApi {
  idSemillero: number;
  nombreSemillero: string;
  nombrePrograma: string;
  porcentajeMatch: number;
  factor1Habilidades: number;
  factor2AreaEspecialidad: number;
  factor3DisponibilidadConvocatorias: number;
  factor4ProximidadTematica: number;
  factor5HabilidadesIntegrantes: number;
}

interface SemillerosSimilaresApi {
  base: SemillerosSimilares["base"];
  referencias: SemillerosSimilares["referencias"] | null;
  similares:
    | {
        idSemillero: number;
        nombre: string;
        codigoIdentificador: string;
        nombrePrograma: string | null;
        porcentajeSimilitud: number;
        distancia: number;
        areasEnComun: string[] | null;
        lineasEnComun: string[] | null;
        parecidoA: string | null;
        convocatoriaAbierta: boolean;
      }[]
    | null;
  busqueda: SemillerosSimilares["busqueda"];
}

function mapearSemillerosSimilares(
  respuesta: SemillerosSimilaresApi,
): SemillerosSimilares {
  return {
    base: respuesta.base,
    referencias: respuesta.referencias ?? [],
    similares: (respuesta.similares ?? []).map((similar) => ({
      idSemillero: similar.idSemillero,
      nombre: similar.nombre,
      codigoIdentificador: similar.codigoIdentificador,
      nombrePrograma: similar.nombrePrograma ?? null,
      porcentajeSimilitud: similar.porcentajeSimilitud,
      distancia: similar.distancia,
      areasEnComun: similar.areasEnComun ?? [],
      lineasEnComun: similar.lineasEnComun ?? [],
      parecidoA: similar.parecidoA ?? null,
      convocatoriaAbierta: similar.convocatoriaAbierta,
    })),
    busqueda: respuesta.busqueda,
  };
}
