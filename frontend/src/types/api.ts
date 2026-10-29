/**
 * Tipos/DTOs del backend Spring Boot de Agora UdeA.
 * Mantener alineados con los DTOs expuestos por la API.
 */

export type RolUsuario = "ESTUDIANTE" | "PROFESOR" | "ADMINISTRADOR";

export type EstadoSemillero =
  "ACTIVO" | "EN_PAUSA" | "PENDIENTE_APROBACION" | "INACTIVO";

export type EstadoConvocatoria = "ABIERTA" | "CERRADA" | "BORRADOR";

// Mismos estados de postulación que tbl_estados (Pendiente, Pre-aprobada, Aceptada, ...)
export type EstadoPostulacion =
  | "PENDIENTE"
  | "PRE_APROBADA"
  | "ACEPTADA"
  | "RECHAZADA"
  | "RECHAZADA_POR_ESTUDIANTE"
  | "EXPIRADA"
  | "CANCELADA";

export interface ProgramaAcademico {
  idPrograma: number;
  nombre: string;
  facultad: string;
}

export interface Habilidad {
  idHabilidad: number;
  nombre: string;
}

export interface Usuario {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correoInstitucional: string;
  rol: RolUsuario;
}

export interface UsuarioSesion {
  idUsuario: number;
  nombreCompleto: string;
  email: string;
  rol: string;
  idEstudiante: number | null;
  idProfesor: number | null;
  idSemillerosLiderados: number[];
  // Un estudiante puede ser integrante activo de varios semilleros a la vez
  idSemillerosIntegrante: number[];
}

export interface Estudiante {
  idEstudiante: number;
  usuario: Usuario;
  programa: ProgramaAcademico;
  semestre: number;
  tags: string[];
  habilidades: Habilidad[];
}

export interface Profesor {
  idProfesor: number;
  usuario: Usuario;
  programa: ProgramaAcademico;
  areasEspecialidad: string[];
}

export interface Semillero {
  idSemillero: number;
  idEstudianteLider: number;
  nombreLider: string;
  nombre: string;
  descripcion: string;
  lineasInvestigacion: string[];
  estado: EstadoSemillero;
  facultad: string;
  programa?: ProgramaAcademico;
  lideres: Profesor[];
  totalProyectos: number;
  totalPublicaciones: number;
  totalIntegrantes: number;
  convocatoriaAbierta: boolean;
}

/** Equipo visible solo para quienes pertenecen al semillero (incluye correos). */
export interface IntegranteSemillero {
  idEstudiante: number;
  nombre: string;
  email: string;
  programa: string;
  semestre: number;
}

export interface TutorSemillero {
  idProfesor: number;
  nombre: string;
  email: string;
}

export interface Convocatoria {
  idConvocatoria: number;
  idSemillero: number;
  nombreSemillero: string;
  titulo: string;
  descripcion: string;
  fechaApertura: string;
  fechaCierre: string;
  cuposTotales: number;
  cuposDisponibles: number;
  estado: EstadoConvocatoria;
  habilidadesRequeridas: string[];
}

export interface Postulacion {
  idPostulacion: number;
  idConvocatoria: number;
  tituloConvocatoria: string;
  nombreSemillero: string;
  idEstudiante: number;
  nombreEstudiante: string;
  programaEstudiante: string;
  estado: EstadoPostulacion;
  fechaPostulacion: string;
  fechaPreAprobacion?: string | null;
  fechaDecisionEstudiante?: string | null;
  // Plazo para responder una oferta pre-aprobada; vencido, la oferta expira
  fechaLimiteRespuesta?: string | null;
  disponibilidadHorasSemana: number;
  porcentajeMatch?: number;
  // Servicio 2: solo en postulaciones recibidas que siguen en proceso
  priorizacion?: PriorizacionCandidato;
  habilidades: string[];
  cartaMotivacion?: string;
}

/** Servicio 2: priorización de un candidato dentro de su convocatoria (0-100). */
export interface PriorizacionCandidato {
  posicion: number;
  puntaje: number;
  cuposDisponibles: number;
  ofertasVigentes: number;
  desglose: {
    afinidadTematica: number;
    habilidades: number;
    disponibilidad: number;
    compromiso: number;
  };
  observaciones: string[];
}

export interface Proyecto {
  idProyecto: number;
  idSemillero: number;
  titulo: string;
  estado: string;
  fechaInicio: string;
  fechaFin?: string | null;
  presupuestoAsignado: number;
  integrantes: number;
}

export interface Publicacion {
  idPublicacion: number;
  idSemillero: number;
  titulo: string;
  tipo: string;
  anio: number;
  doi?: string | null;
  enlace?: string | null;
  autores: { nombre: string; rolAutoria: string; esEstudiante: boolean }[];
}

/** Servicio 1: motor de afinidad (0-100). */
export interface ResultadoMatch {
  idSemillero: number;
  nombre: string;
  porcentajeMatch: number;
  factoresCoincidencia: string[];
  desglose: {
    habilidades: number;
    areasEspecialidad: number;
    convocatoriaAbierta: number;
    proximidadTematica: number;
  };
  convocatoriaAbierta: boolean;
  facultad: string;
}

/** Servicio 3: indicadores institucionales. */
export interface KpiInstitucional {
  totalSemilleros: number;
  semillerosActivos: number;
  convocatoriasAbiertas: number;
  postulacionesActivas: number;
  tasaDemandaPromedio: number;
  tiempoRespuestaEstudianteHoras: number;
  tasaRechazoOfertas: number;
  indiceConversionProyectoPublicacion: number;
  participacionEstudiantilAutorias: number;
  indiceInterdisciplinariedad: number;
}

export interface DemandaSemillero {
  idSemillero: number;
  nombre: string;
  cuposOfertados: number;
  postulacionesRecibidas: number;
  tasaDemanda: number;
}

export interface DistribucionReconocimientos {
  nombre: string;
  total: number;
}

export interface HabilidadEmergente {
  nombre: string;
  totalAceptadas: number;
}

export interface AnaliticaInstitucional {
  kpis: KpiInstitucional;
  demandaPorSemillero: DemandaSemillero[];
  reconocimientos: DistribucionReconocimientos[];
  habilidadesEmergentes: HabilidadEmergente[];
}

export interface FiltroSemilleros {
  busqueda?: string;
  facultad?: string;
  soloConvocatoriaAbierta?: boolean;
}
