/**
 * CATÁLOGOS: datos de referencia para formularios y filtros (programas, facultades, áreas,
 * líneas de investigación y habilidades).
 */

import { apiRequest } from "./client";

export interface ItemCatalogo {
  id: number;
  nombre: string;
}

export interface Habilidad {
  id: number;
  nombre: string;
}

export interface ProgramaCatalogo extends ItemCatalogo {
  nombreFacultad: string | null;
}

export const catalogoService = {
  programas: () =>
    apiRequest<ProgramaCatalogo[]>("/api/catalogos/programas-academicos"),
  facultades: () => apiRequest<ItemCatalogo[]>("/api/catalogos/facultades"),
  areasEspecialidad: () =>
    apiRequest<ItemCatalogo[]>("/api/areas-especialidad"),
  lineasInvestigacion: () =>
    apiRequest<ItemCatalogo[]>("/api/lineas-investigacion"),
  habilidades: () => apiRequest<Habilidad[]>("/api/habilidades"),
};
