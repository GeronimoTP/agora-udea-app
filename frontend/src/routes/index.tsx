import { createFileRoute, Link } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrowRight, BarChart3, CalendarClock, FileText, Library, Target, Users } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { StatCard } from "@/components/common/StatCard";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { MatchScore } from "@/components/common/MatchScore";
import {
  analiticaQuery,
  convocatoriasAbiertasQuery,
  misPostulacionesQuery,
  recomendacionesQuery,
  SESION_DEMO,
} from "@/lib/api/queries";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Tablero institucional | Agora UdeA" },
      {
        name: "description",
        content:
          "Tablero de Agora UdeA: indicadores de semilleros de investigación, convocatorias abiertas y estado de postulaciones de la Universidad de Antioquia.",
      },
      { property: "og:title", content: "Tablero institucional | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Indicadores institucionales, convocatorias abiertas y accesos rápidos a la gestión de semilleros de investigación de la UdeA.",
      },
    ],
  }),
  loader: async ({ context }) => {
    await Promise.all([
      context.queryClient.ensureQueryData(analiticaQuery()),
      context.queryClient.ensureQueryData(convocatoriasAbiertasQuery()),
      context.queryClient.ensureQueryData(misPostulacionesQuery(SESION_DEMO.idEstudiante)),
      context.queryClient.ensureQueryData(recomendacionesQuery(SESION_DEMO.idEstudiante)),
    ]);
  },
  component: TableroPage,
});

const accesos = [
  {
    to: "/semilleros" as const,
    titulo: "Explorar semilleros",
    detalle: "Consulta líneas de investigación, proyectos y publicaciones por facultad.",
    icon: Library,
  },
  {
    to: "/compatibilidad" as const,
    titulo: "Revisar compatibilidad",
    detalle: "Ranking de afinidad ponderada entre tu perfil y los semilleros activos.",
    icon: Target,
  },
  {
    to: "/postulaciones" as const,
    titulo: "Gestionar postulaciones",
    detalle: "Seguimiento del flujo de doble vía y resolución en cascada.",
    icon: FileText,
  },
  {
    to: "/analitica" as const,
    titulo: "Analítica institucional",
    detalle: "Indicadores de demanda, productividad científica e interdisciplinariedad.",
    icon: BarChart3,
  },
];

function TableroPage() {
  const { data: analitica } = useSuspenseQuery(analiticaQuery());
  const { data: convocatorias } = useSuspenseQuery(convocatoriasAbiertasQuery());
  const { data: postulaciones } = useSuspenseQuery(
    misPostulacionesQuery(SESION_DEMO.idEstudiante),
  );
  const { data: recomendaciones } = useSuspenseQuery(
    recomendacionesQuery(SESION_DEMO.idEstudiante),
  );

  const kpis = analitica.kpis;
  const ofertasPendientes = postulaciones.filter((p) => p.estado === "PRE_APROBADA");

  return (
    <AppShell>
      <PageHeader
        eyebrow="Universidad de Antioquia"
        title="Tablero de gestión de semilleros"
        description="Resumen del estado del sistema de investigación formativa: semilleros registrados, convocatorias vigentes y avance de los procesos de vinculación."
      />

      <section aria-labelledby="indicadores" className="mb-10">
        <h2 id="indicadores" className="mb-3 font-display text-sm font-semibold text-foreground">
          Indicadores generales
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            label="Semilleros activos"
            value={kpis.semillerosActivos}
            hint={`De ${kpis.totalSemilleros} semilleros registrados en la plataforma.`}
            icon={Library}
          />
          <StatCard
            label="Convocatorias abiertas"
            value={kpis.convocatoriasAbiertas}
            hint="Con cupos disponibles para postulación inmediata."
            icon={CalendarClock}
          />
          <StatCard
            label="Postulaciones activas"
            value={kpis.postulacionesActivas}
            hint="En revisión, pre-aprobadas o en espera de decisión del estudiante."
            icon={FileText}
          />
          <StatCard
            label="Interdisciplinariedad"
            value={kpis.indiceInterdisciplinariedad.toFixed(1)}
            unit="%"
            hint="Estudiantes vinculados a semilleros de un programa distinto al propio."
            icon={Users}
          />
        </div>
      </section>

      <div className="grid gap-8 lg:grid-cols-3">
        <section aria-labelledby="convocatorias" className="lg:col-span-2">
          <div className="mb-3 flex items-end justify-between">
            <h2 id="convocatorias" className="font-display text-sm font-semibold text-foreground">
              Convocatorias con cupos disponibles
            </h2>
            <Link
              to="/semilleros"
              className="text-sm font-medium text-accent underline-offset-4 hover:underline"
            >
              Ver todos los semilleros
            </Link>
          </div>
          <ul className="space-y-3">
            {convocatorias.map((convocatoria) => (
              <li key={convocatoria.idConvocatoria} className="panel p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="max-w-2xl">
                    <p className="label-caps">{convocatoria.nombreSemillero}</p>
                    <h3 className="mt-1 font-display text-base font-semibold text-foreground">
                      {convocatoria.titulo}
                    </h3>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                      {convocatoria.descripcion}
                    </p>
                  </div>
                  <EstadoBadge estado={convocatoria.estado} />
                </div>
                <div className="mt-4 flex flex-wrap items-center justify-between gap-4 border-t border-border pt-4 text-sm">
                  <p className="text-muted-foreground">
                    Cierra el{" "}
                    <span className="font-medium text-foreground">
                      {formatearFecha(convocatoria.fechaCierre)}
                    </span>
                  </p>
                  <p className="text-muted-foreground">
                    Cupos:{" "}
                    <span className="font-medium tabular-nums text-foreground">
                      {convocatoria.cuposDisponibles} de {convocatoria.cuposTotales}
                    </span>
                  </p>
                  <Link
                    to="/semilleros/$idSemillero"
                    params={{ idSemillero: String(convocatoria.idSemillero) }}
                    className="inline-flex items-center gap-1.5 rounded-sm bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground transition-colors hover:bg-primary"
                  >
                    Postularme
                    <ArrowRight className="size-3.5" strokeWidth={1.75} aria-hidden />
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        </section>

        <div className="space-y-8">
          <section aria-labelledby="ofertas">
            <h2 id="ofertas" className="mb-3 font-display text-sm font-semibold text-foreground">
              Ofertas por confirmar
            </h2>
            <div className="panel p-5">
              {ofertasPendientes.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  No tienes ofertas de admisión pendientes de respuesta.
                </p>
              ) : (
                <ul className="space-y-4">
                  {ofertasPendientes.map((postulacion) => (
                    <li key={postulacion.idPostulacion}>
                      <p className="text-sm font-medium text-foreground">
                        {postulacion.nombreSemillero}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        Pre-aprobada el {formatearFecha(postulacion.fechaPreAprobacion)}
                      </p>
                      <Link
                        to="/postulaciones"
                        className="mt-2 inline-flex text-sm font-medium text-accent underline-offset-4 hover:underline"
                      >
                        Responder oferta
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>

          <section aria-labelledby="afinidad">
            <h2 id="afinidad" className="mb-3 font-display text-sm font-semibold text-foreground">
              Mayor afinidad con tu perfil
            </h2>
            <div className="panel divide-y divide-border">
              {recomendaciones.slice(0, 3).map((match) => (
                <div key={match.idSemillero} className="p-4">
                  <p className="text-sm font-medium leading-snug text-foreground">{match.nombre}</p>
                  <MatchScore porcentaje={match.porcentajeMatch} className="mt-2" compact />
                  <p className="mt-2 text-xs text-muted-foreground">
                    {match.factoresCoincidencia.join(" · ")}
                  </p>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>

      <section aria-labelledby="accesos" className="mt-10">
        <h2 id="accesos" className="mb-3 font-display text-sm font-semibold text-foreground">
          Accesos rápidos
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {accesos.map(({ to, titulo, detalle, icon: Icon }) => (
            <Link key={to} to={to} className="panel p-5 transition-colors hover:bg-secondary/50">
              <Icon className="size-5 text-accent" strokeWidth={1.75} aria-hidden />
              <p className="mt-3 font-display text-sm font-semibold text-foreground">{titulo}</p>
              <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{detalle}</p>
            </Link>
          ))}
        </div>
      </section>
    </AppShell>
  );
}

function formatearFecha(valor?: string | null) {
  if (!valor) return "Sin registro";
  return new Date(valor).toLocaleDateString("es-CO", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}
