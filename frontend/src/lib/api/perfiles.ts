/**
 * PERFILES de estudiantes y profesores. Usa los DTOs del backend tal cual. El perfil propio
 * va por /me (el backend toma la identidad del token, no de un ID en la URL); los listados de
 * los buscadores no traen correo.
 */

import { apiRequest } from "./client";

// ProfesorResponseDTO del backend (perfil propio)
export interface PerfilProfesorCompleto {
  id: number;
  nombreCompleto: string;
  email: string;
  idPrograma: number | null;
  nombrePrograma: string | null;
  idFacultad: number | null;
  nombreFacultad: string | null;
  areasEspecialidad: string[];
}

// ProfesorResumenDTO: listado para elegir tutores, sin correo
export interface Profesor {
  id: number;
  nombreCompleto: string;
  idPrograma: number | null;
  nombrePrograma: string | null;
  idFacultad: number | null;
  nombreFacultad: string | null;
  areasEspecialidad: string[];
}

// EstudianteResponseDTO del backend (perfil propio o de un integrante)
export interface PerfilEstudianteCompleto {
  id: number;
  nombreCompleto: string;
  email: string;
  nombrePrograma: string | null;
  semestre: number;
  lineasInvestigacion: string[];
  habilidades: string[];
}

// EstudianteResumenDTO: listado para elegir al líder de un semillero, sin correo
export interface EstudianteResumen {
  id: number;
  nombreCompleto: string;
  nombrePrograma: string | null;
  semestre: number;
  // Ya lidera un semillero (aprobado o no): no puede liderar otro
  lideraSemillero: boolean;
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

export const perfilService = {
  miPerfilEstudiante: () =>
    apiRequest<PerfilEstudianteCompleto>("/api/estudiantes/me"),

  // PUT reemplaza programa, semestre, líneas y habilidades en una sola transacción
  actualizarEstudiante: (payload: PerfilEstudiante) =>
    apiRequest<PerfilEstudianteCompleto>("/api/estudiantes/me", {
      method: "PUT",
      body: payload,
    }),

  miPerfilProfesor: () =>
    apiRequest<PerfilProfesorCompleto>("/api/profesores/me"),

  actualizarProfesor: (payload: PerfilProfesor) =>
    apiRequest<PerfilProfesorCompleto>("/api/profesores/me", {
      method: "PUT",
      body: payload,
    }),
};

export const profesoresService = {
  listar: () => apiRequest<Profesor[]>("/api/profesores"),
};

// Candidatos a estudiante líder cuando un profesor registra un semillero (solo profesores,
// líderes y administrador pueden consultarlo)
export const estudiantesResumenService = {
  listar: () => apiRequest<EstudianteResumen[]>("/api/estudiantes"),
};
