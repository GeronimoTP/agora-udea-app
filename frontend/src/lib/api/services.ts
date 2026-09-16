import { apiRequest, apiRequestWithFallback } from "./client";
import {
  analiticaDemo,
  convocatoriasDemo,
  matchesDemo,
  postulacionesDemo,
  proyectosDemo,
  publicacionesDemo,
  semillerosDemo,
} from "./mock-data";
import type {
  AnaliticaInstitucional,
  Convocatoria,
  FiltroSemilleros,
  Postulacion,
  Proyecto,
  Publicacion,
  ResultadoMatch,
  Semillero,
} from "@/types/api";

/** Endpoints del backend Spring Boot agrupados por módulo. */

export const semillerosService = {
  listar: (filtros: FiltroSemilleros = {}) =>
    apiRequestWithFallback<Semillero[]>("/semilleros", semillerosDemo, {
      query: {
        busqueda: filtros.busqueda,
        facultad: filtros.facultad,
        convocatoriaAbierta: filtros.soloConvocatoriaAbierta,
      },
    }),
  obtener: (idSemillero: number) =>
    apiRequestWithFallback<Semillero>(
      `/semilleros/${idSemillero}`,
      semillerosDemo.find((s) => s.idSemillero === idSemillero) ?? semillerosDemo[0]!,
    ),
  proyectos: (idSemillero: number) =>
    apiRequestWithFallback<Proyecto[]>(
      `/semilleros/${idSemillero}/proyectos`,
      proyectosDemo.map((p) => ({ ...p, idSemillero })),
    ),
  publicaciones: (idSemillero: number) =>
    apiRequestWithFallback<Publicacion[]>(
      `/semilleros/${idSemillero}/publicaciones`,
      publicacionesDemo.map((p) => ({ ...p, idSemillero })),
    ),
};

export const convocatoriasService = {
  listarAbiertas: () =>
    apiRequestWithFallback<Convocatoria[]>("/convocatorias", convocatoriasDemo, {
      query: { estado: "ABIERTA" },
    }),
  porSemillero: (idSemillero: number) =>
    apiRequestWithFallback<Convocatoria[]>(
      `/semilleros/${idSemillero}/convocatorias`,
      convocatoriasDemo.filter((c) => c.idSemillero === idSemillero),
    ),
};

export const postulacionesService = {
  misPostulaciones: (idEstudiante: number) =>
    apiRequestWithFallback<Postulacion[]>(
      `/estudiantes/${idEstudiante}/postulaciones`,
      postulacionesDemo,
    ),
  recibidas: (idSemillero: number) =>
    apiRequestWithFallback<Postulacion[]>(
      `/semilleros/${idSemillero}/postulaciones`,
      postulacionesDemo,
    ),
  crear: (payload: { idConvocatoria: number; cartaMotivacion: string; habilidades: number[] }) =>
    apiRequest<Postulacion>("/postulaciones", { method: "POST", body: payload }),
  /** Servicio 2: pre-aprobación del líder. */
  preAprobar: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/postulaciones/${idPostulacion}/pre-aprobar`, { method: "PATCH" }),
  rechazar: (idPostulacion: number, motivo?: string) =>
    apiRequest<Postulacion>(`/postulaciones/${idPostulacion}/rechazar`, {
      method: "PATCH",
      body: { motivo },
    }),
  /** Servicio 2: decisión final del estudiante, dispara resolución en cascada. */
  aceptarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/postulaciones/${idPostulacion}/aceptar`, { method: "PATCH" }),
  rechazarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/postulaciones/${idPostulacion}/declinar`, { method: "PATCH" }),
};

export const matchingService = {
  /** Servicio 1: ranking de afinidad ponderado. */
  recomendaciones: (idEstudiante: number) =>
    apiRequestWithFallback<ResultadoMatch[]>(
      `/matching/estudiantes/${idEstudiante}/recomendaciones`,
      matchesDemo,
    ),
};

export const analiticaService = {
  /** Servicio 3: KPIs institucionales. */
  institucional: () =>
    apiRequestWithFallback<AnaliticaInstitucional>("/analitica/institucional", analiticaDemo),
};
