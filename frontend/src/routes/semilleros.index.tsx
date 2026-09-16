import { createFileRoute } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useMemo, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { SemilleroCard } from "@/components/semilleros/SemilleroCard";
import { semillerosQuery } from "@/lib/api/queries";

export const Route = createFileRoute("/semilleros/")({
  head: () => ({
    meta: [
      { title: "Semilleros de investigación | Agora UdeA" },
      {
        name: "description",
        content:
          "Directorio de semilleros de investigación de la Universidad de Antioquia con filtros por facultad, línea de investigación y convocatorias abiertas.",
      },
      { property: "og:title", content: "Semilleros de investigación | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Explora los semilleros de la UdeA, sus líneas de investigación y las convocatorias con cupos disponibles.",
      },
    ],
  }),
  loader: async ({ context }) => {
    await context.queryClient.ensureQueryData(semillerosQuery());
  },
  component: SemillerosPage,
});

function SemillerosPage() {
  const { data: semilleros } = useSuspenseQuery(semillerosQuery());
  const [busqueda, setBusqueda] = useState("");
  const [facultad, setFacultad] = useState("TODAS");
  const [soloAbiertas, setSoloAbiertas] = useState(false);

  const facultades = useMemo(
    () => Array.from(new Set(semilleros.map((s) => s.facultad))).sort(),
    [semilleros],
  );

  const resultados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return semilleros.filter((s) => {
      const coincideTexto =
        texto === "" ||
        s.nombre.toLowerCase().includes(texto) ||
        s.descripcion.toLowerCase().includes(texto) ||
        s.lineasInvestigacion.some((l) => l.toLowerCase().includes(texto));
      const coincideFacultad = facultad === "TODAS" || s.facultad === facultad;
      const coincideConvocatoria = !soloAbiertas || s.convocatoriaAbierta;
      return coincideTexto && coincideFacultad && coincideConvocatoria;
    });
  }, [semilleros, busqueda, facultad, soloAbiertas]);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Investigación formativa"
        title="Semilleros de investigación"
        description="Consulta la información oficial de cada semillero, sus líneas de trabajo, su producción académica y las convocatorias de vinculación vigentes."
      />

      <form
        className="panel mb-6 grid gap-4 p-5 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_auto]"
        onSubmit={(event) => event.preventDefault()}
      >
        <div>
          <label htmlFor="busqueda" className="label-caps mb-1.5 block">
            Búsqueda
          </label>
          <div className="relative">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              strokeWidth={1.75}
              aria-hidden
            />
            <input
              id="busqueda"
              type="search"
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              placeholder="Nombre, descripción o línea de investigación"
              className="h-10 w-full rounded-sm border border-input bg-surface pl-9 pr-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-1 focus:ring-ring"
            />
          </div>
        </div>

        <div>
          <label htmlFor="facultad" className="label-caps mb-1.5 block">
            Facultad
          </label>
          <select
            id="facultad"
            value={facultad}
            onChange={(event) => setFacultad(event.target.value)}
            className="h-10 w-full rounded-sm border border-input bg-surface px-3 text-sm text-foreground outline-none focus:border-ring focus:ring-1 focus:ring-ring"
          >
            <option value="TODAS">Todas las facultades</option>
            {facultades.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-end">
          <label className="flex h-10 items-center gap-2 text-sm text-foreground">
            <input
              type="checkbox"
              checked={soloAbiertas}
              onChange={(event) => setSoloAbiertas(event.target.checked)}
              className="size-4 rounded-sm border-input accent-[var(--accent)]"
            />
            Solo con convocatoria abierta
          </label>
        </div>
      </form>

      <p className="mb-4 text-sm text-muted-foreground">
        {resultados.length} semillero{resultados.length === 1 ? "" : "s"} encontrado
        {resultados.length === 1 ? "" : "s"}
      </p>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {resultados.map((semillero) => (
          <SemilleroCard key={semillero.idSemillero} semillero={semillero} />
        ))}
      </div>

      {resultados.length === 0 ? (
        <div className="panel p-8 text-center text-sm text-muted-foreground">
          No se encontraron semilleros con los criterios seleccionados.
        </div>
      ) : null}
    </AppShell>
  );
}
