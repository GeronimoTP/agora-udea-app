/**
 * SERVICIO 3: analítica institucional (administrador). La vista por semillero de líderes y
 * tutores está en ./gestion.
 */

import { apiRequest } from "./client";
import { convocatoriasService } from "./convocatorias";
import { semillerosService } from "./semilleros";
import type { AnaliticaInstitucional } from "@/types/api";

export const analiticaService = {
  institucional: async () => {
    const [dashboard, semilleros, convocatorias] = await Promise.all([
      apiRequest<AnaliticaDashboardApi>("/api/analitica/dashboard"),
      semillerosService.listar(),
      convocatoriasService.listarAbiertas(),
    ]);

    const demandaPorSemillero = dashboard.tasasDemanda.map((demanda) => ({
      idSemillero: demanda.idSemillero,
      nombre: demanda.nombreSemillero,
      cuposOfertados: demanda.cuposOfertados,
      postulacionesRecibidas: demanda.postulacionesRecibidas,
      tasaDemanda: demanda.tasaDemanda,
    }));
    const promedioDemanda = demandaPorSemillero.length
      ? demandaPorSemillero.reduce(
          (total, demanda) => total + demanda.tasaDemanda,
          0,
        ) / demandaPorSemillero.length
      : 0;
    return {
      kpis: {
        totalSemilleros: semilleros.length,
        semillerosActivos: semilleros.filter((semillero) =>
          semillero.estado.toLowerCase().includes("activ"),
        ).length,
        convocatoriasAbiertas: convocatorias.length,
        postulacionesActivas: dashboard.postulacionesActivas,
        tasaDemandaPromedio: promedioDemanda,
        tiempoRespuestaEstudianteHoras: dashboard.tiempoRespuesta.promedioHoras,
        tasaRechazoOfertas:
          dashboard.tasaRetencion.porcentajeRechazoYExpiracion,
        indiceConversionProyectoPublicacion:
          dashboard.conversionProyectos.promedioPublicacionesPorProyecto,
        participacionEstudiantilAutorias:
          dashboard.participacionEstudiantil.porcentajeParticipacionEstudiantil,
        indiceInterdisciplinariedad:
          dashboard.interdisciplinariedad.porcentajeInterdisciplinariedad,
      },
      demandaPorSemillero,
      reconocimientos: Object.entries(
        dashboard.impactoReconocimientos.distribucionPorEntidad ?? {},
      ).map(([nombre, total]) => ({ nombre, total })),
      habilidadesEmergentes: dashboard.habilidadesEmergentes.map(
        (habilidad) => ({
          nombre: habilidad.habilidad,
          totalAceptadas: habilidad.frecuencia,
        }),
      ),
    } satisfies AnaliticaInstitucional;
  },
};

interface AnaliticaDashboardApi {
  postulacionesActivas: number;
  tasasDemanda: {
    idSemillero: number;
    nombreSemillero: string;
    cuposOfertados: number;
    postulacionesRecibidas: number;
    tasaDemanda: number;
  }[];
  tiempoRespuesta: { promedioHoras: number };
  tasaRetencion: { porcentajeRechazoYExpiracion: number };
  conversionProyectos: { promedioPublicacionesPorProyecto: number };
  participacionEstudiantil: { porcentajeParticipacionEstudiantil: number };
  impactoReconocimientos: { distribucionPorEntidad: Record<string, number> };
  habilidadesEmergentes: { habilidad: string; frecuencia: number }[];
  interdisciplinariedad: { porcentajeInterdisciplinariedad: number };
}
