/**
 * SERVICIOS HTTP - Mapeo directo con endpoints del backend Spring Boot
 */

import { apiRequest } from "./client";
import type {
  AnaliticaInstitucional,
  Convocatoria,
  Estudiante,
  EstadoPostulacion,
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
  listar: () => apiRequest<Estudiante[]>("/api/estudiantes"),

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
  listarAbiertas: async () => {
    const convocatorias = await apiRequest<ConvocatoriaApi[]>(
      "/api/convocatorias/activas",
    );
    return convocatorias.map(
      (convocatoria) =>
        ({
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
          habilidadesRequeridas: [],
        }) satisfies Convocatoria,
    );
  },

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
  misPostulaciones: async (idEstudiante: number) => {
    const postulaciones = await apiRequest<PostulacionApi[]>(
      `/api/postulaciones/estudiante/${idEstudiante}`,
    );
    return postulaciones.map(
      (postulacion) =>
        ({
          idPostulacion: postulacion.id,
          idConvocatoria: postulacion.idConvocatoria,
          tituloConvocatoria: postulacion.tituloConvocatoria,
          nombreSemillero: postulacion.nombreSemillero,
          idEstudiante: postulacion.idEstudiante,
          nombreEstudiante: postulacion.nombreEstudiante,
          programaEstudiante: "",
          estado: normalizarEstadoPostulacion(postulacion.estadoPostulacion),
          fechaPostulacion: postulacion.fechaPostulacion,
          fechaPreAprobacion: null,
          fechaDecisionEstudiante: postulacion.fechaDecisionEstudiante,
          habilidades: postulacion.habilidades ?? [],
          cartaMotivacion: postulacion.respuestaMotivacion,
        }) satisfies Postulacion,
    );
  },

  recibidas: (idSemillero: number) =>
    apiRequest<Postulacion[]>(`/api/postulaciones/semillero/${idSemillero}`),

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
    apiRequest<Postulacion>(
      `/api/postulaciones/${idPostulacion}/confirmar-aceptacion`,
      {
        method: "PATCH",
      },
    ),

  rechazarOferta: (idPostulacion: number) =>
    apiRequest<Postulacion>(
      `/api/postulaciones/${idPostulacion}/rechazar-oferta`,
      {
        method: "PATCH",
      },
    ),
};

// ============================================
// MOTOR DE RECOMENDACIÓN (Servicio 1)
// ============================================

export const matchingService = {
  recomendaciones: async (idEstudiante: number) => {
    const recomendaciones = await apiRequest<RecomendacionApi[]>(
      `/api/recomendaciones/estudiante/${idEstudiante}`,
    );
    return recomendaciones.map(
      (recomendacion) =>
        ({
          idSemillero: recomendacion.idSemillero,
          nombre: recomendacion.nombreSemillero,
          porcentajeMatch: recomendacion.porcentajeMatch,
          factoresCoincidencia: [
            ["Habilidades", recomendacion.factor1Habilidades],
            ["Especialidad", recomendacion.factor2AreaEspecialidad],
            ["Cupos", recomendacion.factor3DisponibilidadConvocatorias],
            ["Temática", recomendacion.factor4ProximidadTematica],
          ]
            .filter(([, value]) => typeof value === "number" && value > 0)
            .map(([label, value]) => `${label}: ${Math.round(Number(value))}%`),
          desglose: {
            habilidades: recomendacion.factor1Habilidades ?? 0,
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

// ============================================
// ANALÍTICA INSTITUCIONAL (Servicio 3)
// ============================================

export const analiticaService = {
  institucional: async () => {
    const [dashboard, semilleros, convocatorias] = await Promise.all([
      apiRequest<AnaliticaDashboardApi>("/api/analitica/dashboard"),
      semillerosService.listar(),
      apiRequest<ConvocatoriaApi[]>("/api/convocatorias/activas"),
    ]);

    const demandaPorSemillero = dashboard.tasasDemanda.map((demanda) => ({
      idSemillero: demanda.idSemillero,
      nombre: demanda.nombreSemillero,
      cuposOfertados: demanda.cuposOfertados,
      postulacionesRecibidas: demanda.postulacionesRecibidas,
      tasaDemanda: demanda.tasaDemanda,
    }));
    const promedioDemanda = demandaPorSemillero.length
      ? demandaPorSemillero.reduce(
          (total, demanda) => total + demanda.tasaDemanda,
          0,
        ) / demandaPorSemillero.length
      : 0;
    return {
      kpis: {
        totalSemilleros: semilleros.length,
        semillerosActivos: semilleros.filter((semillero) =>
          semillero.estado.toLowerCase().includes("activ"),
        ).length,
        convocatoriasAbiertas: convocatorias.length,
        postulacionesActivas: dashboard.postulacionesActivas,
        tasaDemandaPromedio: promedioDemanda,
        tiempoRespuestaEstudianteHoras: dashboard.tiempoRespuesta.promedioHoras,
        tasaRechazoOfertas:
          dashboard.tasaRetencion.porcentajeRechazoYExpiracion,
        indiceConversionProyectoPublicacion:
          dashboard.conversionProyectos.promedioPublicacionesPorProyecto,
        participacionEstudiantilAutorias:
          dashboard.participacionEstudiantil.porcentajeParticipacionEstudiantil,
        indiceInterdisciplinariedad:
          dashboard.interdisciplinariedad.porcentajeInterdisciplinariedad,
      },
      demandaPorSemillero,
      reconocimientos: Object.entries(
        dashboard.impactoReconocimientos.distribucionPorEntidad ?? {},
      ).map(([nombre, total]) => ({ nombre, total })),
      habilidadesEmergentes: dashboard.habilidadesEmergentes.map(
        (habilidad) => ({
          nombre: habilidad.habilidad,
          totalAceptadas: habilidad.frecuencia,
        }),
      ),
    } satisfies AnaliticaInstitucional;
  },
};

// ============================================
// CATÁLOGOS (Datos estáticos)
// ============================================

export const catalogoService = {
  programas: () => apiRequest("/api/catalogos/programas-academicos"),
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

interface PostulacionApi {
  id: number;
  idConvocatoria: number;
  idEstudiante: number;
  tituloConvocatoria: string;
  nombreSemillero: string;
  nombreEstudiante: string;
  respuestaMotivacion: string;
  fechaPostulacion: string;
  fechaDecisionEstudiante: string | null;
  estadoPostulacion: string;
  habilidades: string[];
}

interface RecomendacionApi {
  idSemillero: number;
  nombreSemillero: string;
  nombrePrograma: string;
  porcentajeMatch: number;
  factor1Habilidades: number;
  factor2AreaEspecialidad: number;
  factor3DisponibilidadConvocatorias: number;
  factor4ProximidadTematica: number;
}

interface AnaliticaDashboardApi {
  postulacionesActivas: number;
  tasasDemanda: {
    idSemillero: number;
    nombreSemillero: string;
    cuposOfertados: number;
    postulacionesRecibidas: number;
    tasaDemanda: number;
  }[];
  tiempoRespuesta: { promedioHoras: number };
  tasaRetencion: { porcentajeRechazoYExpiracion: number };
  conversionProyectos: { promedioPublicacionesPorProyecto: number };
  participacionEstudiantil: { porcentajeParticipacionEstudiantil: number };
  impactoReconocimientos: { distribucionPorEntidad: Record<string, number> };
  habilidadesEmergentes: { habilidad: string; frecuencia: number }[];
  interdisciplinariedad: { porcentajeInterdisciplinariedad: number };
}

function normalizarEstadoConvocatoria(estado: string): Convocatoria["estado"] {
  const normalizado = estado.toLocaleLowerCase();
  if (normalizado.includes("abierta")) return "ABIERTA";
  if (normalizado.includes("borrador")) return "BORRADOR";
  return "CERRADA";
}

function normalizarEstadoPostulacion(estado: string): EstadoPostulacion {
  const normalizado = estado
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleUpperCase();

  if (
    normalizado.includes("PRE-APROBADA") ||
    normalizado.includes("PRE APROBADA")
  ) {
    return "PRE_APROBADA";
  }
  if (normalizado.includes("ACEPTADA")) return "ACEPTADA_POR_ESTUDIANTE";
  if (normalizado.includes("RECHAZADA") && normalizado.includes("ESTUDIANTE")) {
    return "RECHAZADA_POR_ESTUDIANTE";
  }
  if (normalizado.includes("RECHAZADA")) return "RECHAZADA_POR_LIDER";
  if (normalizado.includes("EXPIRADA")) return "EXPIRADA";
  if (normalizado.includes("CANCELADA")) return "CANCELADA_EN_CASCADA";
  if (normalizado.includes("REVISION")) return "EN_REVISION";
  return "ENVIADA";
}
