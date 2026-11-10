/**
 * SEMILLEROS para las vistas públicas (listado, detalle, nuevo semillero). Mapea los DTOs del
 * backend (tipos *Api, privados de este módulo) a los tipos de vista de @/types/api.
 */

import { apiRequest } from "./client";
import type {
  FiltroSemilleros,
  IntegranteSemillero,
  Proyecto,
  Publicacion,
  Semillero,
  TutorSemillero,
} from "@/types/api";

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

  // Solo integrantes activos: los retirados se gestionan desde /gestion. El backend
  // solo responde a líderes, tutores e integrantes activos del semillero
  integrantes: async (idSemillero: number) =>
    (
      await apiRequest<IntegranteApi[]>(
        `/api/semilleros/${idSemillero}/estudiantes`,
      )
    )
      .filter((integrante) => integrante.fechaSalida === null)
      .map((integrante): IntegranteSemillero => ({
        idEstudiante: integrante.idEstudiante,
        nombre: integrante.nombreEstudiante,
        email: integrante.email,
        programa: integrante.nombrePrograma ?? "",
        semestre: integrante.semestre,
      })),

  tutores: async (idSemillero: number) =>
    (
      await apiRequest<TutorApi[]>(`/api/semilleros/${idSemillero}/profesores`)
    ).map((tutor): TutorSemillero => ({
      idProfesor: tutor.idProfesor,
      nombre: tutor.nombreProfesor,
      email: tutor.email,
    })),

  // El backend asigna el estudiante líder y vincula los profesores tutores
  crear: async (payload: NuevoSemillero) =>
    mapearSemillero(
      await apiRequest<SemilleroApi>("/api/semilleros", {
        method: "POST",
        body: payload,
      }),
    ),
};

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

interface SemilleroApi {
  id: number;
  idEstudianteLider: number;
  nombreEstudianteLider: string;
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

// EstudianteSemilleroResponseDTO
interface IntegranteApi {
  idEstudiante: number;
  nombreEstudiante: string;
  email: string;
  nombrePrograma: string | null;
  semestre: number;
  fechaSalida: string | null;
}

// ProfesorSemilleroResponseDTO
interface TutorApi {
  idProfesor: number;
  nombreProfesor: string;
  email: string;
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
    nombreLider: semillero.nombreEstudianteLider,
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
