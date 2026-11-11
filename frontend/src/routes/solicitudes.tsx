import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import {
  BadgeEstado,
  EncabezadoSeccion,
  Vacio,
} from "@/components/gestion/comunes";
import { FormularioSolicitudCatalogo } from "@/components/solicitudes/FormularioSolicitudCatalogo";
import { TarjetaSolicitudCatalogo } from "@/components/solicitudes/TarjetaSolicitudCatalogo";
import { gestionService } from "@/lib/api/gestion";
import {
  catalogoSemilleroQuery,
  esAdministrador,
  usuarioSesionQuery,
} from "@/lib/api/queries";
import { requerirSesion } from "@/lib/api/sesion";
import {
  ESTADO_SOLICITUD_PENDIENTE,
  solicitudesQueries,
} from "@/lib/api/solicitudes";
import { formatearFecha } from "@/lib/formato";

export const Route = createFileRoute("/solicitudes")({
  ssr: false,
  head: () => ({
    meta: [
      { title: "Solicitudes | Agora UdeA" },
      {
        name: "description",
        content:
          "Propón elementos del catálogo y consulta tus solicitudes de retiro y las que esperan tu respuesta.",
      },
    ],
  }),
  loader: async ({ context }) => {
    const sesion = await requerirSesion();
    // El administrador responde solicitudes desde /administracion
    if (esAdministrador(sesion)) {
      throw redirect({
        to: "/administracion",
        search: { seccion: "semilleros" },
      });
    }
    await Promise.all([
      context.queryClient.ensureQueryData(solicitudesQueries.misCatalogo()),
      context.queryClient.ensureQueryData(catalogoSemilleroQuery()),
    ]);
  },
  component: SolicitudesPage,
});

function SolicitudesPage() {
  const { data: sesion } = useQuery(usuarioSesionQuery());
  const { data: misCatalogo } = useSuspenseQuery(
    solicitudesQueries.misCatalogo(),
  );
  const liderados = sesion?.idSemillerosLiderados ?? [];

  return (
    <AppShell>
      <PageHeader
        eyebrow="Solicitudes"
        title="Solicitudes"
        description="Propón al administrador elementos nuevos del catálogo y sigue el estado de tus solicitudes."
      />
      <div className="space-y-10">
        {liderados.length > 0 && <RetirosPorResponder liderados={liderados} />}

        <div className="grid gap-8 lg:grid-cols-2">
          <FormularioSolicitudCatalogo />
          <section>
            <EncabezadoSeccion
              titulo={`Mis solicitudes de catálogo (${misCatalogo.length})`}
            />
            {misCatalogo.length === 0 ? (
              <Vacio>Aún no has propuesto elementos del catálogo.</Vacio>
            ) : (
              <ul className="space-y-3">
                {misCatalogo.map((s) => (
                  <TarjetaSolicitudCatalogo key={s.id} solicitud={s} />
                ))}
              </ul>
            )}
          </section>
        </div>

        {sesion?.idEstudiante != null && <MisRetiros />}
      </div>
    </AppShell>
  );
}

/** Para líderes y tutores: retiros pendientes en sus semilleros, con acceso a la gestión. */
function RetirosPorResponder({ liderados }: { liderados: number[] }) {
  const { data: pendientes = [] } = useQuery({
    queryKey: ["solicitudes", "retiro", "por-responder", liderados],
    queryFn: async () =>
      (
        await Promise.all(
          liderados.map((id) => gestionService.solicitudesRetiro(id)),
        )
      )
        .flat()
        .filter((s) => s.estado === ESTADO_SOLICITUD_PENDIENTE),
  });

  return (
    <section>
      <EncabezadoSeccion
        titulo={`Solicitudes de retiro por responder (${pendientes.length})`}
        descripcion="Integrantes de tus semilleros que pidieron salir. Respóndelas en la gestión del semillero, después de hablar con ellos si lo necesitas."
      />
      {pendientes.length === 0 ? (
        <Vacio>
          No hay solicitudes de retiro pendientes en tus semilleros.
        </Vacio>
      ) : (
        <ul className="panel divide-y divide-border">
          {pendientes.map((s) => (
            <li
              key={s.id}
              className="flex flex-wrap items-center justify-between gap-3 p-4"
            >
              <div>
                <p className="text-sm font-medium text-foreground">
                  {s.nombreEstudiante} · {s.nombreSemillero}
                </p>
                <p className="text-xs text-muted-foreground">
                  {formatearFecha(s.fechaSolicitud)}: "{s.motivo}"
                </p>
              </div>
              <Link
                to="/gestion/$idSemillero"
                params={{ idSemillero: String(s.idSemillero) }}
                search={{ seccion: "miembros" }}
                className="text-sm font-medium text-accent underline-offset-4 hover:underline"
              >
                Responder
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function MisRetiros() {
  const { data: retiros = [] } = useQuery(solicitudesQueries.misRetiros());
  if (retiros.length === 0) return null;
  return (
    <section>
      <EncabezadoSeccion
        titulo="Mis solicitudes de retiro"
        descripcion="Se piden desde la página del semillero, en la sección 'Retirarme del semillero'."
      />
      <ul className="panel divide-y divide-border">
        {retiros.map((s) => (
          <li key={s.id} className="p-4 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="font-medium text-foreground">{s.nombreSemillero}</p>
              <BadgeEstado nombre={s.estado} />
            </div>
            <p className="text-xs text-muted-foreground">
              {formatearFecha(s.fechaSolicitud)}: "{s.motivo}"
            </p>
            {s.fechaRespuesta && (
              <p className="text-xs text-muted-foreground">
                Respondió {s.nombreQuienResponde ?? "un líder"}
                {s.comentarioRespuesta ? `: "${s.comentarioRespuesta}"` : "."}
              </p>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
