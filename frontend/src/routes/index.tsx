import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  CalendarClock,
  FileText,
  Library,
  Plus,
  Target,
} from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { StatCard } from "@/components/common/StatCard";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { MatchScore } from "@/components/common/MatchScore";
import {
  convocatoriasAbiertasQuery,
  esAdministrador,
  misPostulacionesQuery,
  postulacionesRecibidasQuery,
  recomendacionesQuery,
  semillerosQuery,
} from "@/lib/api/queries";
import { ApiError, apiRequest } from "@/lib/api/client";
import type { UsuarioSesion } from "@/types/api";

export const Route = createFileRoute("/")({
  ssr: false,
  beforeLoad: () => {
    const isAuthenticated = localStorage.getItem("access_token");

    if (!isAuthenticated) {
      throw redirect({
        to: "/auth/login",
      });
    }
  },
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
    const isAuthenticated = localStorage.getItem("access_token");
    if (!isAuthenticated) {
      throw redirect({ to: "/auth/login" });
    }

    let sesion: UsuarioSesion;
    try {
      sesion = await apiRequest<UsuarioSesion>("/api/usuarios/me");
    } catch (error) {
      if (error instanceof ApiError && [403, 404].includes(error.status)) {
        throw redirect({ to: "/auth/select-role" });
      }
      throw error;
    }
    // El administrador no tiene tablero de estudiante/profesor: su página principal es la analítica
    if (esAdministrador(sesion)) {
      throw redirect({ to: "/analitica" });
    }
    const consultas: Promise<unknown>[] = [
      context.queryClient.ensureQueryData(convocatoriasAbiertasQuery()),
    ];

    if (sesion.idEstudiante !== null) {
      consultas.push(
        context.queryClient.ensureQueryData(
          misPostulacionesQuery(sesion.idEstudiante),
        ),
        context.queryClient.ensureQueryData(
          recomendacionesQuery(sesion.idEstudiante),
        ),
      );
    }

    await Promise.all(consultas);
    return sesion;
  },
  component: TableroPage,
});

const accesos: {
  to: "/semilleros" | "/compatibilidad" | "/postulaciones";
  titulo: string;
  detalle: string;
  icon: typeof Library;
  soloEstudiante?: boolean;
}[] = [
  {
    to: "/semilleros",
    titulo: "Explorar semilleros",
    detalle:
      "Consulta líneas de investigación, proyectos y publicaciones por facultad.",
    icon: Library,
  },
  {
    to: "/compatibilidad",
    titulo: "Revisar compatibilidad",
    detalle:
      "Ranking de afinidad ponderada entre tu perfil y los semilleros activos.",
    icon: Target,
    soloEstudiante: true,
  },
  {
    to: "/postulaciones",
    titulo: "Gestionar postulaciones",
    detalle: "Seguimiento del flujo de doble vía y resolución en cascada.",
    icon: FileText,
  },
];

function TableroPage() {
  const sesion = Route.useLoaderData();
  const { data: convocatorias } = useSuspenseQuery(
    convocatoriasAbiertasQuery(),
  );
  const { data: postulaciones = [] } = useQuery({
    ...misPostulacionesQuery(sesion.idEstudiante ?? 0),
    enabled: sesion.idEstudiante !== null,
  });
  const { data: recomendaciones = [] } = useQuery({
    ...recomendacionesQuery(sesion.idEstudiante ?? 0),
    enabled: sesion.idEstudiante !== null,
  });

  const esEstudianteActual = sesion.idEstudiante !== null;
  const esProfesorActual = sesion.idProfesor !== null;
  const ofertasPendientes = postulaciones.filter(
    (p) => p.estado === "PRE_APROBADA",
  );

  return (
    <AppShell>
      <PageHeader
        eyebrow="Universidad de Antioquia"
        title="Tablero de gestión de semilleros"
        description="Resumen del estado del sistema de investigación formativa: semilleros registrados, convocatorias vigentes y avance de los procesos de vinculación."
      />

      <section aria-labelledby="indicadores" className="mb-10">
        <h2
          id="indicadores"
          className="mb-3 font-display text-sm font-semibold text-foreground"
        >
          Indicadores generales
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            label="Convocatorias abiertas"
            value={convocatorias.length}
            hint="Con cupos disponibles para postulación inmediata."
            icon={CalendarClock}
          />
          <StatCard
            label={
              sesion.idEstudiante !== null ? "Mis postulaciones" : "Mi perfil"
            }
            value={
              sesion.idEstudiante !== null ? postulaciones.length : sesion.rol
            }
            hint={
              sesion.idEstudiante !== null
                ? "Postulaciones asociadas a tu perfil de estudiante."
                : "Perfil docente registrado en Agora UdeA."
            }
            icon={FileText}
          />
          {sesion.idEstudiante !== null ? (
            <StatCard
              label="Semilleros recomendados"
              value={recomendaciones.length}
              hint="Semilleros afines a tu perfil según el motor de compatibilidad."
              icon={Target}
            />
          ) : (
            <StatCard
              label="Semilleros liderados"
              value={sesion.idSemillerosLiderados.length}
              hint="Semilleros en los que eres profesor vinculado."
              icon={Library}
            />
          )}
        </div>
      </section>

      {sesion.idSemillerosLiderados.length > 0 ? (
        <SemillerosLiderados
          idSemilleros={sesion.idSemillerosLiderados}
          puedeRegistrar={esProfesorActual}
        />
      ) : esProfesorActual ? (
        <ProfesorSinSemilleros />
      ) : (
        esEstudianteActual && (
          <section
            aria-labelledby="registrar-semillero"
            className="panel mb-10 flex flex-wrap items-center justify-between gap-4 p-5"
          >
            <div className="max-w-2xl">
              <h2
                id="registrar-semillero"
                className="font-display text-sm font-semibold text-foreground"
              >
                ¿Lideras un semillero avalado por la universidad?
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Regístralo con sus profesores tutores. Quedarás como líder y
                podrás gestionar sus convocatorias y postulaciones.
              </p>
            </div>
            <Link
              to="/semilleros/nuevo"
              className="inline-flex items-center gap-1.5 rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover"
            >
              <Plus className="size-4" strokeWidth={1.75} aria-hidden />
              Registrar semillero
            </Link>
          </section>
        )
      )}

      {sesion.idSemillerosIntegrante.length > 0 && (
        <SemillerosIntegrante idSemilleros={sesion.idSemillerosIntegrante} />
      )}

      <div className="grid gap-8 lg:grid-cols-3">
        <section aria-labelledby="convocatorias" className="lg:col-span-2">
          <div className="mb-3 flex items-end justify-between">
            <h2
              id="convocatorias"
              className="font-display text-sm font-semibold text-foreground"
            >
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
                      {convocatoria.cuposDisponibles} de{" "}
                      {convocatoria.cuposTotales}
                    </span>
                  </p>
                  <Link
                    to="/semilleros/$idSemillero"
                    params={{ idSemillero: String(convocatoria.idSemillero) }}
                    className="inline-flex items-center gap-1.5 rounded-sm bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground transition-colors hover:bg-primary"
                  >
                    {esEstudianteActual ? "Postularme" : "Ver semillero"}
                    <ArrowRight
                      className="size-3.5"
                      strokeWidth={1.75}
                      aria-hidden
                    />
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        </section>

        <div className="space-y-8">
          {sesion.idEstudiante !== null && (
            <section aria-labelledby="ofertas">
              <h2
                id="ofertas"
                className="mb-3 font-display text-sm font-semibold text-foreground"
              >
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
                          Pre-aprobada el{" "}
                          {formatearFecha(postulacion.fechaPreAprobacion)}
                        </p>
                        {postulacion.fechaLimiteRespuesta && (
                          <p className="mt-0.5 text-xs font-medium text-warning">
                            Responde antes del{" "}
                            {formatearFecha(postulacion.fechaLimiteRespuesta)}
                          </p>
                        )}
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
          )}

          {sesion.idEstudiante !== null && (
            <section aria-labelledby="afinidad">
              <h2
                id="afinidad"
                className="mb-3 font-display text-sm font-semibold text-foreground"
              >
                Mayor afinidad con tu perfil
              </h2>
              <div className="panel divide-y divide-border">
                {recomendaciones.slice(0, 3).map((match) => (
                  <div key={match.idSemillero} className="p-4">
                    <p className="text-sm font-medium leading-snug text-foreground">
                      {match.nombre}
                    </p>
                    <MatchScore
                      porcentaje={match.porcentajeMatch}
                      className="mt-2"
                      compact
                    />
                    <p className="mt-2 text-xs text-muted-foreground">
                      {match.factoresCoincidencia.join(" · ")}
                    </p>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      </div>

      <section aria-labelledby="accesos" className="mt-10">
        <h2
          id="accesos"
          className="mb-3 font-display text-sm font-semibold text-foreground"
        >
          Accesos rápidos
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {accesos
            .filter(
              ({ soloEstudiante }) => !soloEstudiante || esEstudianteActual,
            )
            .map(({ to, titulo, detalle, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                className="panel p-5 transition-colors hover:bg-secondary/50"
              >
                <Icon
                  className="size-5 text-accent"
                  strokeWidth={1.75}
                  aria-hidden
                />
                <p className="mt-3 font-display text-sm font-semibold text-foreground">
                  {titulo}
                </p>
                <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">
                  {detalle}
                </p>
              </Link>
            ))}
        </div>
      </section>
    </AppShell>
  );
}

// Un estudiante puede integrar varios semilleros a la vez: se listan todos
function SemillerosIntegrante({ idSemilleros }: { idSemilleros: number[] }) {
  const { data: semilleros = [] } = useQuery(semillerosQuery());
  const integrados = semilleros.filter((s) =>
    idSemilleros.includes(s.idSemillero),
  );

  return (
    <section aria-labelledby="participas" className="mb-10">
      <h2
        id="participas"
        className="mb-3 font-display text-sm font-semibold text-foreground"
      >
        {idSemilleros.length === 1
          ? "Semillero en el que participas"
          : `Semilleros en los que participas (${idSemilleros.length})`}
      </h2>
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {integrados.map((semillero) => (
          <li key={semillero.idSemillero}>
            <Link
              to="/semilleros/$idSemillero"
              params={{ idSemillero: String(semillero.idSemillero) }}
              className="panel block p-5 transition-colors hover:bg-secondary/50"
            >
              <p className="label-caps">{semillero.facultad}</p>
              <p className="mt-1 font-display text-sm font-semibold text-foreground">
                {semillero.nombre}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                {semillero.totalIntegrantes} integrantes ·{" "}
                {semillero.totalProyectos} proyectos ·{" "}
                {semillero.totalPublicaciones} publicaciones
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

// Profesor aún sin semilleros: explica cómo vincularse en lugar de dejar el tablero vacío
function ProfesorSinSemilleros() {
  return (
    <section
      aria-labelledby="sin-semilleros"
      className="panel mb-10 flex flex-wrap items-center justify-between gap-4 p-5"
    >
      <div className="max-w-2xl">
        <h2
          id="sin-semilleros"
          className="font-display text-sm font-semibold text-foreground"
        >
          Aún no estás vinculado a ningún semillero
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Puedes registrar un semillero y elegir a su estudiante líder. También
          puede incluirte como tutor un estudiante al registrar su semillero, o
          el líder de un semillero existente desde su gestión. Cuando quedes
          vinculado, aquí verás sus postulaciones, convocatorias y proyectos.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        <Link
          to="/semilleros"
          className="inline-flex items-center gap-1.5 rounded-sm border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
        >
          <Library className="size-4" strokeWidth={1.75} aria-hidden />
          Explorar semilleros
        </Link>
        <Link
          to="/semilleros/nuevo"
          className="inline-flex items-center gap-1.5 rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover"
        >
          <Plus className="size-4" strokeWidth={1.75} aria-hidden />
          Registrar semillero
        </Link>
      </div>
    </section>
  );
}

// Vista de líder: estudiante líder o profesor tutor de uno o varios semilleros
function SemillerosLiderados({
  idSemilleros,
  puedeRegistrar,
}: {
  idSemilleros: number[];
  // Un profesor puede registrar más semilleros; un estudiante lidera solo uno
  puedeRegistrar: boolean;
}) {
  const { data: semilleros = [] } = useQuery(semillerosQuery());
  const { data: recibidas = [] } = useQuery(
    postulacionesRecibidasQuery(idSemilleros),
  );
  const liderados = semilleros.filter((s) =>
    idSemilleros.includes(s.idSemillero),
  );

  return (
    <section aria-labelledby="mis-semilleros" className="mb-10">
      <div className="mb-3 flex items-end justify-between gap-3">
        <h2
          id="mis-semilleros"
          className="font-display text-sm font-semibold text-foreground"
        >
          Semilleros que lideras
        </h2>
        {puedeRegistrar && (
          <Link
            to="/semilleros/nuevo"
            className="inline-flex items-center gap-1 text-sm font-medium text-accent underline-offset-4 hover:underline"
          >
            <Plus className="size-3.5" strokeWidth={1.75} aria-hidden />
            Registrar otro semillero
          </Link>
        )}
      </div>
      <ul className="grid gap-4 lg:grid-cols-2">
        {liderados.map((semillero) => {
          const porRevisar = recibidas.filter(
            (p) =>
              p.nombreSemillero === semillero.nombre &&
              p.estado === "PENDIENTE",
          ).length;
          return (
            <li key={semillero.idSemillero} className="panel p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="label-caps">{semillero.facultad}</p>
                  <h3 className="mt-1 font-display text-base font-semibold text-foreground">
                    {semillero.nombre}
                  </h3>
                </div>
                <EstadoBadge estado={semillero.estado} />
              </div>
              <dl className="mt-4 grid grid-cols-3 gap-3 border-t border-border pt-4 text-sm">
                <div>
                  <dt className="label-caps">Integrantes</dt>
                  <dd className="mt-1 tabular-nums text-foreground">
                    {semillero.totalIntegrantes}
                  </dd>
                </div>
                <div>
                  <dt className="label-caps">Proyectos</dt>
                  <dd className="mt-1 tabular-nums text-foreground">
                    {semillero.totalProyectos}
                  </dd>
                </div>
                <div>
                  <dt className="label-caps">Por revisar</dt>
                  <dd className="mt-1 tabular-nums text-foreground">
                    {porRevisar}
                  </dd>
                </div>
              </dl>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link
                  to="/gestion/$idSemillero"
                  params={{ idSemillero: String(semillero.idSemillero) }}
                  search={{ seccion: "informacion" }}
                  className="rounded-sm bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover"
                >
                  Gestionar semillero
                </Link>
                <Link
                  to="/gestion/$idSemillero"
                  params={{ idSemillero: String(semillero.idSemillero) }}
                  search={{ seccion: "convocatorias" }}
                  className="rounded-sm border border-border-strong px-3 py-1.5 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
                >
                  Convocatorias
                </Link>
                <Link
                  to="/postulaciones"
                  className="rounded-sm bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground transition-colors hover:bg-primary"
                >
                  Revisar postulaciones
                </Link>
                <Link
                  to="/semilleros/$idSemillero"
                  params={{ idSemillero: String(semillero.idSemillero) }}
                  className="rounded-sm border border-border-strong px-3 py-1.5 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
                >
                  Ver semillero
                </Link>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
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
