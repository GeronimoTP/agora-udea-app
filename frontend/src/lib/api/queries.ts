import { queryOptions } from "@tanstack/react-query";
import {
  analiticaService,
  convocatoriasService,
  matchingService,
  postulacionesService,
  semillerosService,
} from "./services";
import type { FiltroSemilleros } from "@/types/api";

/** Sesión de demostración; reemplazar por el usuario autenticado vía OAuth/JWT. */
export const SESION_DEMO = {
  idEstudiante: 88,
  idSemilleroLiderado: 4,
  nombre: "Laura Restrepo Gómez",
  rol: "ESTUDIANTE" as const,
  programa: "Ingeniería de Sistemas",
};

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

export const postulacionesRecibidasQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["postulaciones", "semillero", idSemillero],
    queryFn: () => postulacionesService.recibidas(idSemillero),
  });

export const recomendacionesQuery = (idEstudiante: number) =>
  queryOptions({
    queryKey: ["matching", idEstudiante],
    queryFn: () => matchingService.recomendaciones(idEstudiante),
  });

export const analiticaQuery = () =>
  queryOptions({
    queryKey: ["analitica", "institucional"],
    queryFn: () => analiticaService.institucional(),
  });
