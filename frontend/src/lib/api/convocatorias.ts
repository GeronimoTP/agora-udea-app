/**
 * CONVOCATORIAS abiertas y por semillero para las vistas públicas. La gestión (crear, editar,
 * cerrar) está en ./gestion.
 */

import { apiRequest } from "./client";
import type { Convocatoria } from "@/types/api";

export const convocatoriasService = {
  listarAbiertas: async () =>
    (await apiRequest<ConvocatoriaApi[]>("/api/convocatorias/activas")).map(
      mapearConvocatoria,
    ),

  porSemillero: async (idSemillero: number) =>
    (
      await apiRequest<ConvocatoriaApi[]>(
        `/api/convocatorias/semillero/${idSemillero}`,
      )
    ).map(mapearConvocatoria),
};

interface ConvocatoriaApi {
  id: number;
  idSemillero: number;
  titulo: string;
  descripcion: string;
  fechaInicio: string;
  fechaCierre: string;
  cuposTotales: number;
  cuposDisponibles: number;
  nombreSemillero: string;
  estadoActual: string;
}

function mapearConvocatoria(convocatoria: ConvocatoriaApi): Convocatoria {
  return {
    idConvocatoria: convocatoria.id,
    idSemillero: convocatoria.idSemillero,
    nombreSemillero: convocatoria.nombreSemillero,
    titulo: convocatoria.titulo,
    descripcion: convocatoria.descripcion,
    fechaApertura: convocatoria.fechaInicio,
    fechaCierre: convocatoria.fechaCierre,
    cuposTotales: convocatoria.cuposTotales,
    cuposDisponibles: convocatoria.cuposDisponibles,
    estado: normalizarEstadoConvocatoria(convocatoria.estadoActual),
    // El modelo de convocatoria no registra habilidades requeridas
    habilidadesRequeridas: [],
  };
}

function normalizarEstadoConvocatoria(estado: string): Convocatoria["estado"] {
  const normalizado = estado.toLocaleLowerCase();
  if (normalizado.includes("abierta")) return "ABIERTA";
  if (normalizado.includes("borrador")) return "BORRADOR";
  return "CERRADA";
}
