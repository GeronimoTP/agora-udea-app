import {
  createFileRoute,
  Link,
  redirect,
  useNavigate,
} from "@tanstack/react-router";
import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { ArrowLeft, X } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { BuscadorProfesores } from "@/components/gestion/BuscadorProfesores";
import { claseCampo } from "@/components/gestion/comunes";
import {
  catalogoSemilleroQuery,
  esEstudiante,
  esEstudianteLider,
  profesoresQuery,
} from "@/lib/api/queries";
import { ApiError, apiRequest } from "@/lib/api/client";
import { semillerosService, type Profesor } from "@/lib/api/services";
import type { UsuarioSesion } from "@/types/api";

export const Route = createFileRoute("/semilleros/nuevo")({
  ssr: false,
  head: () => ({
    meta: [
      { title: "Registrar semillero | Agora UdeA" },
      {
        name: "description",
        content:
          "Registro de un semillero de investigación avalado por la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context }) => {
    if (!localStorage.getItem("access_token")) {
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
    // Solo un estudiante que todavía no lidera un semillero puede registrar uno
    if (!esEstudiante(sesion) || esEstudianteLider(sesion)) {
      throw redirect({ to: "/" });
    }
    const [estudiante] = await Promise.all([
      apiRequest<{ nombrePrograma: string | null }>(
        `/api/estudiantes/${sesion.idEstudiante}`,
      ),
      context.queryClient.ensureQueryData(catalogoSemilleroQuery()),
      context.queryClient.ensureQueryData(profesoresQuery()),
    ]);
    return { sesion, programaEstudiante: estudiante.nombrePrograma };
  },
  component: NuevoSemilleroPage,
});

function NuevoSemilleroPage() {
  const { sesion, programaEstudiante } = Route.useLoaderData();
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { data: profesores } = useSuspenseQuery(profesoresQuery());
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const programaInicial = catalogo.programas.find(
    (p) => p.nombre === programaEstudiante,
  );

  const [codigo, setCodigo] = useState("");
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [idPrograma, setIdPrograma] = useState(
    programaInicial ? String(programaInicial.id) : "",
  );
  const [lineas, setLineas] = useState<number[]>([]);
  const [seleccionados, setSeleccionados] = useState<Profesor[]>([]);
  const [enviando, setEnviando] = useState(false);

  const estaSeleccionado = (id: number) =>
    seleccionados.some((p) => p.id === id);

  const alternarProfesor = (profesor: Profesor) =>
    setSeleccionados((actuales) =>
      actuales.some((p) => p.id === profesor.id)
        ? actuales.filter((p) => p.id !== profesor.id)
        : [...actuales, profesor],
    );

  const alternarLinea = (id: number) =>
    setLineas((actuales) =>
      actuales.includes(id)
        ? actuales.filter((l) => l !== id)
        : [...actuales, id],
    );

  const codigoValido = /^[A-Z0-9-]+$/.test(codigo);
  const nombreValido = nombre.trim().length >= 5 && nombre.trim().length <= 150;
  const formularioCompleto =
    codigoValido &&
    nombreValido &&
    descripcion.trim().length > 0 &&
    idPrograma !== "" &&
    lineas.length > 0 &&
    seleccionados.length > 0;

  const enviar = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!formularioCompleto || sesion.idEstudiante === null) return;
    setEnviando(true);
    try {
      const creado = await semillerosService.crear({
        codigoIdentificador: codigo,
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        idEstudianteLider: sesion.idEstudiante,
        idPrograma: Number(idPrograma),
        idLineasInvestigacion: lineas,
        idProfesores: seleccionados.map((p) => p.id),
      });
      toast.success(
        `Semillero "${creado.nombre}" registrado. Ahora eres su líder.`,
      );
      // La sesión cambia: ahora tiene un semillero liderado y el tablero pasa a vista de líder
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sesion"] }),
        queryClient.invalidateQueries({ queryKey: ["semilleros"] }),
      ]);
      navigate({ to: "/" });
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "No se pudo registrar el semillero.",
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <AppShell>
      <Link
        to="/"
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-accent underline-offset-4 hover:underline"
      >
        <ArrowLeft className="size-3.5" strokeWidth={1.75} aria-hidden />
        Volver al tablero
      </Link>

      <PageHeader
        eyebrow="Registro de semillero"
        title="Registrar un semillero de investigación"
        description="Registra el semillero con los datos de su aval institucional. Al crearlo quedas como estudiante líder y el semillero debe tener al menos un profesor tutor registrado en Agora UdeA."
      />

      <form onSubmit={enviar} className="grid gap-8 lg:grid-cols-2">
        <section aria-labelledby="datos" className="panel space-y-4 p-6">
          <h2
            id="datos"
            className="font-display text-sm font-semibold text-foreground"
          >
            Datos del semillero
          </h2>

          <div>
            <label htmlFor="codigo" className="label-caps mb-1.5 block">
              Código identificador
            </label>
            <input
              id="codigo"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.toUpperCase())}
              placeholder="SEM-ING-001"
              className={claseCampo}
              required
            />
            {codigo && !codigoValido && (
              <p className="mt-1 text-xs text-destructive">
                Solo letras mayúsculas, números y guiones.
              </p>
            )}
          </div>

          <div>
            <label htmlFor="nombre" className="label-caps mb-1.5 block">
              Nombre
            </label>
            <input
              id="nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              maxLength={150}
              className={claseCampo}
              required
            />
            {nombre && !nombreValido && (
              <p className="mt-1 text-xs text-destructive">
                Entre 5 y 150 caracteres.
              </p>
            )}
          </div>

          <div>
            <label htmlFor="descripcion" className="label-caps mb-1.5 block">
              Descripción
            </label>
            <textarea
              id="descripcion"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              rows={4}
              className={claseCampo}
              required
            />
          </div>

          <div>
            <label htmlFor="programa" className="label-caps mb-1.5 block">
              Programa académico
            </label>
            <select
              id="programa"
              value={idPrograma}
              onChange={(e) => setIdPrograma(e.target.value)}
              className={claseCampo}
              required
            >
              <option value="">Selecciona un programa</option>
              {catalogo.programas.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </div>

          <fieldset>
            <legend className="label-caps mb-1.5">
              Líneas de investigación (mínimo 1)
            </legend>
            <div className="flex flex-wrap gap-2">
              {catalogo.lineas.map((linea) => {
                const activa = lineas.includes(linea.id);
                return (
                  <button
                    key={linea.id}
                    type="button"
                    onClick={() => alternarLinea(linea.id)}
                    aria-pressed={activa}
                    className={
                      activa
                        ? "rounded-sm border border-primary bg-primary px-2.5 py-1 text-sm text-primary-foreground"
                        : "rounded-sm border border-border bg-surface px-2.5 py-1 text-sm text-foreground hover:bg-secondary"
                    }
                  >
                    {linea.nombre}
                  </button>
                );
              })}
            </div>
          </fieldset>
        </section>

        <section aria-labelledby="tutores" className="panel space-y-4 p-6">
          <div>
            <h2
              id="tutores"
              className="font-display text-sm font-semibold text-foreground"
            >
              Profesores tutores
            </h2>
            <p className="mt-1 text-xs text-muted-foreground">
              Selecciona uno o varios profesores. Si un profesor no aparece,
              debe registrarse primero en Agora UdeA.
            </p>
          </div>

          <div>
            <p className="label-caps mb-1.5">
              Seleccionados ({seleccionados.length})
            </p>
            {seleccionados.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Aún no has seleccionado profesores.
              </p>
            ) : (
              <ul className="flex flex-wrap gap-2">
                {seleccionados.map((p) => (
                  <li key={p.id}>
                    <button
                      type="button"
                      onClick={() => alternarProfesor(p)}
                      className="inline-flex items-center gap-1.5 rounded-sm border border-primary bg-primary/10 px-2.5 py-1 text-sm text-foreground hover:bg-primary/20"
                      aria-label={`Quitar a ${p.nombreCompleto}`}
                    >
                      {p.nombreCompleto}
                      <X className="size-3.5" strokeWidth={1.75} aria-hidden />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <BuscadorProfesores
            profesores={profesores}
            programas={catalogo.programas}
            facultades={catalogo.facultades}
            areas={catalogo.areas}
            renderAccion={(p) => (
              <input
                type="checkbox"
                checked={estaSeleccionado(p.id)}
                onChange={() => alternarProfesor(p)}
                aria-label={`Seleccionar a ${p.nombreCompleto}`}
                className="mt-1 size-4"
              />
            )}
          />
        </section>

        <div className="flex flex-wrap items-center gap-3 lg:col-span-2">
          <button
            type="submit"
            disabled={!formularioCompleto || enviando}
            className="rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover disabled:opacity-50"
          >
            {enviando ? "Registrando…" : "Registrar semillero"}
          </button>
          {!formularioCompleto && (
            <p className="text-xs text-muted-foreground">
              Completa los datos, al menos una línea de investigación y un
              profesor tutor.
            </p>
          )}
        </div>
      </form>
    </AppShell>
  );
}
