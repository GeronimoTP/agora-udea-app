import { createFileRoute, Link } from "@tanstack/react-router";
import {
  useQuery,
  useQueryClient,
  useSuspenseQuery,
} from "@tanstack/react-query";
import { AlertTriangle, ArrowLeft, ExternalLink, Mail } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { DataTable, type Columna } from "@/components/common/DataTable";
import {
  convocatoriasSemilleroQuery,
  habilidadesQuery,
  integrantesSemilleroQuery,
  tutoresSemilleroQuery,
  perfilEstudianteQuery,
  usuarioSesionQuery,
  proyectosQuery,
  publicacionesQuery,
  semilleroQuery,
} from "@/lib/api/queries";
import { postulacionesService } from "@/lib/api/postulaciones";
import {
  ESTADO_SOLICITUD_PENDIENTE,
  solicitudesQueries,
  solicitudesService,
} from "@/lib/api/solicitudes";
import type {
  Convocatoria,
  Proyecto,
  Publicacion,
  Semillero,
} from "@/types/api";
import { formatearFecha } from "@/lib/formato";

export const Route = createFileRoute("/semilleros/$idSemillero")({
  // Las consultas usan el token de localStorage: solo pueden ejecutarse en el navegador
  ssr: false,
  head: () => ({
    meta: [
      { title: "Detalle del semillero | Agora UdeA" },
      {
        name: "description",
        content:
          "Información del semillero: líneas de investigación, proyectos activos, producción académica y convocatorias de vinculación abiertas.",
      },
      { property: "og:title", content: "Detalle del semillero | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Proyectos, publicaciones y convocatorias de vinculación de un semillero de investigación de la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context, params }) => {
    const id = Number(params.idSemillero);
    await Promise.all([
      context.queryClient.ensureQueryData(semilleroQuery(id)),
      context.queryClient.ensureQueryData(convocatoriasSemilleroQuery(id)),
      context.queryClient.ensureQueryData(proyectosQuery(id)),
      context.queryClient.ensureQueryData(publicacionesQuery(id)),
    ]);
  },
  component: SemilleroDetallePage,
});

function SemilleroDetallePage() {
  const { idSemillero } = Route.useParams();
  const id = Number(idSemillero);
  const { data: semillero } = useSuspenseQuery(semilleroQuery(id));
  const { data: convocatorias } = useSuspenseQuery(
    convocatoriasSemilleroQuery(id),
  );
  const { data: proyectos } = useSuspenseQuery(proyectosQuery(id));
  const { data: publicaciones } = useSuspenseQuery(publicacionesQuery(id));
  // Solo los estudiantes pueden postularse a convocatorias
  const { data: sesion } = useQuery(usuarioSesionQuery());
  const idEstudiante = sesion?.idEstudiante ?? null;
  // Un estudiante puede integrar varios semilleros, pero no postularse al que ya integra o lidera
  const esIntegrante = sesion?.idSemillerosIntegrante.includes(id) ?? false;
  // El equipo (con correos) solo lo ven integrantes activos, el líder y los tutores
  const perteneceAlEquipo =
    esIntegrante || (sesion?.idSemillerosLiderados.includes(id) ?? false);
  const esLider = sesion?.idSemillerosLiderados.includes(id) ?? false;
  const motivoSinPostulacion =
    idEstudiante === null
      ? null
      : sesion?.idSemillerosLiderados.includes(id)
        ? "Lideras este semillero, así que no puedes postularte a sus convocatorias."
        : esIntegrante
          ? "Ya eres integrante de este semillero."
          : null;

  return (
    <AppShell>
      <Link
        to="/semilleros"
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-accent underline-offset-4 hover:underline"
      >
        <ArrowLeft className="size-3.5" strokeWidth={1.75} aria-hidden />
        Volver al directorio
      </Link>

      <PageHeader
        eyebrow={semillero.facultad}
        title={semillero.nombre}
        description={semillero.descripcion}
        actions={
          <div className="flex items-center gap-3">
            <EstadoBadge estado={semillero.estado} />
            {esIntegrante && (
              <span className="rounded-sm border border-primary bg-primary/10 px-2.5 py-1 text-xs font-medium text-foreground">
                Eres integrante
              </span>
            )}
            {sesion?.idSemillerosLiderados.includes(id) && (
              <Link
                to="/gestion/$idSemillero"
                params={{ idSemillero }}
                search={{ seccion: "informacion" }}
                className="rounded-sm bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover"
              >
                Gestionar
              </Link>
            )}
          </div>
        }
      />

      <AvisoAprobacion semillero={semillero} />

      <section aria-labelledby="lineas" className="mb-10">
        <h2 id="lineas" className="label-caps mb-2">
          Líneas de investigación
        </h2>
        <ul className="flex flex-wrap gap-2">
          {semillero.lineasInvestigacion.map((linea) => (
            <li
              key={linea}
              className="rounded-sm border border-border bg-surface px-2.5 py-1 text-sm text-foreground"
            >
              {linea}
            </li>
          ))}
        </ul>
      </section>

      {perteneceAlEquipo && (
        <EquipoSemillero idSemillero={id} nombreLider={semillero.nombreLider} />
      )}

      {esIntegrante && !esLider && (
        <RetiroVoluntario idSemillero={id} nombreSemillero={semillero.nombre} />
      )}

      <section aria-labelledby="convocatorias-semillero" className="mb-10">
        <h2
          id="convocatorias-semillero"
          className="mb-3 font-display text-sm font-semibold text-foreground"
        >
          Convocatorias de vinculación
        </h2>
        {convocatorias.length === 0 ? (
          <div className="panel p-6 text-sm text-muted-foreground">
            Este semillero no tiene convocatorias registradas actualmente.
          </div>
        ) : (
          <ul className="space-y-3">
            {convocatorias.map((convocatoria) => (
              <ConvocatoriaItem
                key={convocatoria.idConvocatoria}
                convocatoria={convocatoria}
                idEstudiante={idEstudiante}
                motivoSinPostulacion={motivoSinPostulacion}
              />
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="proyectos" className="mb-10">
        <h2
          id="proyectos"
          className="mb-3 font-display text-sm font-semibold text-foreground"
        >
          Proyectos de investigación
        </h2>
        <DataTable<Proyecto>
          rows={proyectos}
          getRowKey={(row) => row.idProyecto}
          columns={columnasProyectos}
        />
      </section>

      <section aria-labelledby="publicaciones">
        <h2
          id="publicaciones"
          className="mb-3 font-display text-sm font-semibold text-foreground"
        >
          Producción académica
        </h2>
        <DataTable<Publicacion>
          rows={publicaciones}
          getRowKey={(row) => row.idPublicacion}
          columns={columnasPublicaciones}
        />
      </section>
    </AppShell>
  );
}

/** Solo lo ven sus líderes y el administrador: para los demás el semillero no existe. */
function AvisoAprobacion({ semillero }: { semillero: Semillero }) {
  if (semillero.estado === "PENDIENTE_APROBACION") {
    return (
      <p className="mb-8 rounded-sm border border-warning/40 bg-warning/10 p-4 text-sm text-foreground">
        Este semillero está pendiente de aprobación del administrador. Solo lo
        ven sus líderes; cuando lo apruebe será público y podrá abrir
        convocatorias.
      </p>
    );
  }
  if (semillero.estado === "RECHAZADO") {
    return (
      <div className="mb-8 rounded-sm border border-destructive/40 bg-destructive/10 p-4 text-sm text-foreground">
        <p className="font-medium">El administrador rechazó este semillero.</p>
        {semillero.motivoRechazo && (
          <p className="mt-1">Motivo: {semillero.motivoRechazo}</p>
        )}
        <p className="mt-1 text-muted-foreground">
          Corrige los datos y reenvíalo desde la gestión del semillero.
        </p>
      </div>
    );
  }
  return null;
}

/**
 * Retiro voluntario: el integrante no sale de inmediato; envía una solicitud que un líder o
 * tutor aprueba o rechaza, para hablar antes con él.
 */
function RetiroVoluntario({
  idSemillero,
  nombreSemillero,
}: {
  idSemillero: number;
  nombreSemillero: string;
}) {
  const queryClient = useQueryClient();
  const { data: solicitudes = [] } = useQuery(solicitudesQueries.misRetiros());
  const ultima = solicitudes.find((s) => s.idSemillero === idSemillero);
  const pendiente = ultima?.estado === ESTADO_SOLICITUD_PENDIENTE;
  const [abierto, setAbierto] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);

  async function solicitar() {
    setEnviando(true);
    try {
      await solicitudesService.solicitarRetiro(idSemillero, motivo.trim());
      toast.success(
        "Solicitud enviada. El líder y los tutores la verán en la gestión del semillero.",
      );
      await queryClient.invalidateQueries({ queryKey: ["solicitudes"] });
      setAbierto(false);
      setMotivo("");
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "No se pudo enviar la solicitud.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section aria-labelledby="retiro" className="mb-10">
      <h2
        id="retiro"
        className="mb-1 font-display text-sm font-semibold text-foreground"
      >
        Retirarme del semillero
      </h2>
      <div className="panel p-5 text-sm">
        {pendiente && ultima ? (
          <p>
            Enviaste una solicitud de retiro el{" "}
            {formatearFecha(ultima.fechaSolicitud)}. Sigues siendo integrante
            hasta que un líder o tutor la responda; es posible que quieran
            hablar contigo antes.
          </p>
        ) : (
          <>
            {ultima?.estado === "Rechazada" && (
              <p className="mb-3 rounded-sm border border-border bg-secondary p-3">
                Tu última solicitud fue rechazada por{" "}
                {ultima.nombreQuienResponde ?? "un líder"}
                {ultima.comentarioRespuesta
                  ? `: "${ultima.comentarioRespuesta}"`
                  : "."}{" "}
                Puedes volver a solicitarlo.
              </p>
            )}
            {abierto ? (
              <div>
                <label
                  htmlFor="motivo-retiro"
                  className="label-caps mb-1.5 block"
                >
                  ¿Por qué quieres retirarte de {nombreSemillero}?
                </label>
                <textarea
                  id="motivo-retiro"
                  value={motivo}
                  onChange={(event) => setMotivo(event.target.value)}
                  rows={3}
                  maxLength={1000}
                  className="w-full rounded-sm border border-input bg-surface p-3 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
                  placeholder="Tu motivo le ayuda al líder a entender tu situación."
                />
                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    onClick={solicitar}
                    disabled={enviando || motivo.trim().length < 10}
                    className="rounded-sm bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground transition-colors hover:opacity-90 disabled:opacity-50"
                  >
                    {enviando ? "Enviando" : "Enviar solicitud de retiro"}
                  </button>
                  <button
                    type="button"
                    onClick={() => setAbierto(false)}
                    className="rounded-sm border border-border-strong px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-muted-foreground">
                  Tu retiro no es inmediato: el líder y los tutores reciben tu
                  solicitud y deciden, para poder hablar contigo antes.
                </p>
                <button
                  type="button"
                  onClick={() => setAbierto(true)}
                  className="rounded-sm border border-destructive/60 px-4 py-2 text-sm font-medium text-destructive transition-colors hover:bg-destructive/10"
                >
                  Solicitar retiro
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

function EquipoSemillero({
  idSemillero,
  nombreLider,
}: {
  idSemillero: number;
  nombreLider: string;
}) {
  const { data: integrantes = [], isLoading } = useQuery(
    integrantesSemilleroQuery(idSemillero),
  );
  const { data: tutores = [] } = useQuery(tutoresSemilleroQuery(idSemillero));

  return (
    <section aria-labelledby="equipo" className="mb-10">
      <h2
        id="equipo"
        className="mb-1 font-display text-sm font-semibold text-foreground"
      >
        Equipo del semillero
      </h2>
      <p className="mb-3 text-xs text-muted-foreground">
        Solo lo ven los integrantes, el líder y los tutores. Líder estudiantil:{" "}
        <span className="font-medium text-foreground">{nombreLider}</span>.
      </p>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <div className="panel p-5">
          <p className="label-caps mb-3">Tutores ({tutores.length})</p>
          <ul className="space-y-3">
            {tutores.map((tutor) => (
              <Persona
                key={tutor.idProfesor}
                nombre={tutor.nombre}
                email={tutor.email}
              />
            ))}
          </ul>
        </div>
        <div className="panel p-5">
          <p className="label-caps mb-3">
            Integrantes activos ({integrantes.length})
          </p>
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : integrantes.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Aún no hay integrantes vinculados.
            </p>
          ) : (
            <ul className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-3">
              {integrantes.map((integrante) => (
                <Persona
                  key={integrante.idEstudiante}
                  nombre={integrante.nombre}
                  detalle={`${integrante.programa} · Semestre ${integrante.semestre}`}
                  email={integrante.email}
                />
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}

function Persona({
  nombre,
  detalle,
  email,
}: {
  nombre: string;
  detalle?: string;
  email: string | null;
}) {
  return (
    <li className="min-w-0">
      <p className="truncate text-sm font-medium text-foreground">{nombre}</p>
      {detalle && (
        <p className="truncate text-xs text-muted-foreground">{detalle}</p>
      )}
      {email && (
        <a
          href={`mailto:${email}`}
          className="mt-0.5 inline-flex max-w-full items-center gap-1.5 text-xs text-accent underline-offset-4 hover:underline"
        >
          <Mail className="size-3.5 shrink-0" strokeWidth={1.75} aria-hidden />
          <span className="truncate">{email}</span>
        </a>
      )}
    </li>
  );
}

function ConvocatoriaItem({
  convocatoria,
  idEstudiante,
  motivoSinPostulacion,
}: {
  convocatoria: Convocatoria;
  idEstudiante: number | null;
  motivoSinPostulacion: string | null;
}) {
  const queryClient = useQueryClient();
  const [motivacion, setMotivacion] = useState("");
  const [experiencia, setExperiencia] = useState("");
  const [horas, setHoras] = useState(4);
  const [idHabilidades, setIdHabilidades] = useState<number[]>([]);
  // Requisitos de texto que el estudiante declara cumplir
  const [idRequisitosCumplidos, setIdRequisitosCumplidos] = useState<number[]>(
    [],
  );
  const [enviando, setEnviando] = useState(false);
  const [abierto, setAbierto] = useState(false);
  const { data: habilidades = [] } = useQuery({
    ...habilidadesQuery(),
    enabled: abierto,
  });
  const { data: perfil } = useQuery({
    ...perfilEstudianteQuery(idEstudiante ?? 0),
    enabled: abierto && idEstudiante !== null,
  });
  const [prellenado, setPrellenado] = useState(false);

  // Al abrir el formulario se proponen las habilidades del perfil; el estudiante las ajusta
  useEffect(() => {
    if (!abierto || prellenado || !perfil || habilidades.length === 0) return;
    setIdHabilidades(
      habilidades
        .filter((h) => perfil.habilidades.includes(h.nombre))
        .map((h) => h.id),
    );
    setPrellenado(true);
  }, [abierto, prellenado, perfil, habilidades]);
  const abiertaConCupos =
    convocatoria.estado === "ABIERTA" && convocatoria.cuposDisponibles > 0;
  const disponible =
    idEstudiante !== null && abiertaConCupos && motivoSinPostulacion === null;
  const formularioValido =
    motivacion.trim().length >= 20 &&
    experiencia.trim().length > 0 &&
    horas >= 2 &&
    idHabilidades.length > 0;

  // Los de habilidad se cumplen con las habilidades elegidas; los de texto, con la declaración
  const cumple = (requisito: Convocatoria["requisitos"][number]) =>
    requisito.idHabilidad !== null
      ? idHabilidades.includes(requisito.idHabilidad)
      : idRequisitosCumplidos.includes(requisito.id);
  const obligatoriosIncumplidos = convocatoria.requisitos.filter(
    (r) => r.obligatorio && !cumple(r),
  );

  function alternarRequisito(idRequisito: number) {
    setIdRequisitosCumplidos((actuales) =>
      actuales.includes(idRequisito)
        ? actuales.filter((idActual) => idActual !== idRequisito)
        : [...actuales, idRequisito],
    );
  }

  function alternarHabilidad(idHabilidad: number) {
    setIdHabilidades((actuales) =>
      actuales.includes(idHabilidad)
        ? actuales.filter((idActual) => idActual !== idHabilidad)
        : [...actuales, idHabilidad],
    );
  }

  async function postular() {
    if (idEstudiante === null) return;
    setEnviando(true);
    try {
      await postulacionesService.crear({
        idConvocatoria: convocatoria.idConvocatoria,
        idEstudiante,
        respuestaMotivacion: motivacion.trim(),
        experienciaPrevia: experiencia.trim(),
        disponibilidadHorasSemana: horas,
        idHabilidades,
        idRequisitosCumplidos,
      });
      toast.success("Postulación registrada correctamente.");
      await queryClient.invalidateQueries({ queryKey: ["postulaciones"] });
      setAbierto(false);
      setMotivacion("");
      setExperiencia("");
      setIdHabilidades([]);
      setIdRequisitosCumplidos([]);
    } catch (error) {
      toast.error(
        error instanceof Error && error.message
          ? error.message
          : "No fue posible registrar la postulación. Intenta nuevamente.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <li className="panel p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="max-w-2xl">
          <h3 className="font-display text-base font-semibold text-foreground">
            {convocatoria.titulo}
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
            {convocatoria.descripcion}
          </p>
        </div>
        <EstadoBadge estado={convocatoria.estado} />
      </div>

      <dl className="mt-4 grid gap-4 border-t border-border pt-4 text-sm sm:grid-cols-3">
        <div>
          <dt className="label-caps">Apertura</dt>
          <dd className="mt-1">
            {formatearFecha(convocatoria.fechaApertura, {
              siVacio: "En curso",
            })}
          </dd>
        </div>
        <div>
          <dt className="label-caps">Cierre</dt>
          <dd className="mt-1">
            {formatearFecha(convocatoria.fechaCierre, { siVacio: "En curso" })}
          </dd>
        </div>
        <div>
          <dt className="label-caps">Cupos disponibles</dt>
          <dd className="mt-1 tabular-nums">
            {convocatoria.cuposDisponibles} de {convocatoria.cuposTotales}
          </dd>
        </div>
      </dl>

      {convocatoria.requisitos.length > 0 && (
        <div className="mt-4">
          <p className="label-caps mb-1.5">Requisitos</p>
          <ul className="flex flex-wrap gap-1.5">
            {convocatoria.requisitos.map((requisito) => (
              <li
                key={requisito.id}
                className={
                  requisito.obligatorio
                    ? "rounded-sm border border-primary bg-primary/10 px-2 py-0.5 text-xs text-foreground"
                    : "rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
                }
              >
                {requisito.texto}
                <span className="ml-1 text-muted-foreground">
                  ({requisito.obligatorio ? "obligatorio" : "opcional"}
                  {requisito.idHabilidad !== null ? ", habilidad" : ""})
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {disponible ? (
        <div className="mt-5 border-t border-border pt-4">
          {abierto ? (
            <div>
              <label
                htmlFor={`motivacion-${convocatoria.idConvocatoria}`}
                className="label-caps mb-1.5 block"
              >
                Motivación
              </label>
              <textarea
                id={`motivacion-${convocatoria.idConvocatoria}`}
                value={motivacion}
                onChange={(event) => setMotivacion(event.target.value)}
                rows={4}
                className="w-full rounded-sm border border-input bg-surface p-3 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
                placeholder="Describe tu interés académico en la línea de investigación (mínimo 20 caracteres)."
              />

              <label
                htmlFor={`experiencia-${convocatoria.idConvocatoria}`}
                className="label-caps mb-1.5 mt-4 block"
              >
                Experiencia previa
              </label>
              <textarea
                id={`experiencia-${convocatoria.idConvocatoria}`}
                value={experiencia}
                onChange={(event) => setExperiencia(event.target.value)}
                rows={3}
                className="w-full rounded-sm border border-input bg-surface p-3 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
                placeholder="Cursos, proyectos o trabajos relacionados."
              />

              <label
                htmlFor={`horas-${convocatoria.idConvocatoria}`}
                className="label-caps mb-1.5 mt-4 block"
              >
                Disponibilidad (horas por semana)
              </label>
              <input
                id={`horas-${convocatoria.idConvocatoria}`}
                type="number"
                min={2}
                value={horas}
                onChange={(event) => setHoras(Number(event.target.value))}
                className="w-32 rounded-sm border border-input bg-surface p-2 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
              />

              <fieldset className="mt-4">
                <legend className="label-caps mb-1.5">
                  Habilidades que aportas
                </legend>
                <ul className="flex flex-wrap gap-1.5">
                  {habilidades.map((habilidad) => {
                    const seleccionada = idHabilidades.includes(habilidad.id);
                    return (
                      <li key={habilidad.id}>
                        <button
                          type="button"
                          aria-pressed={seleccionada}
                          onClick={() => alternarHabilidad(habilidad.id)}
                          className={
                            seleccionada
                              ? "rounded-sm border border-primary bg-primary px-2 py-0.5 text-xs text-primary-foreground"
                              : "rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground hover:border-border-strong"
                          }
                        >
                          {habilidad.nombre}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </fieldset>

              {convocatoria.requisitos.some((r) => r.idHabilidad === null) && (
                <fieldset className="mt-4">
                  <legend className="label-caps mb-1.5">
                    Requisitos que cumples
                  </legend>
                  <ul className="space-y-1.5">
                    {convocatoria.requisitos
                      .filter((r) => r.idHabilidad === null)
                      .map((requisito) => (
                        <li key={requisito.id}>
                          <label className="flex items-start gap-2 text-sm">
                            <input
                              type="checkbox"
                              checked={idRequisitosCumplidos.includes(
                                requisito.id,
                              )}
                              onChange={() => alternarRequisito(requisito.id)}
                              className="mt-0.5"
                            />
                            <span>
                              {requisito.texto}
                              <span className="ml-1 text-xs text-muted-foreground">
                                (
                                {requisito.obligatorio
                                  ? "obligatorio"
                                  : "opcional"}
                                )
                              </span>
                            </span>
                          </label>
                        </li>
                      ))}
                  </ul>
                </fieldset>
              )}

              {obligatoriosIncumplidos.length > 0 && (
                <p className="mt-4 flex items-start gap-2 rounded-sm border border-warning/40 bg-warning/10 p-3 text-sm text-foreground">
                  <AlertTriangle
                    className="mt-0.5 size-4 shrink-0"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                  <span>
                    No cumples estos requisitos obligatorios:{" "}
                    {obligatoriosIncumplidos.map((r) => r.texto).join(", ")}.
                    Puedes postularte igual, pero el líder lo verá y tu
                    postulación quedará después de las que sí los cumplen.
                  </span>
                </p>
              )}

              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={postular}
                  disabled={enviando || !formularioValido}
                  className="rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover disabled:opacity-50"
                >
                  {enviando ? "Enviando" : "Enviar postulación"}
                </button>
                <button
                  type="button"
                  onClick={() => setAbierto(false)}
                  className="rounded-sm border border-border-strong px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
                >
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setAbierto(true)}
              className="rounded-sm bg-accent px-4 py-2 text-sm font-medium text-accent-foreground transition-colors hover:bg-primary"
            >
              Postularme a esta convocatoria
            </button>
          )}
        </div>
      ) : idEstudiante !== null && abiertaConCupos && motivoSinPostulacion ? (
        <p className="mt-5 border-t border-border pt-4 text-sm text-muted-foreground">
          {motivoSinPostulacion}
        </p>
      ) : null}
    </li>
  );
}

const columnasProyectos: Columna<Proyecto>[] = [
  {
    header: "Proyecto",
    cell: (row) => <span className="font-medium">{row.titulo}</span>,
  },
  { header: "Estado", cell: (row) => <EstadoBadge estado={row.estado} /> },
  {
    header: "Inicio",
    cell: (row) => formatearFecha(row.fechaInicio, { siVacio: "En curso" }),
  },
  {
    header: "Finalización",
    cell: (row) => formatearFecha(row.fechaFin, { siVacio: "En curso" }),
  },
  {
    header: "Presupuesto",
    align: "right",
    cell: (row) =>
      row.presupuestoAsignado.toLocaleString("es-CO", {
        style: "currency",
        currency: "COP",
        maximumFractionDigits: 0,
      }),
  },
  { header: "Integrantes", align: "right", cell: (row) => row.integrantes },
];

const columnasPublicaciones: Columna<Publicacion>[] = [
  {
    header: "Título",
    cell: (row) => <span className="font-medium">{row.titulo}</span>,
  },
  { header: "Tipo", cell: (row) => row.tipo },
  { header: "Año", align: "right", cell: (row) => row.anio },
  {
    header: "Autorías",
    cell: (row) => (
      <ul className="space-y-0.5 text-xs text-muted-foreground">
        {row.autores.map((autor) => (
          <li key={autor.nombre}>
            {autor.nombre} — {autor.rolAutoria}
            {autor.esEstudiante ? " (estudiante)" : ""}
          </li>
        ))}
      </ul>
    ),
  },
  {
    header: "Enlace",
    cell: (row) =>
      row.doi || row.enlace ? (
        <a
          href={row.doi ? `https://doi.org/${row.doi}` : (row.enlace as string)}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1.5 text-accent underline-offset-4 hover:underline"
        >
          {row.doi ? "DOI" : "Repositorio"}
          <ExternalLink className="size-3.5" strokeWidth={1.75} aria-hidden />
        </a>
      ) : (
        <span className="text-muted-foreground">No disponible</span>
      ),
  },
];
