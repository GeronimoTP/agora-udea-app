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
  PriorizacionCandidato,
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
  // El backend no filtra: la página de semilleros filtra en el cliente
  listar: async (_filtros: FiltroSemilleros = {}) =>
    (await apiRequest<SemilleroApi[]>("/api/semilleros")).map(mapearSemillero),

  obtener: async (idSemillero: number) =>
    mapearSemillero(
      await apiRequest<SemilleroApi>(`/api/semilleros/${idSemillero}`),
    ),

  proyectos: async (idSemillero: number) =>
    (
      await apiRequest<ProyectoApi[]>(`/api/proyectos/semillero/${idSemillero}`)
    ).map(mapearProyecto),

  publicaciones: async (idSemillero: number) =>
    (
      await apiRequest<PublicacionApi[]>(
        `/api/publicaciones/semillero/${idSemillero}`,
      )
    ).map((publicacion) => mapearPublicacion(publicacion, idSemillero)),

  // El backend asigna el estudiante líder y vincula los profesores tutores
  crear: async (payload: NuevoSemillero) =>
    mapearSemillero(
      await apiRequest<SemilleroApi>("/api/semilleros", {
        method: "POST",
        body: payload,
      }),
    ),
};

// ============================================
// PROFESORES
// ============================================

export const profesoresService = {
  listar: () => apiRequest<Profesor[]>("/api/profesores"),
};

// Candidatos a estudiante líder cuando un profesor registra un semillero
export const estudiantesResumenService = {
  listar: () => apiRequest<EstudianteResumen[]>("/api/estudiantes"),
};

// ============================================
// CONVOCATORIAS
// ============================================

export const convocatoriasService = {
  listarAbiertas: async () => {
    const convocatorias = await apiRequest<ConvocatoriaApi[]>(
      "/api/convocatorias/activas",
    );
    return convocatorias.map(mapearConvocatoria);
  },

  obtener: async (idConvocatoria: number) =>
    mapearConvocatoria(
      await apiRequest<ConvocatoriaApi>(`/api/convocatorias/${idConvocatoria}`),
    ),

  porSemillero: async (idSemillero: number) =>
    (
      await apiRequest<ConvocatoriaApi[]>(
        `/api/convocatorias/semillero/${idSemillero}`,
      )
    ).map(mapearConvocatoria),

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
  misPostulaciones: async (idEstudiante: number) =>
    (
      await apiRequest<PostulacionApi[]>(
        `/api/postulaciones/estudiante/${idEstudiante}`,
      )
    ).map(mapearPostulacion),

  recibidas: async (idSemillero: number) =>
    (
      await apiRequest<PostulacionApi[]>(
        `/api/postulaciones/semillero/${idSemillero}`,
      )
    ).map(mapearPostulacion),

  obtener: async (idPostulacion: number) =>
    mapearPostulacion(
      await apiRequest<PostulacionApi>(`/api/postulaciones/${idPostulacion}`),
    ),

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

// ============================================
// PERFIL (estudiante y profesor)
// ============================================

export const perfilService = {
  estudiante: (idEstudiante: number) =>
    apiRequest<EstudianteResumen>(`/api/estudiantes/${idEstudiante}`),

  // PUT reemplaza programa, semestre y líneas en una sola transacción
  actualizarEstudiante: (idEstudiante: number, payload: PerfilEstudiante) =>
    apiRequest<EstudianteResumen>(`/api/estudiantes/${idEstudiante}`, {
      method: "PUT",
      body: payload,
    }),

  profesor: (idProfesor: number) =>
    apiRequest<Profesor>(`/api/profesores/${idProfesor}`),

  actualizarProfesor: (idProfesor: number, payload: PerfilProfesor) =>
    apiRequest<Profesor>(`/api/profesores/${idProfesor}`, {
      method: "PUT",
      body: payload,
    }),
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
  programas: () =>
    apiRequest<ProgramaCatalogo[]>("/api/catalogos/programas-academicos"),
  facultades: () => apiRequest<ItemCatalogo[]>("/api/catalogos/facultades"),
  areasEspecialidad: () => apiRequest<ItemCatalogo[]>("/api/areas-especialidad"),
  lineasInvestigacion: () =>
    apiRequest<ItemCatalogo[]>("/api/lineas-investigacion"),
  habilidades: () => apiRequest<Habilidad[]>("/api/habilidades"),
  estados: () => apiRequest("/api/estados"),
};

// Cuerpo esperado por PostulacionRequestDTO en el backend
export interface NuevaPostulacion {
  idConvocatoria: number;
  idEstudiante: number;
  respuestaMotivacion: string;
  disponibilidadHorasSemana: number;
  experienciaPrevia: string;
  idHabilidades: number[];
}

export interface Habilidad {
  id: number;
  nombre: string;
}

export interface ItemCatalogo {
  id: number;
  nombre: string;
}

export interface ProgramaCatalogo extends ItemCatalogo {
  nombreFacultad: string | null;
}

// ProfesorResponseDTO del backend
export interface Profesor {
  id: number;
  nombreCompleto: string;
  email: string;
  idPrograma: number | null;
  nombrePrograma: string | null;
  idFacultad: number | null;
  nombreFacultad: string | null;
  areasEspecialidad: string[];
}

// EstudianteResponseDTO del backend
export interface EstudianteResumen {
  id: number;
  nombreCompleto: string;
  email: string;
  nombrePrograma: string | null;
  semestre: number;
  lineasInvestigacion: string[];
  habilidades: string[];
}

// Cuerpos de EstudianteRequestDTO y ProfesorRequestDTO (idUsuario es obligatorio y no cambia)
export interface PerfilEstudiante {
  idUsuario: number;
  idPrograma: number;
  semestre: number;
  idLineasInvestigacion: number[];
  idHabilidades: number[];
}

export interface PerfilProfesor {
  idUsuario: number;
  idPrograma: number;
  idAreasEspecialidad: number[];
}

// Cuerpo esperado por SemilleroRequestDTO en el backend
export interface NuevoSemillero {
  codigoIdentificador: string;
  nombre: string;
  descripcion: string;
  idEstudianteLider: number;
  idPrograma: number;
  idLineasInvestigacion: number[];
  idProfesores: number[];
}

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
  programaEstudiante: string | null;
  respuestaMotivacion: string;
  fechaPostulacion: string;
  fechaPreAprobacion: string | null;
  fechaDecisionEstudiante: string | null;
  fechaLimiteRespuesta: string | null;
  disponibilidadHorasSemana: number;
  estadoPostulacion: string;
  habilidades: string[];
}

// PriorizacionCandidatosService.CandidatoPriorizadoDTO
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
}

interface SemilleroApi {
  id: number;
  idEstudianteLider: number;
  nombre: string;
  descripcion: string;
  estadoActual: string | null;
  lineasInvestigacion: string[];
  nombreFacultad: string | null;
  totalProyectos: number;
  totalPublicaciones: number;
  totalIntegrantes: number;
  convocatoriaAbierta: boolean;
}

interface ProyectoApi {
  id: number;
  idSemillero: number;
  titulo: string;
  estadoActual: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  presupuestoAsignado: number | null;
  totalIntegrantes: number;
}

interface PublicacionApi {
  id: number;
  titulo: string;
  urlDocumento: string | null;
  fechaPublicacion: string | null;
  autoresDetalle: {
    nombre: string;
    rolAutoria: string;
    esEstudiante: boolean;
  }[];
}

function mapearSemillero(semillero: SemilleroApi): Semillero {
  return {
    idSemillero: semillero.id,
    idEstudianteLider: semillero.idEstudianteLider,
    nombre: semillero.nombre,
    descripcion: semillero.descripcion,
    lineasInvestigacion: semillero.lineasInvestigacion ?? [],
    estado: normalizarEstadoSemillero(semillero.estadoActual),
    facultad: semillero.nombreFacultad ?? "Sin facultad",
    lideres: [],
    totalProyectos: semillero.totalProyectos ?? 0,
    totalPublicaciones: semillero.totalPublicaciones ?? 0,
    totalIntegrantes: semillero.totalIntegrantes ?? 0,
    convocatoriaAbierta: semillero.convocatoriaAbierta ?? false,
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
    // El modelo de convocatoria no registra habilidades requeridas
    habilidadesRequeridas: [],
  };
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
  };
}

function mapearProyecto(proyecto: ProyectoApi): Proyecto {
  return {
    idProyecto: proyecto.id,
    idSemillero: proyecto.idSemillero,
    titulo: proyecto.titulo,
    estado: proyecto.estadoActual ?? "",
    fechaInicio: proyecto.fechaInicio,
    fechaFin: proyecto.fechaFin,
    presupuestoAsignado: proyecto.presupuestoAsignado ?? 0,
    integrantes: proyecto.totalIntegrantes ?? 0,
  };
}

function mapearPublicacion(
  publicacion: PublicacionApi,
  idSemillero: number,
): Publicacion {
  return {
    idPublicacion: publicacion.id,
    idSemillero,
    titulo: publicacion.titulo,
    tipo: "Publicación",
    anio: publicacion.fechaPublicacion
      ? new Date(publicacion.fechaPublicacion).getFullYear()
      : 0,
    doi: null,
    enlace: publicacion.urlDocumento,
    autores: publicacion.autoresDetalle ?? [],
  };
}

function normalizarEstadoSemillero(estado: string | null): Semillero["estado"] {
  const normalizado = (estado ?? "").toLocaleLowerCase();
  if (normalizado.includes("inactivo")) return "INACTIVO";
  if (normalizado.includes("activo")) return "ACTIVO";
  if (normalizado.includes("pausa")) return "EN_PAUSA";
  return "PENDIENTE_APROBACION";
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
