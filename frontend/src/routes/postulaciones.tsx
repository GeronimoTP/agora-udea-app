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
      await queryClient.invalidateQueries({ queryKey: ["postulaciones"] });
    } catch {
      toast.error(
        "La operación no pudo completarse. Verifica la conexión con el servidor.",
      );
    }
  }

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
      header: "Afinidad",
      width: "9rem",
      cell: (row) =>
        row.porcentajeMatch === undefined ? (
          <span className="text-muted-foreground">No calculada</span>
        ) : (
          <MatchScore porcentaje={row.porcentajeMatch} compact />
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
      header: "Aspirante",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.nombreEstudiante}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {row.programaEstudiante}
          </p>
        </div>
      ),
    },
    { header: "Convocatoria", cell: (row) => row.tituloConvocatoria },
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
      header: "Afinidad",
      width: "9rem",
      cell: (row) =>
        row.porcentajeMatch === undefined ? (
          <span className="text-muted-foreground">No calculada</span>
        ) : (
          <MatchScore porcentaje={row.porcentajeMatch} compact />
        ),
    },
    { header: "Estado", cell: (row) => <EstadoBadge estado={row.estado} /> },
    {
      header: "Evaluación",
      align: "right",
      cell: (row) =>
        row.estado === "PENDIENTE" ? (
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() =>
                ejecutar(
                  () => postulacionesService.preAprobar(row.idPostulacion),
                  "Postulación pre-aprobada. El estudiante debe confirmar su aceptación.",
                )
              }
              className="rounded-sm bg-accent px-3 py-1.5 text-xs font-medium text-accent-foreground transition-colors hover:bg-primary"
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
            ? "Cuando el líder pre-apruebe una postulación, confirma o declina la oferta. Al aceptar, el sistema descuenta el cupo y cancela automáticamente tus demás postulaciones activas."
            : "Revisa a los aspirantes de las convocatorias de los semilleros que lideras. Pre-aprobar no descuenta cupos: el cupo se asigna cuando el estudiante confirma la oferta."
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
          rows={recibidas}
          getRowKey={(row) => row.idPostulacion}
          columns={columnasLider}
          emptyMessage="No hay postulaciones recibidas en las convocatorias de tus semilleros."
        />
      )}
    </AppShell>
  );
}

function formatearFecha(valor?: string | null) {
  if (!valor) return "Pendiente";
  return new Date(valor).toLocaleDateString("es-CO", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
