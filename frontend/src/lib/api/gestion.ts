/**
 * GESTIÓN DEL SEMILLERO (vista de líder)
 *
 * Usa los DTOs del backend tal cual: la gestión necesita los IDs y los nombres
 * exactos de estado que devuelve Spring Boot. Cada mutación la valida el backend
 * con AutorizacionService (solo líderes del semillero).
 */

import { queryOptions } from "@tanstack/react-query";
import { apiRequest } from "./client";
import type { ItemCatalogo } from "./services";

// ============================================
// TIPOS (DTOs del backend)
// ============================================

export interface SemilleroDetalle {
  id: number;
  codigoIdentificador: string;
  nombre: string;
  descripcion: string;
  idEstudianteLider: number;
  idUsuarioLider: number;
  nombreEstudianteLider: string;
  idPrograma: number;
  nombrePrograma: string;
  nombreFacultad: string | null;
  estadoActual: string;
  lineasInvestigacion: string[];
  totalProyectos: number;
  totalPublicaciones: number;
  totalIntegrantes: number;
  convocatoriaAbierta: boolean;
}

export interface LineaSemillero {
  id: number;
  idLineaInvestigacion: number;
  nombreLineaInvestigacion: string;
}

export interface TutorSemillero {
  id: number;
  idProfesor: number;
  idUsuario: number;
  nombreProfesor: string;
  email: string;
  fechaAsignacion: string;
}

export interface IntegranteSemillero {
  id: number;
  idEstudiante: number;
  idUsuario: number;
  nombreEstudiante: string;
  email: string;
  nombrePrograma: string | null;
  semestre: number | null;
  fechaIngreso: string;
  fechaSalida: string | null;
  estadoVinculacion: string | null;
}

export interface ConvocatoriaGestion {
  id: number;
  idSemillero: number;
  titulo: string;
  descripcion: string;
  cuposTotales: number;
  cuposDisponibles: number;
  fechaInicio: string;
  fechaCierre: string;
  estadoActual: string;
}

export interface NuevaConvocatoria {
  idSemillero: number;
  titulo: string;
  descripcion: string;
  cuposTotales: number;
  fechaInicio: string;
  fechaCierre: string;
}

export interface ProyectoGestion {
  id: number;
  idSemillero: number;
  titulo: string;
  descripcion: string;
  presupuestoAsignado: number;
  fechaInicio: string;
  fechaFin: string;
  estadoActual: string;
  totalIntegrantes: number;
}

export interface NuevoProyecto {
  idSemillero: number;
  titulo: string;
  descripcion: string;
  presupuestoAsignado: number;
  fechaInicio: string;
  fechaFin: string;
}

export interface IntegranteProyecto {
  id: number;
  idEstudiante: number;
  nombreEstudiante: string;
  idRolProyecto: number;
  rolProyecto: string;
  fechaVinculacion: string;
}

export interface AutorPublicacion {
  idUsuario: number;
  nombre: string;
  rolAutoria: string;
  esEstudiante: boolean;
}

export interface PublicacionGestion {
  id: number;
  titulo: string;
  urlDocumento: string;
  fechaPublicacion: string;
  idProyecto: number;
  nombreProyecto: string;
  autoresDetalle: AutorPublicacion[];
}

export interface NuevaPublicacion {
  idProyecto: number;
  titulo: string;
  urlDocumento: string;
  fechaPublicacion: string;
}

export interface ReconocimientoGestion {
  id: number;
  idSemillero: number;
  idTipoReconocimiento: number;
  tipoReconocimiento: string;
  titulo: string;
  descripcion: string;
  idEntidadOtorgante: number;
  entidadOtorgante: string;
  fechaObtencion: string;
  urlCertificado: string;
}

export interface NuevoReconocimiento {
  idSemillero: number;
  idTipoReconocimiento: number;
  titulo: string;
  descripcion: string;
  idEntidadOtorgante: number;
  fechaObtencion: string;
  urlCertificado: string;
}

export interface EstadoCatalogo {
  id: number;
  nombre: string;
  descripcion: string;
}

// Cuerpo de PUT /api/semilleros/{id} (SemilleroRequestDTO); el líder no cambia por aquí
export interface DatosSemillero {
  codigoIdentificador: string;
  nombre: string;
  descripcion: string;
  idEstudianteLider: number;
  idPrograma: number;
  idLineasInvestigacion: number[];
}

// ============================================
// SERVICIOS
// ============================================

// Fecha local AAAA-MM-DD (toISOString usa UTC y en la noche de Colombia daría el día siguiente)
const hoy = () => {
  const ahora = new Date();
  return new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 10);
};

export const gestionService = {
  semillero: (id: number) =>
    apiRequest<SemilleroDetalle>(`/api/semilleros/${id}`),
  actualizarSemillero: (id: number, datos: DatosSemillero) =>
    apiRequest<SemilleroDetalle>(`/api/semilleros/${id}`, {
      method: "PUT",
      body: datos,
    }),
  cambiarEstado: (id: number, idEstado: number) =>
    apiRequest<SemilleroDetalle>(`/api/semilleros/${id}/estado/${idEstado}`, {
      method: "PATCH",
    }),
  transferirLiderazgo: (id: number, idEstudiante: number) =>
    apiRequest<SemilleroDetalle>(
      `/api/semilleros/${id}/transferir-liderazgo/${idEstudiante}`,
      { method: "POST" },
    ),
  eliminarSemillero: (id: number) =>
    apiRequest<void>(`/api/semilleros/${id}`, { method: "DELETE" }),

  lineas: (id: number) =>
    apiRequest<LineaSemillero[]>(`/api/semilleros/${id}/lineas-investigacion`),

  tutores: (id: number) =>
    apiRequest<TutorSemillero[]>(`/api/semilleros/${id}/profesores`),
  agregarTutor: (idSemillero: number, idProfesor: number) =>
    apiRequest<TutorSemillero>("/api/semilleros/profesores", {
      method: "POST",
      body: { idSemillero, idProfesor, fechaAsignacion: hoy() },
    }),
  quitarTutor: (idSemillero: number, idProfesor: number) =>
    apiRequest<void>(
      `/api/semilleros/${idSemillero}/profesores/${idProfesor}`,
      {
        method: "DELETE",
      },
    ),

  integrantes: (id: number) =>
    apiRequest<IntegranteSemillero[]>(`/api/semilleros/${id}/estudiantes`),
  retirarIntegrante: (idSemillero: number, idEstudiante: number) =>
    apiRequest<IntegranteSemillero>(
      `/api/semilleros/${idSemillero}/estudiantes/${idEstudiante}/retirar`,
      { method: "POST" },
    ),

  convocatorias: (id: number) =>
    apiRequest<ConvocatoriaGestion[]>(`/api/convocatorias/semillero/${id}`),
  crearConvocatoria: (datos: NuevaConvocatoria) =>
    apiRequest<ConvocatoriaGestion>("/api/convocatorias", {
      method: "POST",
      body: datos,
    }),
  actualizarConvocatoria: (id: number, datos: NuevaConvocatoria) =>
    apiRequest<ConvocatoriaGestion>(`/api/convocatorias/${id}`, {
      method: "PUT",
      body: datos,
    }),
  cerrarConvocatoria: (id: number) =>
    apiRequest<void>(`/api/convocatorias/${id}/cerrar`, { method: "POST" }),
  cancelarConvocatoria: (id: number) =>
    apiRequest<void>(`/api/convocatorias/${id}/cancelar`, { method: "POST" }),
  eliminarConvocatoria: (id: number) =>
    apiRequest<void>(`/api/convocatorias/${id}`, { method: "DELETE" }),

  proyectos: (id: number) =>
    apiRequest<ProyectoGestion[]>(`/api/proyectos/semillero/${id}`),
  crearProyecto: (datos: NuevoProyecto) =>
    apiRequest<ProyectoGestion>("/api/proyectos", {
      method: "POST",
      body: datos,
    }),
  actualizarProyecto: (id: number, datos: NuevoProyecto) =>
    apiRequest<ProyectoGestion>(`/api/proyectos/${id}`, {
      method: "PUT",
      body: datos,
    }),
  cambiarEstadoProyecto: (id: number, idEstado: number) =>
    apiRequest<ProyectoGestion>(`/api/proyectos/${id}/estado/${idEstado}`, {
      method: "PATCH",
    }),
  eliminarProyecto: (id: number) =>
    apiRequest<void>(`/api/proyectos/${id}`, { method: "DELETE" }),
  integrantesProyecto: (id: number) =>
    apiRequest<IntegranteProyecto[]>(`/api/proyectos/${id}/estudiantes`),
  vincularAProyecto: (
    idProyecto: number,
    idEstudiante: number,
    idRolProyecto: number,
  ) =>
    apiRequest<IntegranteProyecto>("/api/proyectos/estudiantes", {
      method: "POST",
      body: { idProyecto, idEstudiante, idRolProyecto },
    }),
  desvincularDeProyecto: (idProyecto: number, idEstudiante: number) =>
    apiRequest<void>(
      `/api/proyectos/${idProyecto}/estudiantes/${idEstudiante}`,
      {
        method: "DELETE",
      },
    ),

  publicaciones: (id: number) =>
    apiRequest<PublicacionGestion[]>(`/api/publicaciones/semillero/${id}`),
  crearPublicacion: (datos: NuevaPublicacion) =>
    apiRequest<PublicacionGestion>("/api/publicaciones", {
      method: "POST",
      body: datos,
    }),
  actualizarPublicacion: (id: number, datos: NuevaPublicacion) =>
    apiRequest<PublicacionGestion>(`/api/publicaciones/${id}`, {
      method: "PUT",
      body: datos,
    }),
  eliminarPublicacion: (id: number) =>
    apiRequest<void>(`/api/publicaciones/${id}`, { method: "DELETE" }),
  agregarAutor: (
    idPublicacion: number,
    idUsuario: number,
    idRolAutor: number,
  ) =>
    apiRequest<void>("/api/publicaciones/autores", {
      method: "POST",
      body: { idPublicacion, idUsuario, idRolAutor, fechaAsociacion: hoy() },
    }),
  quitarAutor: (idPublicacion: number, idUsuario: number) =>
    apiRequest<void>(
      `/api/publicaciones/${idPublicacion}/autores/${idUsuario}`,
      {
        method: "DELETE",
      },
    ),

  reconocimientos: (id: number) =>
    apiRequest<ReconocimientoGestion[]>(`/api/reconocimientos/semillero/${id}`),
  crearReconocimiento: (datos: NuevoReconocimiento) =>
    apiRequest<ReconocimientoGestion>("/api/reconocimientos", {
      method: "POST",
      body: datos,
    }),
  actualizarReconocimiento: (id: number, datos: NuevoReconocimiento) =>
    apiRequest<ReconocimientoGestion>(`/api/reconocimientos/${id}`, {
      method: "PUT",
      body: datos,
    }),
  eliminarReconocimiento: (id: number) =>
    apiRequest<void>(`/api/reconocimientos/${id}`, { method: "DELETE" }),

  estadosDeCategoria: (categoria: string) =>
    apiRequest<EstadoCatalogo[]>("/api/estados/categoria", {
      query: { nombre: categoria },
    }),
};

// ============================================
// QUERIES
// ============================================

// Todas las consultas de un semillero comparten el prefijo ["gestion", id]
// para invalidarlas juntas tras una mutación
export const claveGestion = (idSemillero: number) => ["gestion", idSemillero];

export const gestionQueries = {
  semillero: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "semillero"],
      queryFn: () => gestionService.semillero(id),
    }),
  lineas: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "lineas"],
      queryFn: () => gestionService.lineas(id),
    }),
  tutores: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "tutores"],
      queryFn: () => gestionService.tutores(id),
    }),
  integrantes: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "integrantes"],
      queryFn: () => gestionService.integrantes(id),
    }),
  convocatorias: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "convocatorias"],
      queryFn: () => gestionService.convocatorias(id),
    }),
  proyectos: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "proyectos"],
      queryFn: () => gestionService.proyectos(id),
    }),
  integrantesProyecto: (idSemillero: number, idProyecto: number) =>
    queryOptions({
      queryKey: [...claveGestion(idSemillero), "proyecto", idProyecto],
      queryFn: () => gestionService.integrantesProyecto(idProyecto),
    }),
  publicaciones: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "publicaciones"],
      queryFn: () => gestionService.publicaciones(id),
    }),
  reconocimientos: (id: number) =>
    queryOptions({
      queryKey: [...claveGestion(id), "reconocimientos"],
      queryFn: () => gestionService.reconocimientos(id),
    }),
};

export const catalogoGestionQuery = () =>
  queryOptions({
    queryKey: ["catalogo", "gestion"],
    queryFn: async () => {
      const [
        estadosSemillero,
        estadosProyecto,
        rolesProyecto,
        rolesAutor,
        tiposReconocimiento,
        entidadesOtorgantes,
      ] = await Promise.all([
        gestionService.estadosDeCategoria("Estado Semillero"),
        gestionService.estadosDeCategoria("Estado Proyecto"),
        apiRequest<ItemCatalogo[]>("/api/catalogos/roles-estudiante-proyecto"),
        apiRequest<ItemCatalogo[]>("/api/catalogos/roles-autor"),
        apiRequest<ItemCatalogo[]>("/api/catalogos/tipos-reconocimiento"),
        apiRequest<ItemCatalogo[]>("/api/catalogos/entidades-otorgantes"),
      ]);
      return {
        estadosSemillero,
        estadosProyecto,
        rolesProyecto,
        rolesAutor,
        tiposReconocimiento,
        entidadesOtorgantes,
      };
    },
  });
