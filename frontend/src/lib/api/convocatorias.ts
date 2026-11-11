/**
 * CONVOCATORIAS abiertas y por semillero para las vistas públicas. La gestión (crear, editar,
 * cerrar) está en ./gestion.
 */

import { apiRequest } from "./client";
import type { Convocatoria, RequisitoConvocatoria } from "@/types/api";

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
  requisitos: RequisitoApi[] | null;
}

// RequisitoResponseDTO
export interface RequisitoApi {
  id: number;
  idHabilidad: number | null;
  nombreHabilidad: string | null;
  descripcion: string | null;
  obligatorio: boolean;
}

export function mapearRequisito(
  requisito: RequisitoApi,
): RequisitoConvocatoria {
  return {
    id: requisito.id,
    idHabilidad: requisito.idHabilidad,
    texto: requisito.nombreHabilidad ?? requisito.descripcion ?? "",
    obligatorio: requisito.obligatorio,
  };
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
    requisitos: (convocatoria.requisitos ?? []).map(mapearRequisito),
  };
}

function normalizarEstadoConvocatoria(estado: string): Convocatoria["estado"] {
  const normalizado = estado.toLocaleLowerCase();
  if (normalizado.includes("abierta")) return "ABIERTA";
  if (normalizado.includes("borrador")) return "BORRADOR";
  return "CERRADA";
}
