import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ElementosCatalogoAdmin } from "@/components/solicitudes/ElementosCatalogoAdmin";
import { SemillerosPorAprobar } from "@/components/solicitudes/SemillerosPorAprobar";
import { SolicitudesCatalogoAdmin } from "@/components/solicitudes/SolicitudesCatalogoAdmin";
import { catalogoSemilleroQuery, esAdministrador } from "@/lib/api/queries";
import { requerirSesion } from "@/lib/api/sesion";
import { solicitudesQueries } from "@/lib/api/solicitudes";

const SECCIONES = [
  { valor: "semilleros", etiqueta: "Semilleros por aprobar" },
  { valor: "solicitudes", etiqueta: "Solicitudes de catálogo" },
  { valor: "catalogo", etiqueta: "Catálogo" },
] as const;

type Seccion = (typeof SECCIONES)[number]["valor"];

export const Route = createFileRoute("/administracion")({
  ssr: false,
  validateSearch: (search: Record<string, unknown>): { seccion: Seccion } => ({
    seccion: SECCIONES.some((s) => s.valor === search["seccion"])
      ? (search["seccion"] as Seccion)
      : "semilleros",
  }),
  head: () => ({
    meta: [
      { title: "Administración | Agora UdeA" },
      {
        name: "description",
        content:
          "Aprobación de semilleros y solicitudes de catálogo de la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context }) => {
    const sesion = await requerirSesion();
    if (!esAdministrador(sesion)) {
      throw redirect({ to: "/" });
    }
    await Promise.all([
      context.queryClient.ensureQueryData(
        solicitudesQueries.semillerosPorAprobar(),
      ),
      context.queryClient.ensureQueryData(solicitudesQueries.catalogo(true)),
      context.queryClient.ensureQueryData(catalogoSemilleroQuery()),
    ]);
  },
  component: AdministracionPage,
});

function AdministracionPage() {
  const { seccion } = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data: semilleros } = useSuspenseQuery(
    solicitudesQueries.semillerosPorAprobar(),
  );
  const { data: catalogo } = useSuspenseQuery(
    solicitudesQueries.catalogo(true),
  );
  const contador: Record<Seccion, number> = {
    semilleros: semilleros.length,
    solicitudes: catalogo.length,
    catalogo: 0,
  };

  return (
    <AppShell>
      <PageHeader
        eyebrow="Administración"
        title="Aprobaciones y catálogo"
        description="Decide qué semilleros nuevos se aprueban y qué elementos propuestos por estudiantes y profesores entran al catálogo."
      />
      <Tabs
        value={seccion}
        onValueChange={(valor) =>
          navigate({ search: { seccion: valor as Seccion }, replace: true })
        }
      >
        <TabsList className="mb-6 h-auto flex-wrap justify-start">
          {SECCIONES.map((s) => (
            <TabsTrigger key={s.valor} value={s.valor}>
              {s.etiqueta}
              {contador[s.valor] > 0 && (
                <span className="ml-2 rounded-full bg-accent px-1.5 text-xs font-semibold text-accent-foreground">
                  {contador[s.valor]}
                </span>
              )}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="semilleros">
          <SemillerosPorAprobar />
        </TabsContent>
        <TabsContent value="solicitudes">
          <SolicitudesCatalogoAdmin />
        </TabsContent>
        <TabsContent value="catalogo">
          <ElementosCatalogoAdmin />
        </TabsContent>
      </Tabs>
    </AppShell>
  );
}
