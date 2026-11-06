import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Network } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { MatchScore } from "@/components/common/MatchScore";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import {
  esEstudiante,
  recomendacionesQuery,
  semillerosSimilaresQuery,
  usuarioSesionQuery,
} from "@/lib/api/queries";
import type {
  ResultadoMatch,
  SemilleroSimilar,
  SemillerosSimilares,
} from "@/types/api";

export const Route = createFileRoute("/compatibilidad")({
  // Las consultas usan el token de localStorage: solo pueden ejecutarse en el navegador
  ssr: false,
  head: () => ({
    meta: [
      { title: "Compatibilidad con semilleros | Agora UdeA" },
      {
        name: "description",
        content:
          "Ranking de afinidad ponderada entre el perfil académico del estudiante y los semilleros de investigación de la Universidad de Antioquia.",
      },
      {
        property: "og:title",
        content: "Compatibilidad con semilleros | Agora UdeA",
      },
      {
        property: "og:description",
        content:
          "Motor de recomendación que pondera habilidades, especialidades del tutor, convocatorias abiertas y proximidad temática.",
      },
    ],
  }),
  loader: async ({ context }) => {
    const sesion =
      await context.queryClient.ensureQueryData(usuarioSesionQuery());
    // El motor de recomendación solo aplica a perfiles de estudiante
    if (!esEstudiante(sesion)) {
      throw redirect({ to: "/" });
    }
    await context.queryClient.ensureQueryData(
      recomendacionesQuery(sesion.idEstudiante!),
    );
    return sesion;
  },
  component: CompatibilidadPage,
});

const factores = [
  { clave: "habilidades", etiqueta: "Habilidades clave", peso: 40 },
  {
    clave: "areasEspecialidad",
    etiqueta: "Áreas de especialidad del tutor",
    peso: 25,
  },
  {
    clave: "convocatoriaAbierta",
    etiqueta: "Convocatoria abierta con cupos",
    peso: 20,
  },
  {
    clave: "proximidadTematica",
    etiqueta: "Proximidad temática y de programa",
    peso: 15,
  },
] as const;

function CompatibilidadPage() {
  const sesion = Route.useLoaderData();
  const { data: resultados = [] } = useQuery({
    ...recomendacionesQuery(sesion.idEstudiante ?? 0),
    enabled: sesion.idEstudiante !== null,
  });
  const ordenados = [...resultados].sort(
    (a, b) => b.porcentajeMatch - a.porcentajeMatch,
  );

  return (
    <AppShell>
      <PageHeader
        eyebrow="Motor de afinidad"
        title="Compatibilidad con semilleros"
        description="El puntaje combina cuatro factores ponderados sobre el perfil del estudiante y la información oficial de cada semillero, priorizando aquellos con convocatorias vigentes."
      />

      {sesion.idEstudiante === null ? (
        <div className="panel p-5 text-sm text-muted-foreground">
          Las recomendaciones de compatibilidad están disponibles para perfiles
          de estudiante.
        </div>
      ) : (
        <>
          <SeccionSimilares idEstudiante={sesion.idEstudiante} />

          <section aria-labelledby="ponderacion" className="mb-8">
            <h2
              id="ponderacion"
              className="mb-3 font-display text-sm font-semibold text-foreground"
            >
              Ponderación aplicada
            </h2>
            <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {factores.map((factor) => (
                <div key={factor.clave} className="panel p-4">
                  <dt className="text-sm font-medium text-foreground">
                    {factor.etiqueta}
                  </dt>
                  <dd className="mt-2 font-display text-2xl font-semibold tabular-nums text-primary">
                    {factor.peso}%
                  </dd>
                </div>
              ))}
            </dl>
          </section>

          <section aria-labelledby="ranking">
            <h2
              id="ranking"
              className="mb-3 font-display text-sm font-semibold text-foreground"
            >
              Ranking de afinidad para {sesion.nombreCompleto}
            </h2>
            <ul className="space-y-4">
              {ordenados.map((match, indice) => (
                <ResultadoItem
                  key={match.idSemillero}
                  match={match}
                  posicion={indice + 1}
                />
              ))}
            </ul>
          </section>
        </>
      )}
    </AppShell>
  );
}

function ResultadoItem({
  match,
  posicion,
}: {
  match: ResultadoMatch;
  posicion: number;
}) {
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
              <span className="text-xs text-muted-foreground">
                Sin convocatoria vigente
              </span>
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

const LIMITE_SIMILARES = 6;

function SeccionSimilares({ idEstudiante }: { idEstudiante: number }) {
  const { data, isPending, isError } = useQuery(
    semillerosSimilaresQuery(idEstudiante, LIMITE_SIMILARES),
  );

  return (
    <section
      aria-labelledby="titulo-similares"
      id="similares"
      className="mb-10"
    >
      <h2
        id="titulo-similares"
        className="font-display text-sm font-semibold text-foreground"
      >
        Semilleros similares a los tuyos
      </h2>
      <p className="mt-1 mb-3 max-w-3xl text-sm text-muted-foreground">
        {data ? describirBase(data) : "Buscando semilleros parecidos…"}
      </p>

      {isPending ? (
        <div className="panel p-5 text-sm text-muted-foreground">
          Calculando similitud…
        </div>
      ) : isError ? (
        <div className="panel p-5 text-sm text-muted-foreground">
          No se pudieron calcular los semilleros similares.
        </div>
      ) : data.similares.length === 0 ? (
        <div className="panel p-5 text-sm text-muted-foreground">
          {data.base === "SIN_DATOS"
            ? "Elige líneas de investigación en tu perfil para encontrar semilleros parecidos."
            : "No hay otros semilleros activos con temáticas parecidas a las tuyas."}
        </div>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {data.similares.map((similar) => (
            <TarjetaSimilar key={similar.idSemillero} similar={similar} />
          ))}
        </ul>
      )}

      {data && data.busqueda.semillerosEnArbol > 0 && (
        <p className="mt-3 flex items-start gap-2 text-xs text-muted-foreground">
          <Network
            className="mt-0.5 size-3.5 shrink-0"
            strokeWidth={1.75}
            aria-hidden
          />
          <span>
            Árbol KD de {data.busqueda.semillerosEnArbol} semilleros en{" "}
            {data.busqueda.dimensiones} dimensiones (una por área de
            especialidad), profundidad {data.busqueda.profundidadArbol}.
            {data.busqueda.nodosVisitados > 0 &&
              ` Para reunir ${data.busqueda.candidatosEvaluados} candidatos (los resultados más sus posibles empates), la búsqueda visitó ${data.busqueda.nodosVisitados} de ${data.busqueda.semillerosEnArbol} nodos.`}
          </span>
        </p>
      )}
    </section>
  );
}

function describirBase(data: SemillerosSimilares) {
  if (data.base === "SIN_DATOS") {
    return "Aún no hay datos para comparar: no perteneces a semilleros con líneas de investigación ni tienes líneas en tu perfil.";
  }
  if (data.base === "PERFIL") {
    return "Aún no perteneces a un semillero, así que la búsqueda parte de las líneas de investigación de tu perfil.";
  }
  const actuales = data.referencias
    .filter((r) => r.actual)
    .map((r) => r.nombre);
  const anteriores = data.referencias
    .filter((r) => !r.actual)
    .map((r) => r.nombre);
  const partes = [
    actuales.length > 0 && `tus semilleros actuales (${actuales.join(", ")})`,
    anteriores.length > 0 &&
      `aquellos en los que estuviste (${anteriores.join(", ")}), con la mitad de peso`,
  ].filter(Boolean);
  return `Temáticamente cercanos a ${partes.join(" y ")}.`;
}

function TarjetaSimilar({ similar }: { similar: SemilleroSimilar }) {
  return (
    <li className="panel flex flex-col p-5">
      <p className="label-caps">
        {similar.nombrePrograma ?? similar.codigoIdentificador}
      </p>
      <h3 className="mt-1 font-display text-base font-semibold text-foreground">
        {similar.nombre}
      </h3>
      <MatchScore
        porcentaje={similar.porcentajeSimilitud}
        etiqueta="Similitud"
        className="mt-3"
      />
      {similar.areasEnComun.length > 0 && (
        <ul className="mt-3 flex flex-wrap gap-1.5">
          {similar.areasEnComun.map((area) => (
            <li
              key={area}
              className="rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
            >
              {area}
            </li>
          ))}
        </ul>
      )}
      {similar.lineasEnComun.length > 0 && (
        <p className="mt-3 text-xs text-foreground">
          Líneas en común: {similar.lineasEnComun.join(", ")}
        </p>
      )}
      {/* mb-4 separa el pie; mt-auto lo alinea abajo entre tarjetas */}
      <p className="mt-1 mb-4 text-xs text-muted-foreground">
        {similar.parecidoA
          ? `Parecido a ${similar.parecidoA}`
          : "Según las líneas de tu perfil"}
      </p>
      <div className="mt-auto flex items-center justify-between gap-3 border-t border-border pt-3">
        {similar.convocatoriaAbierta ? (
          <EstadoBadge estado="ABIERTA" />
        ) : (
          <span className="text-xs text-muted-foreground">
            Sin convocatoria vigente
          </span>
        )}
        <Link
          to="/semilleros/$idSemillero"
          params={{ idSemillero: String(similar.idSemillero) }}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-accent underline-offset-4 hover:underline"
        >
          Ver semillero
          <ArrowRight className="size-3.5" strokeWidth={1.75} aria-hidden />
        </Link>
      </div>
    </li>
  );
}
