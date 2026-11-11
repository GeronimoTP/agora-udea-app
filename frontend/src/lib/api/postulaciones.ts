/**
 * POSTULACIONES y flujo de doble vía: el estudiante se postula y responde ofertas; el líder
 * pre-aprueba o rechaza y consulta la priorización de candidatos (Servicio 2).
 */

import { apiRequest } from "./client";
import type {
  EstadoPostulacion,
  Postulacion,
  PriorizacionCandidato,
  RequisitoEvaluado,
} from "@/types/api";

export const postulacionesService = {
  // El estudiante sale del token: no se envía su ID
  misPostulaciones: async () =>
    (await apiRequest<PostulacionApi[]>("/api/postulaciones/mias")).map(
      mapearPostulacion,
    ),

  recibidas: async (idSemillero: number) =>
    (
      await apiRequest<PostulacionApi[]>(
        `/api/postulaciones/semillero/${idSemillero}`,
      )
    ).map(mapearPostulacion),

  crear: (payload: NuevaPostulacion) =>
    apiRequest<unknown>("/api/postulaciones", {
      method: "POST",
      body: payload,
    }),

  preAprobar: (idPostulacion: number) =>
    apiRequest<string>(`/api/postulaciones/${idPostulacion}/pre-aprobar`, {
      method: "POST",
    }),

  rechazar: (idPostulacion: number) =>
    apiRequest<string>(`/api/postulaciones/${idPostulacion}/rechazar-lider`, {
      method: "POST",
    }),

  aceptarOferta: (idPostulacion: number) =>
    apiRequest<string>(
      `/api/postulaciones/${idPostulacion}/confirmar-aceptacion`,
      {
        method: "POST",
      },
    ),

  rechazarOferta: (idPostulacion: number) =>
    apiRequest<string>(
      `/api/postulaciones/${idPostulacion}/rechazar-estudiante`,
      {
        method: "POST",
      },
    ),

  // Servicio 2: candidatos en proceso ordenados por puntaje dentro de cada convocatoria
  priorizacion: async (idSemillero: number) =>
    (
      await apiRequest<CandidatoPriorizadoApi[]>(
        `/api/postulaciones/semillero/${idSemillero}/priorizacion`,
      )
    ).map(mapearPriorizacion),
};

// Cuerpo esperado por PostulacionRequestDTO en el backend
export interface NuevaPostulacion {
  idConvocatoria: number;
  idEstudiante: number;
  respuestaMotivacion: string;
  disponibilidadHorasSemana: number;
  experienciaPrevia: string;
  idHabilidades: number[];
  // Requisitos de texto de la convocatoria que el estudiante declara cumplir
  idRequisitosCumplidos: number[];
}

interface PostulacionApi {
  id: number;
  idConvocatoria: number;
  idEstudiante: number;
  tituloConvocatoria: string;
  nombreSemillero: string;
  nombreEstudiante: string;
  programaEstudiante: string | null;
  respuestaMotivacion: string;
  fechaPostulacion: string;
  fechaPreAprobacion: string | null;
  fechaDecisionEstudiante: string | null;
  fechaLimiteRespuesta: string | null;
  disponibilidadHorasSemana: number;
  estadoPostulacion: string;
  habilidades: string[];
  requisitos: RequisitoEvaluado[] | null;
  cumpleObligatorios: boolean;
}

// CandidatoPriorizadoDTO del backend
interface CandidatoPriorizadoApi {
  idPostulacion: number;
  idConvocatoria: number;
  posicion: number;
  cuposDisponibles: number;
  ofertasVigentes: number;
  puntaje: number;
  afinidadTematica: number;
  habilidades: number;
  disponibilidad: number;
  compromiso: number;
  observaciones: string[];
  cumpleObligatorios: boolean;
  requisitosObligatoriosIncumplidos: string[] | null;
}

function mapearPostulacion(postulacion: PostulacionApi): Postulacion {
  return {
    idPostulacion: postulacion.id,
    idConvocatoria: postulacion.idConvocatoria,
    tituloConvocatoria: postulacion.tituloConvocatoria,
    nombreSemillero: postulacion.nombreSemillero,
    idEstudiante: postulacion.idEstudiante,
    nombreEstudiante: postulacion.nombreEstudiante,
    programaEstudiante: postulacion.programaEstudiante ?? "",
    estado: normalizarEstadoPostulacion(postulacion.estadoPostulacion),
    fechaPostulacion: postulacion.fechaPostulacion,
    fechaPreAprobacion: postulacion.fechaPreAprobacion,
    fechaDecisionEstudiante: postulacion.fechaDecisionEstudiante,
    fechaLimiteRespuesta: postulacion.fechaLimiteRespuesta,
    disponibilidadHorasSemana: postulacion.disponibilidadHorasSemana,
    habilidades: postulacion.habilidades ?? [],
    cartaMotivacion: postulacion.respuestaMotivacion,
    requisitos: postulacion.requisitos ?? [],
    cumpleObligatorios: postulacion.cumpleObligatorios ?? true,
  };
}

function mapearPriorizacion(
  candidato: CandidatoPriorizadoApi,
): PriorizacionCandidato & { idPostulacion: number } {
  return {
    idPostulacion: candidato.idPostulacion,
    posicion: candidato.posicion,
    puntaje: candidato.puntaje,
    cuposDisponibles: candidato.cuposDisponibles,
    ofertasVigentes: candidato.ofertasVigentes,
    desglose: {
      afinidadTematica: candidato.afinidadTematica,
      habilidades: candidato.habilidades,
      disponibilidad: candidato.disponibilidad,
      compromiso: candidato.compromiso,
    },
    observaciones: candidato.observaciones ?? [],
    cumpleObligatorios: candidato.cumpleObligatorios ?? true,
    requisitosObligatoriosIncumplidos:
      candidato.requisitosObligatoriosIncumplidos ?? [],
  };
}

// Refleja 1:1 los estados de postulación de tbl_estados
function normalizarEstadoPostulacion(estado: string): EstadoPostulacion {
  const normalizado = estado
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase()
    .trim();

  switch (normalizado) {
    case "pre-aprobada":
      return "PRE_APROBADA";
    case "aceptada":
      return "ACEPTADA";
    case "rechazada":
      return "RECHAZADA";
    case "rechazada por estudiante":
      return "RECHAZADA_POR_ESTUDIANTE";
    case "expirada":
      return "EXPIRADA";
    case "cancelada en cascada":
      return "CANCELADA";
    default:
      return "PENDIENTE";
  }
}
