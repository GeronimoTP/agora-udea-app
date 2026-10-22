import {
  createFileRoute,
  Link,
  redirect,
  useNavigate,
} from "@tanstack/react-router";
import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BadgeEstado } from "@/components/gestion/comunes";
import { InformacionSemillero } from "@/components/gestion/InformacionSemillero";
import { ConvocatoriasGestion } from "@/components/gestion/ConvocatoriasGestion";
import { MiembrosGestion } from "@/components/gestion/MiembrosGestion";
import { ProyectosGestion } from "@/components/gestion/ProyectosGestion";
import { PublicacionesGestion } from "@/components/gestion/PublicacionesGestion";
import { ReconocimientosGestion } from "@/components/gestion/ReconocimientosGestion";
import { catalogoGestionQuery, gestionQueries } from "@/lib/api/gestion";
import {
  catalogoSemilleroQuery,
  profesoresQuery,
  usuarioSesionQuery,
} from "@/lib/api/queries";
import { ApiError, apiRequest } from "@/lib/api/client";
import type { UsuarioSesion } from "@/types/api";

const SECCIONES = [
  { valor: "informacion", etiqueta: "Información" },
  { valor: "convocatorias", etiqueta: "Convocatorias" },
  { valor: "miembros", etiqueta: "Integrantes y tutores" },
  { valor: "proyectos", etiqueta: "Proyectos" },
  { valor: "publicaciones", etiqueta: "Publicaciones" },
  { valor: "reconocimientos", etiqueta: "Reconocimientos" },
] as const;

type Seccion = (typeof SECCIONES)[number]["valor"];

export const Route = createFileRoute("/gestion/$idSemillero")({
  ssr: false,
  validateSearch: (search: Record<string, unknown>): { seccion: Seccion } => ({
    seccion: SECCIONES.some((s) => s.valor === search["seccion"])
      ? (search["seccion"] as Seccion)
      : "informacion",
  }),
  head: () => ({
    meta: [
      { title: "Gestión del semillero | Agora UdeA" },
      {
        name: "description",
        content:
          "Gestión del semillero: datos, convocatorias, integrantes, tutores, proyectos, publicaciones y reconocimientos.",
      },
    ],
  }),
  loader: async ({ context, params }) => {
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
    const id = Number(params.idSemillero);
    // Solo un líder del semillero (estudiante líder o profesor tutor) entra a la gestión;
    // el backend valida lo mismo en cada acción
    if (!sesion.idSemillerosLiderados.includes(id)) {
      throw redirect({ to: "/" });
    }
    const { queryClient } = context;
    await Promise.all([
      queryClient.ensureQueryData(gestionQueries.semillero(id)),
      queryClient.ensureQueryData(gestionQueries.lineas(id)),
      queryClient.ensureQueryData(gestionQueries.integrantes(id)),
      queryClient.ensureQueryData(gestionQueries.tutores(id)),
      queryClient.ensureQueryData(gestionQueries.convocatorias(id)),
      queryClient.ensureQueryData(gestionQueries.proyectos(id)),
      queryClient.ensureQueryData(gestionQueries.publicaciones(id)),
      queryClient.ensureQueryData(gestionQueries.reconocimientos(id)),
      queryClient.ensureQueryData(catalogoGestionQuery()),
      queryClient.ensureQueryData(catalogoSemilleroQuery()),
      queryClient.ensureQueryData(profesoresQuery()),
    ]);
  },
  component: GestionSemilleroPage,
});

function GestionSemilleroPage() {
  const { idSemillero } = Route.useParams();
  const { seccion } = Route.useSearch();
  const id = Number(idSemillero);
  const { data: semillero } = useSuspenseQuery(gestionQueries.semillero(id));
  const navigate = useNavigate({ from: Route.fullPath });
  const queryClient = useQueryClient();

  // Tras transferir el liderazgo o eliminar el semillero, la sesión cambia: si el usuario
  // ya no lidera este semillero (p. ej. el estudiante líder saliente) vuelve al tablero
  const alPerderLiderazgo = async (esEliminacion: boolean) => {
    await queryClient.invalidateQueries({ queryKey: ["sesion"] });
    const sesion = await queryClient.fetchQuery(usuarioSesionQuery());
    if (esEliminacion || !sesion.idSemillerosLiderados.includes(id)) {
      navigate({ to: "/" });
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
        eyebrow={`Gestión del semillero · ${semillero.codigoIdentificador}`}
        title={semillero.nombre}
        description={`${semillero.nombrePrograma}${semillero.nombreFacultad ? ` · ${semillero.nombreFacultad}` : ""}`}
        actions={
          <div className="flex items-center gap-3">
            <BadgeEstado nombre={semillero.estadoActual} />
            <Link
              to="/semilleros/$idSemillero"
              params={{ idSemillero }}
              className="text-sm font-medium text-accent underline-offset-4 hover:underline"
            >
              Ver página pública
            </Link>
          </div>
        }
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
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="informacion">
          <InformacionSemillero
            idSemillero={id}
            onLiderazgoPerdido={alPerderLiderazgo}
          />
        </TabsContent>
        <TabsContent value="convocatorias">
          <ConvocatoriasGestion idSemillero={id} />
        </TabsContent>
        <TabsContent value="miembros">
          <MiembrosGestion idSemillero={id} />
        </TabsContent>
        <TabsContent value="proyectos">
          <ProyectosGestion idSemillero={id} />
        </TabsContent>
        <TabsContent value="publicaciones">
          <PublicacionesGestion idSemillero={id} />
        </TabsContent>
        <TabsContent value="reconocimientos">
          <ReconocimientosGestion idSemillero={id} />
        </TabsContent>
      </Tabs>
    </AppShell>
  );
}
