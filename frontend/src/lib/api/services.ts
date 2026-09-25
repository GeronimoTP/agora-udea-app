/**
 * SERVICIOS HTTP - Mapeo directo con endpoints del backend Spring Boot
 */

import { apiRequest } from "./client";
import type {
  AnaliticaInstitucional,
  Convocatoria,
  Estudiante,
  FiltroSemilleros,
  Postulacion,
  Proyecto,
  Publicacion,
  ResultadoMatch,
  Semillero,
} from "@/types/api";

// ============================================
// ESTUDIANTES
// ============================================

export const estudianteService = {
  listar: () =>
    apiRequest<Estudiante[]>("/api/estudiantes"),

  obtener: (idEstudiante: number) =>
    apiRequest<Estudiante>(`/api/estudiantes/${idEstudiante}`),

  obtenerPorUsuario: (idUsuario: number) =>
    apiRequest<Estudiante>(`/api/estudiantes/usuario/${idUsuario}`),

  crear: (payload: Partial<Estudiante>) =>
    apiRequest<Estudiante>("/api/estudiantes", {
      method: "POST",
      body: payload,
    }),

  actualizar: (idEstudiante: number, payload: Partial<Estudiante>) =>
    apiRequest<Estudiante>(`/api/estudiantes/${idEstudiante}`, {
      method: "PUT",
      body: payload,
    }),
};

// ============================================
// SEMILLEROS
// ============================================

export const semillerosService = {
  listar: (filtros: FiltroSemilleros = {}) =>
    apiRequest<Semillero[]>("/api/semilleros", {
      query: {
        busqueda: filtros.busqueda,
        facultad: filtros.facultad,
        convocatoriaAbierta: filtros.soloConvocatoriaAbierta,
      },
    }),

  obtener: (idSemillero: number) =>
    apiRequest<Semillero>(`/api/semilleros/${idSemillero}`),

  proyectos: (idSemillero: number) =>
    apiRequest<Proyecto[]>(`/api/semilleros/${idSemillero}/proyectos`),

  publicaciones: (idSemillero: number) =>
    apiRequest<Publicacion[]>(`/api/semilleros/${idSemillero}/publicaciones`),

  crear: (payload: Partial<Semillero>) =>
    apiRequest<Semillero>("/api/semilleros", {
      method: "POST",
      body: payload,
    }),

  actualizar: (idSemillero: number, payload: Partial<Semillero>) =>
    apiRequest<Semillero>(`/api/semilleros/${idSemillero}`, {
      method: "PUT",
      body: payload,
    }),
};

// ============================================
// CONVOCATORIAS
// ============================================

export const convocatoriasService = {
  listarAbiertas: () =>
    apiRequest<Convocatoria[]>("/api/convocatorias", {
      query: { estado: "ABIERTA" },
    }),

  obtener: (idConvocatoria: number) =>
    apiRequest<Convocatoria>(`/api/convocatorias/${idConvocatoria}`),

  porSemillero: (idSemillero: number) =>
    apiRequest<Convocatoria[]>("/api/convocatorias", {
      query: { idSemillero },
    }),

  crear: (payload: Partial<Convocatoria>) =>
    apiRequest<Convocatoria>("/api/convocatorias", {
      method: "POST",
      body: payload,
    }),

  actualizar: (idConvocatoria: number, payload: Partial<Convocatoria>) =>
    apiRequest<Convocatoria>(`/api/convocatorias/${idConvocatoria}`, {
      method: "PUT",
      body: payload,
    }),
};

// ============================================
// POSTULACIONES - Flujo de doble vía
// ============================================

export const postulacionesService = {
  misPostulaciones: (idEstudiante: number) =>
    apiRequest<Postulacion[]>("/api/postulaciones", { 
      query: { idEstudiante } 
    }),

  recibidas: (idSemillero: number) =>
    apiRequest<Postulacion[]>("/api/postulaciones", { 
      query: { idSemillero } 
    }),

  obtener: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}`),

  crear: (payload: Partial<Postulacion>) =>
    apiRequest<Postulacion>("/api/postulaciones", {
      method: "POST",
      body: payload,
    }),

  preAprobar: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/pre-aprobar`, {
      method: "PATCH",
    }),

  rechazar: (idPostulacion: number, motivo?: string) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/rechazar`, {
      method: "PATCH",
      body: { motivo },
    }),

  aceptarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/confirmar-aceptacion`, {
      method: "PATCH",
    }),

  rechazarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/rechazar-oferta`, {
      method: "PATCH",
    }),
};

// ============================================
// MOTOR DE RECOMENDACIÓN (Servicio 1)
// ============================================

export const matchingService = {
  recomendaciones: (idEstudiante: number) =>
    apiRequest<ResultadoMatch[]>(`/api/recomendaciones/${idEstudiante}`),
};

// ============================================
// ANALÍTICA INSTITUCIONAL (Servicio 3)
// ============================================

export const analiticaService = {
  institucional: () =>
    apiRequest<AnaliticaInstitucional>("/api/analitica/institucional"),
};

// ============================================
// CATÁLOGOS (Datos estáticos)
// ============================================

export const catalogoService = {
  programas: () => apiRequest("/api/catalogos/programas"),
  facultades: () => apiRequest("/api/catalogos/facultades"),
  areasEspecialidad: () => apiRequest("/api/areas-especialidad"),
  lineasInvestigacion: () => apiRequest("/api/lineas-investigacion"),
  habilidades: () => apiRequest("/api/habilidades"),
  estados: () => apiRequest("/api/estados"),
};

export default {
  estudiantes: estudianteService,
  semilleros: semillerosService,
  convocatorias: convocatoriasService,
  postulaciones: postulacionesService,
  matching: matchingService,
  analitica: analiticaService,
  catalogo: catalogoService,
};