import { createFileRoute, Link } from "@tanstack/react-router";
import {
  useQuery,
  useQueryClient,
  useSuspenseQuery,
} from "@tanstack/react-query";
import { ArrowLeft, ExternalLink } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { DataTable, type Columna } from "@/components/common/DataTable";
import {
  convocatoriasSemilleroQuery,
  habilidadesQuery,
  usuarioSesionQuery,
  proyectosQuery,
  publicacionesQuery,
  semilleroQuery,
} from "@/lib/api/queries";
import { postulacionesService } from "@/lib/api/services";
import type { Convocatoria, Proyecto, Publicacion } from "@/types/api";

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
  const { data: convocatorias } = useSuspenseQuery(convocatoriasSemilleroQuery(id));
  const { data: proyectos } = useSuspenseQuery(proyectosQuery(id));
  const { data: publicaciones } = useSuspenseQuery(publicacionesQuery(id));
  // Solo los estudiantes pueden postularse a convocatorias
  const { data: sesion } = useQuery(usuarioSesionQuery());
  const idEstudiante = sesion?.idEstudiante ?? null;
  // Un estudiante puede integrar varios semilleros, pero no postularse al que ya integra o lidera
  const esIntegrante = sesion?.idSemillerosIntegrante.includes(id) ?? false;
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
        <h2 id="proyectos" className="mb-3 font-display text-sm font-semibold text-foreground">
          Proyectos de investigación
        </h2>
        <DataTable<Proyecto>
          rows={proyectos}
          getRowKey={(row) => row.idProyecto}
          columns={columnasProyectos}
        />
      </section>

      <section aria-labelledby="publicaciones">
        <h2 id="publicaciones" className="mb-3 font-display text-sm font-semibold text-foreground">
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
  const [enviando, setEnviando] = useState(false);
  const [abierto, setAbierto] = useState(false);
  const { data: habilidades = [] } = useQuery({
    ...habilidadesQuery(),
    enabled: abierto,
  });
  const abiertaConCupos =
    convocatoria.estado === "ABIERTA" && convocatoria.cuposDisponibles > 0;
  const disponible =
    idEstudiante !== null && abiertaConCupos && motivoSinPostulacion === null;
  const formularioValido =
    motivacion.trim().length >= 20 &&
    experiencia.trim().length > 0 &&
    horas >= 2 &&
    idHabilidades.length > 0;

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
      });
      toast.success("Postulación registrada correctamente.");
      await queryClient.invalidateQueries({ queryKey: ["postulaciones"] });
      setAbierto(false);
      setMotivacion("");
      setExperiencia("");
      setIdHabilidades([]);
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
          <dd className="mt-1">{formatearFecha(convocatoria.fechaApertura)}</dd>
        </div>
        <div>
          <dt className="label-caps">Cierre</dt>
          <dd className="mt-1">{formatearFecha(convocatoria.fechaCierre)}</dd>
        </div>
        <div>
          <dt className="label-caps">Cupos disponibles</dt>
          <dd className="mt-1 tabular-nums">
            {convocatoria.cuposDisponibles} de {convocatoria.cuposTotales}
          </dd>
        </div>
      </dl>

      {convocatoria.habilidadesRequeridas.length > 0 && (
        <div className="mt-4">
          <p className="label-caps mb-1.5">Habilidades requeridas</p>
          <ul className="flex flex-wrap gap-1.5">
            {convocatoria.habilidadesRequeridas.map((habilidad) => (
              <li
                key={habilidad}
                className="rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
              >
                {habilidad}
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
  { header: "Proyecto", cell: (row) => <span className="font-medium">{row.titulo}</span> },
  { header: "Estado", cell: (row) => <EstadoBadge estado={row.estado} /> },
  { header: "Inicio", cell: (row) => formatearFecha(row.fechaInicio) },
  { header: "Finalización", cell: (row) => formatearFecha(row.fechaFin) },
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
  { header: "Título", cell: (row) => <span className="font-medium">{row.titulo}</span> },
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

function formatearFecha(valor?: string | null) {
  if (!valor) return "En curso";
  return new Date(valor).toLocaleDateString("es-CO", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
