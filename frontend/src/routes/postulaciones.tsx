import { createFileRoute, redirect } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { DataTable, type Columna } from "@/components/common/DataTable";
import { MatchScore } from "@/components/common/MatchScore";
import {
  esEstudiante,
  esProfesor,
  misPostulacionesQuery,
  postulacionesRecibidasQuery,
  usuarioSesionQuery,
} from "@/lib/api/queries";
import { postulacionesService } from "@/lib/api/services";
import type { Postulacion } from "@/types/api";

export const Route = createFileRoute("/postulaciones")({
  head: () => ({
    meta: [
      { title: "Postulaciones y selección de doble vía | Agora UdeA" },
      {
        name: "description",
        content:
          "Seguimiento de postulaciones a semilleros: pre-aprobación del líder, decisión final del estudiante y resolución en cascada de ofertas.",
      },
      {
        property: "og:title",
        content: "Postulaciones y selección de doble vía | Agora UdeA",
      },
      {
        property: "og:description",
        content:
          "Panel de gestión de postulaciones a semilleros de investigación de la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context }) => {
    const sesion =
      await context.queryClient.ensureQueryData(usuarioSesionQuery());
    // Solo estudiantes (sus postulaciones) y profesores (postulaciones recibidas)
    if (!esEstudiante(sesion) && !esProfesor(sesion)) {
      throw redirect({ to: "/" });
    }
    const consultas: Promise<unknown>[] = [];
    if (sesion.idEstudiante !== null) {
      consultas.push(
        context.queryClient.ensureQueryData(
          misPostulacionesQuery(sesion.idEstudiante),
        ),
      );
    }
    if (sesion.idSemillerosLiderados.length > 0) {
      consultas.push(
        context.queryClient.ensureQueryData(
          postulacionesRecibidasQuery(sesion.idSemillerosLiderados),
        ),
      );
    }
    await Promise.all(consultas);
    return sesion;
  },
  component: PostulacionesPage,
});

type Vista = "ESTUDIANTE" | "LIDER";

const etiquetasVista: Record<Vista, string> = {
  ESTUDIANTE: "Mis postulaciones",
  LIDER: "Postulaciones recibidas",
};

function PostulacionesPage() {
  const sesion = Route.useLoaderData();
  // Un estudiante líder de semillero ve sus postulaciones y también las recibidas
  const vistas: Vista[] = [
    ...(esEstudiante(sesion) ? (["ESTUDIANTE"] as const) : []),
    ...(esProfesor(sesion) || sesion.idSemillerosLiderados.length > 0
      ? (["LIDER"] as const)
      : []),
  ];
  const [vista, setVista] = useState<Vista>(vistas[0] ?? "LIDER");
  const vistaEstudiante = vista === "ESTUDIANTE";
  const queryClient = useQueryClient();
  const { data: propias = [] } = useQuery({
    ...misPostulacionesQuery(sesion.idEstudiante ?? 0),
    enabled: sesion.idEstudiante !== null,
  });
  const { data: recibidas = [] } = useQuery({
    ...postulacionesRecibidasQuery(sesion.idSemillerosLiderados),
    enabled: sesion.idSemillerosLiderados.length > 0,
  });

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string) {
    try {
      await accion();
      toast.success(mensaje);
    } catch (error) {
      // Reglas del backend: límite de ofertas, plazo vencido, convocatoria cerrada...
      toast.error(
        error instanceof Error && error.message
          ? error.message
          : "La operación no pudo completarse. Verifica la conexión con el servidor.",
      );
    } finally {
      await queryClient.invalidateQueries({ queryKey: ["postulaciones"] });
    }
  }

  // En proceso primero, agrupadas por convocatoria y en el orden de la priorización
  const recibidasOrdenadas = [...recibidas].sort((a, b) => {
    if (!!a.priorizacion !== !!b.priorizacion) return a.priorizacion ? -1 : 1;
    if (a.priorizacion && b.priorizacion) {
      return (
        a.idConvocatoria - b.idConvocatoria ||
        a.priorizacion.posicion - b.priorizacion.posicion
      );
    }
    return b.fechaPostulacion.localeCompare(a.fechaPostulacion);
  });

  const columnasEstudiante: Columna<Postulacion>[] = [
    {
      header: "Semillero y convocatoria",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.nombreSemillero}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {row.tituloConvocatoria}
          </p>
        </div>
      ),
    },
    { header: "Estado", cell: (row) => <EstadoBadge estado={row.estado} /> },
    {
      header: "Postulación",
      cell: (row) => formatearFecha(row.fechaPostulacion),
    },
    {
      header: "Pre-aprobación",
      cell: (row) => formatearFecha(row.fechaPreAprobacion),
    },
    {
      header: "Responder antes de",
      cell: (row) =>
        row.estado === "PRE_APROBADA" && row.fechaLimiteRespuesta ? (
          <span className="font-medium text-foreground">
            {formatearFechaHora(row.fechaLimiteRespuesta)}
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
    {
      header: "Decisión",
      align: "right",
      cell: (row) =>
        row.estado === "PRE_APROBADA" ? (
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() =>
                ejecutar(
                  () => postulacionesService.aceptarOferta(row.idPostulacion),
                  "Oferta aceptada. Las demás postulaciones activas se cancelan en cascada.",
                )
              }
              className="rounded-sm bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary-hover"
            >
              Aceptar
            </button>
            <button
              type="button"
              onClick={() =>
                ejecutar(
                  () => postulacionesService.rechazarOferta(row.idPostulacion),
                  "Oferta declinada. El cupo queda liberado para otros aspirantes.",
                )
              }
              className="rounded-sm border border-border-strong px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-secondary"
            >
              Declinar
            </button>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">
            Sin acciones pendientes
          </span>
        ),
    },
  ];

  const columnasLider: Columna<Postulacion>[] = [
    {
      header: "Prioridad",
      width: "11rem",
      cell: (row) =>
        row.priorizacion ? (
          <div>
            <div className="flex items-center gap-2">
              <span className="font-display text-sm font-semibold tabular-nums text-foreground">
                #{row.priorizacion.posicion}
              </span>
              <MatchScore
                porcentaje={row.priorizacion.puntaje}
                compact
                className="flex-1"
              />
            </div>
            <p className="mt-1 text-[11px] leading-snug text-muted-foreground">
              Temática {Math.round(row.priorizacion.desglose.afinidadTematica)}{" "}
              · Habilidades {Math.round(row.priorizacion.desglose.habilidades)}{" "}
              · Horas {Math.round(row.priorizacion.desglose.disponibilidad)} ·
              Compromiso {Math.round(row.priorizacion.desglose.compromiso)}
            </p>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">Resuelta</span>
        ),
    },
    {
      header: "Aspirante",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.nombreEstudiante}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {row.programaEstudiante} · {row.disponibilidadHorasSemana} h/semana
          </p>
          {row.priorizacion?.observaciones.map((observacion) => (
            <p key={observacion} className="mt-0.5 text-xs text-warning">
              {observacion}
            </p>
          ))}
        </div>
      ),
    },
    {
      header: "Convocatoria",
      cell: (row) => (
        <div>
          <p>{row.tituloConvocatoria}</p>
          {row.priorizacion && (
            <p className="mt-0.5 text-xs text-muted-foreground">
              Ofertas vigentes: {row.priorizacion.ofertasVigentes} de{" "}
              {row.priorizacion.cuposDisponibles} cupos
            </p>
          )}
        </div>
      ),
    },
    {
      header: "Habilidades",
      cell: (row) => (
        <ul className="flex flex-wrap gap-1">
          {row.habilidades.map((habilidad) => (
            <li
              key={habilidad}
              className="rounded-sm border border-border bg-secondary px-1.5 py-0.5 text-xs text-secondary-foreground"
            >
              {habilidad}
            </li>
          ))}
        </ul>
      ),
    },
    {
      header: "Estado",
      cell: (row) => (
        <div>
          <EstadoBadge estado={row.estado} />
          {row.estado === "PRE_APROBADA" && row.fechaLimiteRespuesta && (
            <p className="mt-1 text-xs text-muted-foreground">
              Responde hasta {formatearFechaHora(row.fechaLimiteRespuesta)}
            </p>
          )}
        </div>
      ),
    },
    {
      header: "Evaluación",
      align: "right",
      cell: (row) =>
        row.estado === "PENDIENTE" ? (
          <div className="flex justify-end gap-2">
            <button
              type="button"
              disabled={sinEspacioParaOfertas(row)}
              title={
                sinEspacioParaOfertas(row)
                  ? "Las ofertas vigentes ya igualan los cupos disponibles"
                  : undefined
              }
              onClick={() =>
                ejecutar(
                  () => postulacionesService.preAprobar(row.idPostulacion),
                  "Postulación pre-aprobada. El estudiante debe confirmar su aceptación.",
                )
              }
              className="rounded-sm bg-accent px-3 py-1.5 text-xs font-medium text-accent-foreground transition-colors hover:bg-primary disabled:cursor-not-allowed disabled:opacity-50"
            >
              Pre-aprobar
            </button>
            <button
              type="button"
              onClick={() =>
                ejecutar(
                  () => postulacionesService.rechazar(row.idPostulacion),
                  "Postulación rechazada.",
                )
              }
              className="rounded-sm border border-border-strong px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-secondary"
            >
              Rechazar
            </button>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">
            Proceso resuelto
          </span>
        ),
    },
  ];

  return (
    <AppShell>
      <PageHeader
        eyebrow="Flujo de selección de doble vía"
        title={etiquetasVista[vista]}
        description={
          vistaEstudiante
            ? "Cuando el líder pre-apruebe una postulación, confirma o declina la oferta antes de su fecha límite; si no respondes, la oferta expira. Al aceptar, el sistema descuenta el cupo y cancela automáticamente tus demás postulaciones activas."
            : "Los aspirantes en proceso aparecen ordenados por prioridad dentro de cada convocatoria (afinidad temática, habilidades, horas disponibles y compromiso con ofertas anteriores). Puedes emitir tantas ofertas vigentes como cupos disponibles: el cupo se asigna cuando el estudiante confirma."
        }
      />

      {vistas.length > 1 && (
        <div
          role="tablist"
          aria-label="Perspectiva de gestión"
          className="mb-6 inline-flex rounded-sm border border-border bg-surface p-1"
        >
          {vistas.map((opcion) => (
            <button
              key={opcion}
              type="button"
              role="tab"
              aria-selected={vista === opcion}
              onClick={() => setVista(opcion)}
              className={
                vista === opcion
                  ? "rounded-sm bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground"
                  : "rounded-sm px-4 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              }
            >
              {etiquetasVista[opcion]}
            </button>
          ))}
        </div>
      )}

      {vistaEstudiante ? (
        <DataTable<Postulacion>
          rows={propias}
          getRowKey={(row) => row.idPostulacion}
          columns={columnasEstudiante}
          emptyMessage="Aún no has registrado postulaciones a convocatorias."
        />
      ) : (
        <DataTable<Postulacion>
          rows={recibidasOrdenadas}
          getRowKey={(row) => row.idPostulacion}
          columns={columnasLider}
          emptyMessage="No hay postulaciones recibidas en las convocatorias de tus semilleros."
        />
      )}
    </AppShell>
  );
}

// Regla del Servicio 2: las ofertas vigentes no pueden superar los cupos disponibles
function sinEspacioParaOfertas(row: Postulacion) {
  return (
    !!row.priorizacion &&
    row.priorizacion.ofertasVigentes >= row.priorizacion.cuposDisponibles
  );
}

function formatearFechaHora(valor: string) {
  return new Date(valor).toLocaleString("es-CO", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function formatearFecha(valor?: string | null) {
  if (!valor) return "Pendiente";
  return new Date(valor).toLocaleDateString("es-CO", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
