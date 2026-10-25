import { queryOptions } from "@tanstack/react-query";
import {
  analiticaService,
  catalogoService,
  convocatoriasService,
  estudiantesResumenService,
  matchingService,
  postulacionesService,
  profesoresService,
  semillerosService,
} from "./services";
import { apiRequest } from "./client";
import type { FiltroSemilleros, UsuarioSesion } from "@/types/api";

export const ROL_ADMINISTRADOR = "Administrador";

export const esAdministrador = (sesion?: UsuarioSesion | null) =>
  sesion?.rol === ROL_ADMINISTRADOR;

export const esEstudiante = (sesion?: UsuarioSesion | null) =>
  sesion != null && sesion.idEstudiante !== null;

export const esProfesor = (sesion?: UsuarioSesion | null) =>
  sesion != null && sesion.idProfesor !== null;

// Liderar es una relación con el semillero, no un rol: el estudiante sigue siendo Estudiante
export const esEstudianteLider = (sesion?: UsuarioSesion | null) =>
  esEstudiante(sesion) && (sesion?.idSemillerosLiderados.length ?? 0) > 0;

export type TipoPerfil = "ADMINISTRADOR" | "ESTUDIANTE" | "PROFESOR";

export const tipoPerfil = (
  sesion?: UsuarioSesion | null,
): TipoPerfil | null => {
  if (esAdministrador(sesion)) return "ADMINISTRADOR";
  if (esEstudiante(sesion)) return "ESTUDIANTE";
  if (esProfesor(sesion)) return "PROFESOR";
  return null;
};

export const usuarioSesionQuery = () =>
  queryOptions({
    queryKey: ["sesion", "usuario"],
    queryFn: () => apiRequest<UsuarioSesion>("/api/usuarios/me"),
  });

export const semillerosQuery = (filtros: FiltroSemilleros = {}) =>
  queryOptions({
    queryKey: ["semilleros", filtros],
    queryFn: () => semillerosService.listar(filtros),
  });

export const semilleroQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["semillero", idSemillero],
    queryFn: () => semillerosService.obtener(idSemillero),
  });

export const proyectosQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["proyectos", idSemillero],
    queryFn: () => semillerosService.proyectos(idSemillero),
  });

export const publicacionesQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["publicaciones", idSemillero],
    queryFn: () => semillerosService.publicaciones(idSemillero),
  });

export const convocatoriasAbiertasQuery = () =>
  queryOptions({
    queryKey: ["convocatorias", "abiertas"],
    queryFn: () => convocatoriasService.listarAbiertas(),
  });

export const convocatoriasSemilleroQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["convocatorias", idSemillero],
    queryFn: () => convocatoriasService.porSemillero(idSemillero),
  });

export const misPostulacionesQuery = (idEstudiante: number) =>
  queryOptions({
    queryKey: ["postulaciones", "estudiante", idEstudiante],
    queryFn: () => postulacionesService.misPostulaciones(idEstudiante),
  });

export const postulacionesRecibidasQuery = (idSemilleros: number[]) =>
  queryOptions({
    queryKey: ["postulaciones", "semilleros", idSemilleros],
    queryFn: async () =>
      (
        await Promise.all(
          idSemilleros.map((id) => postulacionesService.recibidas(id)),
        )
      ).flat(),
  });

export const recomendacionesQuery = (idEstudiante: number) =>
  queryOptions({
    queryKey: ["matching", idEstudiante],
    queryFn: () => matchingService.recomendaciones(idEstudiante),
  });

export const habilidadesQuery = () =>
  queryOptions({
    queryKey: ["catalogo", "habilidades"],
    queryFn: () => catalogoService.habilidades(),
  });

export const analiticaQuery = () =>
  queryOptions({
    queryKey: ["analitica", "institucional"],
    queryFn: () => analiticaService.institucional(),
  });

export const profesoresQuery = () =>
  queryOptions({
    queryKey: ["profesores"],
    queryFn: () => profesoresService.listar(),
  });

export const estudiantesQuery = () =>
  queryOptions({
    queryKey: ["estudiantes"],
    queryFn: () => estudiantesResumenService.listar(),
  });

export const catalogoSemilleroQuery = () =>
  queryOptions({
    queryKey: ["catalogo", "semillero"],
    queryFn: async () => {
      const [programas, facultades, lineas, areas] = await Promise.all([
        catalogoService.programas(),
        catalogoService.facultades(),
        catalogoService.lineasInvestigacion(),
        catalogoService.areasEspecialidad(),
      ]);
      return { programas, facultades, lineas, areas };
    },
  });
