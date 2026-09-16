import { createFileRoute } from "@tanstack/react-router";
import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { DataTable, type Columna } from "@/components/common/DataTable";
import { MatchScore } from "@/components/common/MatchScore";
import { misPostulacionesQuery, postulacionesRecibidasQuery, SESION_DEMO } from "@/lib/api/queries";
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
      { property: "og:title", content: "Postulaciones y selección de doble vía | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Panel de gestión de postulaciones a semilleros de investigación de la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context }) => {
    await Promise.all([
      context.queryClient.ensureQueryData(misPostulacionesQuery(SESION_DEMO.idEstudiante)),
      context.queryClient.ensureQueryData(
        postulacionesRecibidasQuery(SESION_DEMO.idSemilleroLiderado),
      ),
    ]);
  },
  component: PostulacionesPage,
});

type Vista = "ESTUDIANTE" | "LIDER";

function PostulacionesPage() {
  const [vista, setVista] = useState<Vista>("ESTUDIANTE");
  const queryClient = useQueryClient();
  const { data: propias } = useSuspenseQuery(misPostulacionesQuery(SESION_DEMO.idEstudiante));
  const { data: recibidas } = useSuspenseQuery(
    postulacionesRecibidasQuery(SESION_DEMO.idSemilleroLiderado),
  );

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string) {
    try {
      await accion();
      toast.success(mensaje);
      await queryClient.invalidateQueries({ queryKey: ["postulaciones"] });
    } catch {
      toast.error("La operación no pudo completarse. Verifica la conexión con el servidor.");
    }
  }

  const columnasEstudiante: Columna<Postulacion>[] = [
    {
      header: "Semillero y convocatoria",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.nombreSemillero}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{row.tituloConvocatoria}</p>
        </div>
      ),
    },
    { header: "Estado", cell: (row) => <EstadoBadge estado={row.estado} /> },
    { header: "Postulación", cell: (row) => formatearFecha(row.fechaPostulacion) },
    { header: "Pre-aprobación", cell: (row) => formatearFecha(row.fechaPreAprobacion) },
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
          <span className="text-xs text-muted-foreground">Sin acciones pendientes</span>
        ),
    },
  ];

  const columnasLider: Columna<Postulacion>[] = [
    {
      header: "Aspirante",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.nombreEstudiante}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{row.programaEstudiante}</p>
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
        row.estado === "ENVIADA" || row.estado === "EN_REVISION" ? (
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
          <span className="text-xs text-muted-foreground">Proceso resuelto</span>
        ),
    },
  ];

  return (
    <AppShell>
      <PageHeader
        eyebrow="Flujo de selección de doble vía"
        title="Gestión de postulaciones"
        description="El líder emite una pre-aprobación y el estudiante confirma la decisión final. Al aceptar una oferta, el sistema cancela automáticamente las demás postulaciones activas y descuenta el cupo correspondiente."
      />

      <div
        role="tablist"
        aria-label="Perspectiva de gestión"
        className="mb-6 inline-flex rounded-sm border border-border bg-surface p-1"
      >
        {(
          [
            { valor: "ESTUDIANTE", etiqueta: "Mis postulaciones" },
            { valor: "LIDER", etiqueta: "Postulaciones recibidas" },
          ] as const
        ).map((opcion) => (
          <button
            key={opcion.valor}
            type="button"
            role="tab"
            aria-selected={vista === opcion.valor}
            onClick={() => setVista(opcion.valor)}
            className={
              vista === opcion.valor
                ? "rounded-sm bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground"
                : "rounded-sm px-4 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            }
          >
            {opcion.etiqueta}
          </button>
        ))}
      </div>

      {vista === "ESTUDIANTE" ? (
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
          emptyMessage="No hay postulaciones recibidas en las convocatorias del semillero."
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
