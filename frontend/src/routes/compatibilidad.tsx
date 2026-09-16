import { createFileRoute, Link } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrowRight } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { MatchScore } from "@/components/common/MatchScore";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { recomendacionesQuery, SESION_DEMO } from "@/lib/api/queries";
import type { ResultadoMatch } from "@/types/api";

export const Route = createFileRoute("/compatibilidad")({
  head: () => ({
    meta: [
      { title: "Compatibilidad con semilleros | Agora UdeA" },
      {
        name: "description",
        content:
          "Ranking de afinidad ponderada entre el perfil académico del estudiante y los semilleros de investigación de la Universidad de Antioquia.",
      },
      { property: "og:title", content: "Compatibilidad con semilleros | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Motor de recomendación que pondera habilidades, especialidades del tutor, convocatorias abiertas y proximidad temática.",
      },
    ],
  }),
  loader: async ({ context }) => {
    await context.queryClient.ensureQueryData(recomendacionesQuery(SESION_DEMO.idEstudiante));
  },
  component: CompatibilidadPage,
});

const factores = [
  { clave: "habilidades", etiqueta: "Habilidades clave", peso: 40 },
  { clave: "areasEspecialidad", etiqueta: "Áreas de especialidad del tutor", peso: 25 },
  { clave: "convocatoriaAbierta", etiqueta: "Convocatoria abierta con cupos", peso: 20 },
  { clave: "proximidadTematica", etiqueta: "Proximidad temática y de programa", peso: 15 },
] as const;

function CompatibilidadPage() {
  const { data: resultados } = useSuspenseQuery(recomendacionesQuery(SESION_DEMO.idEstudiante));
  const ordenados = [...resultados].sort((a, b) => b.porcentajeMatch - a.porcentajeMatch);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Motor de afinidad"
        title="Compatibilidad con semilleros"
        description="El puntaje combina cuatro factores ponderados sobre el perfil del estudiante y la información oficial de cada semillero, priorizando aquellos con convocatorias vigentes."
      />

      <section aria-labelledby="ponderacion" className="mb-8">
        <h2 id="ponderacion" className="mb-3 font-display text-sm font-semibold text-foreground">
          Ponderación aplicada
        </h2>
        <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {factores.map((factor) => (
            <div key={factor.clave} className="panel p-4">
              <dt className="text-sm font-medium text-foreground">{factor.etiqueta}</dt>
              <dd className="mt-2 font-display text-2xl font-semibold tabular-nums text-primary">
                {factor.peso}%
              </dd>
            </div>
          ))}
        </dl>
      </section>

      <section aria-labelledby="ranking">
        <h2 id="ranking" className="mb-3 font-display text-sm font-semibold text-foreground">
          Ranking de afinidad para {SESION_DEMO.nombre}
        </h2>
        <ul className="space-y-4">
          {ordenados.map((match, indice) => (
            <ResultadoItem key={match.idSemillero} match={match} posicion={indice + 1} />
          ))}
        </ul>
      </section>
    </AppShell>
  );
}

function ResultadoItem({ match, posicion }: { match: ResultadoMatch; posicion: number }) {
  return (
    <li className="panel p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex gap-4">
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-sm border border-border bg-secondary font-display text-sm font-semibold tabular-nums text-primary">
            {posicion}
          </span>
          <div>
            <p className="label-caps">{match.facultad}</p>
            <h3 className="mt-1 font-display text-base font-semibold text-foreground">
              {match.nombre}
            </h3>
            <ul className="mt-2 flex flex-wrap gap-1.5">
              {match.factoresCoincidencia.map((factor) => (
                <li
                  key={factor}
                  className="rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
                >
                  {factor}
                </li>
              ))}
            </ul>
          </div>
        </div>
        <div className="w-full max-w-xs">
          <MatchScore porcentaje={match.porcentajeMatch} />
          <div className="mt-3 flex items-center justify-between gap-3">
            {match.convocatoriaAbierta ? (
              <EstadoBadge estado="ABIERTA" />
            ) : (
              <span className="text-xs text-muted-foreground">Sin convocatoria vigente</span>
            )}
            <Link
              to="/semilleros/$idSemillero"
              params={{ idSemillero: String(match.idSemillero) }}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-accent underline-offset-4 hover:underline"
            >
              Ver semillero
              <ArrowRight className="size-3.5" strokeWidth={1.75} aria-hidden />
            </Link>
          </div>
        </div>
      </div>

      <dl className="mt-5 grid gap-4 border-t border-border pt-4 sm:grid-cols-4">
        {factores.map((factor) => {
          const obtenido = match.desglose[factor.clave];
          return (
            <div key={factor.clave}>
              <dt className="label-caps">{factor.etiqueta}</dt>
              <dd className="mt-1 text-sm tabular-nums text-foreground">
                {obtenido.toFixed(1)}
                <span className="text-muted-foreground"> / {factor.peso}</span>
              </dd>
            </div>
          );
        })}
      </dl>
    </li>
  );
}
