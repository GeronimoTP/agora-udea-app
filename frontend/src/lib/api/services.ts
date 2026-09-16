/**
 * SERVICIOS HTTP - Mapeo directo con endpoints del backend Spring Boot
 * 
 * Estructura:
 * - Cada servicio agrupa operaciones relacionadas (semilleros, convocatorias, etc.)
 * - Todos usan la ruta con "/api" incluido (ej. "/api/semilleros")
 * - Los métodos incluyen validaciones básicas y manejo de errores
 * 
 * URLs del backend (Spring Boot):
 * - http://localhost:8080/api/semilleros
 * - http://localhost:8080/api/estudiantes
 * - http://localhost:8080/api/postulaciones
 * - etc.
 */

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
  /**
   * Obtiene la lista de todos los estudiantes.
   * GET /api/estudiantes
   */
  listar: () =>
    apiRequestWithFallback<Estudiante[]>(
      "/api/estudiantes",
      []
    ),

  /**
   * Obtiene un estudiante por su ID.
   * GET /api/estudiantes/:id
   */
  obtener: (idEstudiante: number) =>
    apiRequestWithFallback<Estudiante>(
      `/api/estudiantes/${idEstudiante}`,
      {} as Estudiante
    ),

  /**
   * Obtiene el estudiante del usuario autenticado.
   * GET /api/estudiantes/usuario/:idUsuario
   */
  obtenerPorUsuario: (idUsuario: number) =>
    apiRequest<Estudiante>(`/api/estudiantes/usuario/${idUsuario}`),

  /**
   * Crea un nuevo estudiante.
   * POST /api/estudiantes
   */
  crear: (payload: Partial<Estudiante>) =>
    apiRequest<Estudiante>("/api/estudiantes", {
      method: "POST",
      body: payload,
    }),

  /**
   * Actualiza un estudiante existente.
   * PUT /api/estudiantes/:id
   */
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
  /**
   * Lista semilleros con filtros opcionales.
   * GET /api/semilleros
   */
  listar: (filtros: FiltroSemilleros = {}) =>
    apiRequestWithFallback<Semillero[]>("/api/semilleros", semillerosDemo, {
      query: {
        busqueda: filtros.busqueda,
        facultad: filtros.facultad,
        convocatoriaAbierta: filtros.soloConvocatoriaAbierta,
      },
    }),

  /**
   * Obtiene un semillero por su ID.
   * GET /api/semilleros/:id
   */
  obtener: (idSemillero: number) =>
    apiRequestWithFallback<Semillero>(
      `/api/semilleros/${idSemillero}`,
      semillerosDemo.find((s) => s.idSemillero === idSemillero) ?? semillerosDemo[0]!
    ),

  /**
   * Obtiene proyectos de un semillero.
   * GET /api/semilleros/:id/proyectos
   */
  proyectos: (idSemillero: number) =>
    apiRequestWithFallback<Proyecto[]>(
      `/api/semilleros/${idSemillero}/proyectos`,
      proyectosDemo.map((p) => ({ ...p, idSemillero }))
    ),

  /**
   * Obtiene publicaciones de un semillero.
   * GET /api/semilleros/:id/publicaciones
   */
  publicaciones: (idSemillero: number) =>
    apiRequestWithFallback<Publicacion[]>(
      `/api/semilleros/${idSemillero}/publicaciones`,
      publicacionesDemo.map((p) => ({ ...p, idSemillero }))
    ),

  /**
   * Crea un nuevo semillero.
   * POST /api/semilleros
   */
  crear: (payload: Partial<Semillero>) =>
    apiRequest<Semillero>("/api/semilleros", {
      method: "POST",
      body: payload,
    }),

  /**
   * Actualiza un semillero existente.
   * PUT /api/semilleros/:id
   */
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
  /**
   * Lista convocatorias abiertas.
   * GET /api/convocatorias?estado=ABIERTA
   */
  listarAbiertas: () =>
    apiRequestWithFallback<Convocatoria[]>("/api/convocatorias", convocatoriasDemo, {
      query: { estado: "ABIERTA" },
    }),

  /**
   * Obtiene una convocatoria por su ID.
   * GET /api/convocatorias/:id
   */
  obtener: (idConvocatoria: number) =>
    apiRequest<Convocatoria>(`/api/convocatorias/${idConvocatoria}`),

  /**
   * Lista convocatorias de un semillero específico.
   * GET /api/convocatorias?idSemillero=:id
   */
  porSemillero: (idSemillero: number) =>
    apiRequestWithFallback<Convocatoria[]>(
      "/api/convocatorias",
      convocatoriasDemo.filter((c) => c.idSemillero === idSemillero),
      {
        query: { idSemillero },
      }
    ),

  /**
   * Crea una nueva convocatoria.
   * POST /api/convocatorias
   */
  crear: (payload: Partial<Convocatoria>) =>
    apiRequest<Convocatoria>("/api/convocatorias", {
      method: "POST",
      body: payload,
    }),

  /**
   * Actualiza una convocatoria.
   * PUT /api/convocatorias/:id
   */
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
  /**
   * Obtiene postulaciones de un estudiante.
   * GET /api/postulaciones?idEstudiante=:id
   */
  misPostulaciones: (idEstudiante: number) =>
    apiRequestWithFallback<Postulacion[]>(
      "/api/postulaciones",
      postulacionesDemo,
      { query: { idEstudiante } }
    ),

  /**
   * Obtiene postulaciones recibidas en un semillero (para el líder).
   * GET /api/postulaciones?idSemillero=:id
   */
  recibidas: (idSemillero: number) =>
    apiRequestWithFallback<Postulacion[]>(
      "/api/postulaciones",
      postulacionesDemo,
      { query: { idSemillero } }
    ),

  /**
   * Obtiene una postulación específica.
   * GET /api/postulaciones/:id
   */
  obtener: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}`),

  /**
   * Crea una nueva postulación (acción del estudiante).
   * POST /api/postulaciones
   */
  crear: (payload: Partial<Postulacion>) =>
    apiRequest<Postulacion>("/api/postulaciones", {
      method: "POST",
      body: payload,
    }),

  /**
   * Pre-aprueba una postulación (acción del líder).
   * Paso 1 del flujo de doble vía.
   * PATCH /api/postulaciones/:id/pre-aprobar
   */
  preAprobar: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/pre-aprobar`, {
      method: "PATCH",
    }),

  /**
   * Rechaza una postulación (acción del líder).
   * PATCH /api/postulaciones/:id/rechazar
   */
  rechazar: (idPostulacion: number, motivo?: string) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/rechazar`, {
      method: "PATCH",
      body: { motivo },
    }),

  /**
   * Confirma aceptación de oferta (acción del estudiante).
   * Paso 2 del flujo de doble vía. Dispara resolución en cascada.
   * PATCH /api/postulaciones/:id/confirmar-aceptacion
   */
  aceptarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/confirmar-aceptacion`, {
      method: "PATCH",
    }),

  /**
   * Rechaza oferta (acción del estudiante).
   * PATCH /api/postulaciones/:id/rechazar-oferta
   */
  rechazarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(`/api/postulaciones/${idPostulacion}/rechazar-oferta`, {
      method: "PATCH",
    }),
};

// ============================================
// MOTOR DE RECOMENDACIÓN (Servicio 1)
// ============================================

export const matchingService = {
  /**
   * Obtiene recomendaciones de semilleros para un estudiante.
   * Ranking de afinidad ponderada (0-100).
   * GET /api/recomendaciones/:idEstudiante
   */
  recomendaciones: (idEstudiante: number) =>
    apiRequestWithFallback<ResultadoMatch[]>(
      `/api/recomendaciones/${idEstudiante}`,
      matchesDemo
    ),
};

// ============================================
// ANALÍTICA INSTITUCIONAL (Servicio 3)
// ============================================

export const analiticaService = {
  /**
   * Obtiene KPIs y métricas institucionales.
   * GET /api/analitica/institucional
   */
  institucional: () =>
    apiRequestWithFallback<AnaliticaInstitucional>(
      "/api/analitica/institucional",
      analiticaDemo
    ),
};

// ============================================
// CATÁLOGOS (Datos estáticos)
// ============================================

export const catalogoService = {
  /**
   * Obtiene lista de programas académicos.
   * GET /api/catalogos/programas
   */
  programas: () =>
    apiRequest("/api/catalogos/programas"),

  /**
   * Obtiene lista de facultades.
   * GET /api/catalogos/facultades
   */
  facultades: () =>
    apiRequest("/api/catalogos/facultades"),

  /**
   * Obtiene lista de áreas de especialidad.
   * GET /api/areas-especialidad
   */
  areasEspecialidad: () =>
    apiRequest("/api/areas-especialidad"),

  /**
   * Obtiene lista de líneas de investigación.
   * GET /api/lineas-investigacion
   */
  lineasInvestigacion: () =>
    apiRequest("/api/lineas-investigacion"),

  /**
   * Obtiene lista de habilidades.
   * GET /api/habilidades
   */
  habilidades: () =>
    apiRequest("/api/habilidades"),

  /**
   * Obtiene lista de estados.
   * GET /api/estados
   */
  estados: () =>
    apiRequest("/api/estados"),
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