import { createFileRoute, redirect } from "@tanstack/react-router";
import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import {
  Campo,
  claseBotonPrimario,
  claseCampo,
} from "@/components/gestion/comunes";
import {
  catalogoSemilleroQuery,
  habilidadesQuery,
  esAdministrador,
  perfilEstudianteQuery,
  perfilProfesorQuery,
} from "@/lib/api/queries";
import { ApiError, apiRequest } from "@/lib/api/client";
import { perfilService, type ItemCatalogo } from "@/lib/api/services";
import type { UsuarioSesion } from "@/types/api";

export const Route = createFileRoute("/perfil")({
  ssr: false,
  head: () => ({
    meta: [
      { title: "Mi perfil | Agora UdeA" },
      {
        name: "description",
        content:
          "Actualiza tu programa académico, semestre, líneas de investigación o áreas de especialidad.",
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
    // El administrador no tiene perfil de estudiante ni de profesor
    if (esAdministrador(sesion)) {
      throw redirect({ to: "/" });
    }
    const { queryClient } = context;
    await Promise.all([
      queryClient.ensureQueryData(catalogoSemilleroQuery()),
      queryClient.ensureQueryData(habilidadesQuery()),
      sesion.idEstudiante !== null
        ? queryClient.ensureQueryData(
            perfilEstudianteQuery(sesion.idEstudiante),
          )
        : null,
      sesion.idProfesor !== null
        ? queryClient.ensureQueryData(perfilProfesorQuery(sesion.idProfesor))
        : null,
    ]);
    return sesion;
  },
  component: PerfilPage,
});

function PerfilPage() {
  const sesion = Route.useLoaderData();

  return (
    <AppShell>
      <PageHeader
        eyebrow="Mi cuenta"
        title={sesion.nombreCompleto}
        description={`${sesion.email} · Mantén tu perfil al día: el motor de recomendación y la priorización de candidatos usan estos datos.`}
      />
      {sesion.idEstudiante !== null ? (
        <PerfilEstudiante
          idEstudiante={sesion.idEstudiante}
          idUsuario={sesion.idUsuario}
        />
      ) : sesion.idProfesor !== null ? (
        <PerfilProfesor
          idProfesor={sesion.idProfesor}
          idUsuario={sesion.idUsuario}
        />
      ) : null}
    </AppShell>
  );
}

function PerfilEstudiante({
  idEstudiante,
  idUsuario,
}: {
  idEstudiante: number;
  idUsuario: number;
}) {
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { data: catalogoHabilidades } = useSuspenseQuery(habilidadesQuery());
  const { data: perfil } = useSuspenseQuery(
    perfilEstudianteQuery(idEstudiante),
  );
  const queryClient = useQueryClient();

  // El backend devuelve nombres; el formulario trabaja con los IDs del catálogo
  const [idPrograma, setIdPrograma] = useState(
    String(
      catalogo.programas.find((p) => p.nombre === perfil.nombrePrograma)?.id ??
        "",
    ),
  );
  const [semestre, setSemestre] = useState(perfil.semestre);
  const [lineas, setLineas] = useState<number[]>(
    idsPorNombre(catalogo.lineas, perfil.lineasInvestigacion),
  );
  const [habilidades, setHabilidades] = useState<number[]>(
    idsPorNombre(catalogoHabilidades, perfil.habilidades),
  );
  const [guardando, setGuardando] = useState(false);

  const valido =
    idPrograma !== "" &&
    semestre >= 1 &&
    lineas.length > 0 &&
    habilidades.length > 0;

  async function guardar(event: React.FormEvent) {
    event.preventDefault();
    if (!valido) return;
    setGuardando(true);
    try {
      await perfilService.actualizarEstudiante(idEstudiante, {
        idUsuario,
        idPrograma: Number(idPrograma),
        semestre,
        idLineasInvestigacion: lineas,
        idHabilidades: habilidades,
      });
      toast.success("Perfil actualizado.");
      // Las recomendaciones dependen del programa, las líneas y las habilidades del estudiante
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["perfil"] }),
        queryClient.invalidateQueries({ queryKey: ["matching"] }),
        queryClient.invalidateQueries({ queryKey: ["estudiantes"] }),
      ]);
    } catch (error) {
      toast.error(
        error instanceof Error && error.message
          ? error.message
          : "No se pudo actualizar el perfil.",
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} className="panel max-w-3xl space-y-5 p-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo etiqueta="Programa académico" htmlFor="programa">
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
        </Campo>
        <Campo
          etiqueta="Semestre"
          htmlFor="semestre"
          ayuda="Actualízalo al iniciar cada periodo académico."
        >
          <input
            id="semestre"
            type="number"
            min={1}
            max={20}
            value={semestre}
            onChange={(e) => setSemestre(Number(e.target.value))}
            className={claseCampo}
            required
          />
        </Campo>
      </div>
      <SelectorChips
        leyenda="Líneas de investigación de interés (mínimo 1)"
        opciones={catalogo.lineas}
        seleccionadas={lineas}
        onCambio={setLineas}
      />
      <SelectorChips
        leyenda="Habilidades (mínimo 1)"
        opciones={catalogoHabilidades}
        seleccionadas={habilidades}
        onCambio={setHabilidades}
      />
      {perfil.habilidades.length === 0 && (
        <p className="text-xs text-warning">
          Aún no registras habilidades: elígelas para que las recomendaciones de
          semilleros reflejen tu perfil.
        </p>
      )}
      <button
        type="submit"
        disabled={!valido || guardando}
        className={claseBotonPrimario}
      >
        {guardando ? "Guardando…" : "Guardar cambios"}
      </button>
    </form>
  );
}

function PerfilProfesor({
  idProfesor,
  idUsuario,
}: {
  idProfesor: number;
  idUsuario: number;
}) {
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { data: perfil } = useSuspenseQuery(perfilProfesorQuery(idProfesor));
  const queryClient = useQueryClient();

  const [idPrograma, setIdPrograma] = useState(
    perfil.idPrograma !== null ? String(perfil.idPrograma) : "",
  );
  const [areas, setAreas] = useState<number[]>(
    idsPorNombre(catalogo.areas, perfil.areasEspecialidad),
  );
  const [guardando, setGuardando] = useState(false);

  const valido = idPrograma !== "" && areas.length > 0;

  async function guardar(event: React.FormEvent) {
    event.preventDefault();
    if (!valido) return;
    setGuardando(true);
    try {
      await perfilService.actualizarProfesor(idProfesor, {
        idUsuario,
        idPrograma: Number(idPrograma),
        idAreasEspecialidad: areas,
      });
      toast.success("Perfil actualizado.");
      // Las áreas de los tutores alimentan el buscador de profesores y las recomendaciones
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["perfil"] }),
        queryClient.invalidateQueries({ queryKey: ["profesores"] }),
      ]);
    } catch (error) {
      toast.error(
        error instanceof Error && error.message
          ? error.message
          : "No se pudo actualizar el perfil.",
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} className="panel max-w-3xl space-y-5 p-6">
      <Campo etiqueta="Programa académico" htmlFor="programa">
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
      </Campo>
      <SelectorChips
        leyenda="Áreas de especialidad (mínimo 1)"
        opciones={catalogo.areas}
        seleccionadas={areas}
        onCambio={setAreas}
      />
      <button
        type="submit"
        disabled={!valido || guardando}
        className={claseBotonPrimario}
      >
        {guardando ? "Guardando…" : "Guardar cambios"}
      </button>
    </form>
  );
}

function SelectorChips({
  leyenda,
  opciones,
  seleccionadas,
  onCambio,
}: {
  leyenda: string;
  opciones: ItemCatalogo[];
  seleccionadas: number[];
  onCambio: (ids: number[]) => void;
}) {
  const alternar = (id: number) =>
    onCambio(
      seleccionadas.includes(id)
        ? seleccionadas.filter((actual) => actual !== id)
        : [...seleccionadas, id],
    );

  return (
    <fieldset>
      <legend className="label-caps mb-1.5">{leyenda}</legend>
      <div className="flex flex-wrap gap-2">
        {opciones.map((opcion) => {
          const activa = seleccionadas.includes(opcion.id);
          return (
            <button
              key={opcion.id}
              type="button"
              onClick={() => alternar(opcion.id)}
              aria-pressed={activa}
              className={
                activa
                  ? "rounded-sm border border-primary bg-primary px-2.5 py-1 text-sm text-primary-foreground"
                  : "rounded-sm border border-border bg-surface px-2.5 py-1 text-sm text-foreground hover:bg-secondary"
              }
            >
              {opcion.nombre}
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}

function idsPorNombre(catalogo: ItemCatalogo[], nombres: string[]) {
  return catalogo
    .filter((item) => nombres.includes(item.nombre))
    .map((i) => i.id);
}
