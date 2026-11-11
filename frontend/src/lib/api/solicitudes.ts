/**
 * SOLICITUDES que alguien debe aprobar o rechazar (decisiones de la reunión con el profesor):
 * - Aprobación de semilleros nuevos por el administrador.
 * - Solicitudes de catálogo: estudiantes y profesores proponen habilidades, líneas, áreas,
 *   entidades y tipos de reconocimiento; el administrador decide.
 * - Retiro voluntario: el integrante lo pide (las respuestas de los líderes están en ./gestion).
 * - Avisos del menú: cuántas solicitudes esperan respuesta del usuario de la sesión.
 *
 * Usa los DTOs del backend tal cual, como la gestión.
 */

import { queryOptions } from "@tanstack/react-query";
import { apiRequest } from "./client";
import type { SemilleroDetalle } from "./gestion";

export type TipoElementoCatalogo =
  | "HABILIDAD"
  | "LINEA_INVESTIGACION"
  | "AREA_ESPECIALIDAD"
  | "ENTIDAD_OTORGANTE"
  | "TIPO_RECONOCIMIENTO";

export const TIPOS_CATALOGO: {
  tipo: TipoElementoCatalogo;
  etiqueta: string;
  requiereDescripcion: boolean;
}[] = [
  { tipo: "HABILIDAD", etiqueta: "Habilidad", requiereDescripcion: false },
  {
    tipo: "LINEA_INVESTIGACION",
    etiqueta: "Línea de investigación",
    requiereDescripcion: true,
  },
  {
    tipo: "AREA_ESPECIALIDAD",
    etiqueta: "Área de especialidad",
    requiereDescripcion: true,
  },
  {
    tipo: "ENTIDAD_OTORGANTE",
    etiqueta: "Entidad otorgante",
    requiereDescripcion: true,
  },
  {
    tipo: "TIPO_RECONOCIMIENTO",
    etiqueta: "Tipo de reconocimiento",
    requiereDescripcion: true,
  },
];

// Nombres de tbl_estados (categoría "Estado Solicitud")
export const ESTADO_SOLICITUD_PENDIENTE = "Pendiente";
export const ESTADO_SOLICITUD_APROBADA = "Aprobada";

// SolicitudCatalogoResponseDTO
export interface SolicitudCatalogo {
  id: number;
  tipo: TipoElementoCatalogo;
  etiquetaTipo: string;
  nombre: string;
  descripcion: string | null;
  idAreaEspecialidad: number | null;
  nombreAreaEspecialidad: string | null;
  justificacion: string;
  idSolicitante: number;
  nombreSolicitante: string;
  rolSolicitante: string;
  estado: string;
  fechaSolicitud: string;
  fechaRespuesta: string | null;
  nombreQuienResponde: string | null;
  comentarioRespuesta: string | null;
  idElementoCreado: number | null;
}

export interface NuevaSolicitudCatalogo {
  tipo: TipoElementoCatalogo;
  nombre: string;
  descripcion?: string;
  idAreaEspecialidad?: number;
  justificacion: string;
}

// Ajustes opcionales del administrador antes de crear el elemento
export interface AjustesAprobacion {
  nombre?: string;
  descripcion?: string;
  idAreaEspecialidad?: number;
  comentario?: string;
}

// SolicitudRetiroResponseDTO
export interface SolicitudRetiro {
  id: number;
  idSemillero: number;
  nombreSemillero: string;
  idEstudiante: number;
  nombreEstudiante: string;
  motivo: string;
  estado: string;
  fechaSolicitud: string;
  fechaRespuesta: string | null;
  nombreQuienResponde: string | null;
  comentarioRespuesta: string | null;
}

// PendientesResponseDTO
export interface Pendientes {
  semillerosPorAprobar: number;
  solicitudesCatalogo: number;
  solicitudesRetiro: number;
}

export const solicitudesService = {
  // Aprobación de semilleros (administrador)
  semillerosPorAprobar: () =>
    apiRequest<SemilleroDetalle[]>("/api/semilleros/pendientes-aprobacion"),
  aprobarSemillero: (id: number) =>
    apiRequest<SemilleroDetalle>(`/api/semilleros/${id}/aprobar`, {
      method: "POST",
    }),
  rechazarSemillero: (id: number, motivo: string) =>
    apiRequest<SemilleroDetalle>(`/api/semilleros/${id}/rechazar`, {
      method: "POST",
      body: { motivo },
    }),

  // Catálogo
  solicitarCatalogo: (datos: NuevaSolicitudCatalogo) =>
    apiRequest<SolicitudCatalogo>("/api/solicitudes-catalogo", {
      method: "POST",
      body: datos,
    }),
  misSolicitudesCatalogo: () =>
    apiRequest<SolicitudCatalogo[]>("/api/solicitudes-catalogo/mias"),
  solicitudesCatalogo: (soloPendientes: boolean) =>
    apiRequest<SolicitudCatalogo[]>("/api/solicitudes-catalogo", {
      query: { soloPendientes: String(soloPendientes) },
    }),
  aprobarCatalogo: (id: number, ajustes: AjustesAprobacion) =>
    apiRequest<SolicitudCatalogo>(`/api/solicitudes-catalogo/${id}/aprobar`, {
      method: "POST",
      body: ajustes,
    }),
  rechazarCatalogo: (id: number, motivo: string) =>
    apiRequest<SolicitudCatalogo>(`/api/solicitudes-catalogo/${id}/rechazar`, {
      method: "POST",
      body: { motivo },
    }),

  // Retiro voluntario (estudiante)
  solicitarRetiro: (idSemillero: number, motivo: string) =>
    apiRequest<SolicitudRetiro>("/api/solicitudes-retiro", {
      method: "POST",
      body: { idSemillero, motivo },
    }),
  misSolicitudesRetiro: () =>
    apiRequest<SolicitudRetiro[]>("/api/solicitudes-retiro/mias"),

  pendientes: () => apiRequest<Pendientes>("/api/pendientes"),
};

// Todas bajo el prefijo "solicitudes": una respuesta invalida los avisos del menú
export const solicitudesQueries = {
  pendientes: () =>
    queryOptions({
      queryKey: ["solicitudes", "pendientes"],
      queryFn: () => solicitudesService.pendientes(),
      // Avisos del menú: se refrescan solos cada minuto
      refetchInterval: 60_000,
    }),
  semillerosPorAprobar: () =>
    queryOptions({
      queryKey: ["solicitudes", "semilleros"],
      queryFn: () => solicitudesService.semillerosPorAprobar(),
    }),
  catalogo: (soloPendientes: boolean) =>
    queryOptions({
      queryKey: ["solicitudes", "catalogo", soloPendientes],
      queryFn: () => solicitudesService.solicitudesCatalogo(soloPendientes),
    }),
  misCatalogo: () =>
    queryOptions({
      queryKey: ["solicitudes", "catalogo", "mias"],
      queryFn: () => solicitudesService.misSolicitudesCatalogo(),
    }),
  misRetiros: () =>
    queryOptions({
      queryKey: ["solicitudes", "retiro", "mias"],
      queryFn: () => solicitudesService.misSolicitudesRetiro(),
    }),
};
