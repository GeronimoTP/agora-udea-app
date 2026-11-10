/**
 * PERFILES de estudiantes y profesores. Usa los DTOs del backend tal cual: los formularios
 * de perfil y los buscadores de la gestión necesitan los mismos campos.
 */

import { apiRequest } from "./client";

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

export const profesoresService = {
  listar: () => apiRequest<Profesor[]>("/api/profesores"),
};

// Candidatos a estudiante líder cuando un profesor registra un semillero
export const estudiantesResumenService = {
  listar: () => apiRequest<EstudianteResumen[]>("/api/estudiantes"),
};
