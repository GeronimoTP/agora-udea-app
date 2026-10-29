import { queryOptions } from "@tanstack/react-query";
import {
  analiticaService,
  catalogoService,
  convocatoriasService,
  estudiantesResumenService,
  perfilService,
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

export const integrantesSemilleroQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["semillero", idSemillero, "integrantes"],
    queryFn: () => semillerosService.integrantes(idSemillero),
  });

export const tutoresSemilleroQuery = (idSemillero: number) =>
  queryOptions({
    queryKey: ["semillero", idSemillero, "tutores"],
    queryFn: () => semillerosService.tutores(idSemillero),
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

// Postulaciones recibidas con la priorización del Servicio 2 en las que siguen en proceso
export const postulacionesRecibidasQuery = (idSemilleros: number[]) =>
  queryOptions({
    queryKey: ["postulaciones", "semilleros", idSemilleros],
    queryFn: async () =>
      (
        await Promise.all(
          idSemilleros.map(async (id) => {
            const [recibidas, priorizacion] = await Promise.all([
              postulacionesService.recibidas(id),
              postulacionesService.priorizacion(id),
            ]);
            const porPostulacion = new Map(
              priorizacion.map(({ idPostulacion, ...resto }) => [
                idPostulacion,
                resto,
              ]),
            );
            return recibidas.map((postulacion) => {
              const priorizada = porPostulacion.get(postulacion.idPostulacion);
              return priorizada
                ? {
                    ...postulacion,
                    priorizacion: priorizada,
                    porcentajeMatch: priorizada.puntaje,
                  }
                : postulacion;
            });
          }),
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

export const perfilEstudianteQuery = (idEstudiante: number) =>
  queryOptions({
    queryKey: ["perfil", "estudiante", idEstudiante],
    queryFn: () => perfilService.estudiante(idEstudiante),
  });

export const perfilProfesorQuery = (idProfesor: number) =>
  queryOptions({
    queryKey: ["perfil", "profesor", idProfesor],
    queryFn: () => perfilService.profesor(idProfesor),
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
