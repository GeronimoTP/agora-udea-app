import { Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  BarChart3,
  FileText,
  LayoutDashboard,
  Library,
  LogOut,
  Settings2,
  Target,
  UserRound,
  type LucideIcon,
} from "lucide-react";
import type { ReactNode } from "react";
import {
  tipoPerfil,
  usuarioSesionQuery,
  type TipoPerfil,
} from "@/lib/api/queries";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  perfiles: TipoPerfil[];
}

const navItems: NavItem[] = [
  {
    to: "/",
    label: "Tablero",
    icon: LayoutDashboard,
    perfiles: ["ESTUDIANTE", "PROFESOR"],
  },
  {
    to: "/semilleros",
    label: "Semilleros",
    icon: Library,
    perfiles: ["ESTUDIANTE", "PROFESOR", "ADMINISTRADOR"],
  },
  {
    to: "/postulaciones",
    label: "Postulaciones",
    icon: FileText,
    perfiles: ["ESTUDIANTE", "PROFESOR"],
  },
  {
    to: "/compatibilidad",
    label: "Compatibilidad",
    icon: Target,
    perfiles: ["ESTUDIANTE"],
  },
  {
    to: "/analitica",
    label: "Analítica",
    icon: BarChart3,
    perfiles: ["ADMINISTRADOR"],
  },
];

export function AppShell({ children }: { children: ReactNode }) {
  const { data: sesion } = useQuery(usuarioSesionQuery());
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const perfil = tipoPerfil(sesion);
  const items = perfil
    ? navItems.filter(({ perfiles }) => perfiles.includes(perfil))
    : [];
  // Acceso directo a la gestión cuando el usuario lidera exactamente un semillero;
  // con varios, se entra desde "Semilleros que lideras" en el tablero
  const liderados = sesion?.idSemillerosLiderados ?? [];
  const gestion = liderados.length === 1 ? liderados[0] : null;

  const cerrarSesion = () => {
    window.localStorage.removeItem("access_token");
    // Sin esto, el siguiente usuario vería en caché los datos del anterior
    queryClient.clear();
    navigate({ to: "/auth/login", replace: true });
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-primary text-primary-foreground">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <Link to="/" className="flex items-baseline gap-3">
            <span className="font-display text-lg font-semibold tracking-tight">
              Agora UdeA
            </span>
            <span className="hidden border-l border-sidebar-border pl-3 text-xs text-sidebar-foreground/80 sm:block">
              Sistema de Gestión de Semilleros de Investigación
            </span>
          </Link>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-medium">
                {sesion?.nombreCompleto ?? ""}
              </p>
              <p className="text-xs text-sidebar-foreground/75">
                {sesion?.rol ?? ""}
              </p>
            </div>
            {/* El administrador no tiene perfil de estudiante ni de profesor */}
            {(perfil === "ESTUDIANTE" || perfil === "PROFESOR") && (
              <Link
                to="/perfil"
                title="Mi perfil"
                className="flex items-center gap-2 rounded-sm border border-sidebar-border px-3 py-2 text-xs text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                activeProps={{
                  className: "bg-sidebar-accent text-sidebar-accent-foreground",
                }}
              >
                <UserRound className="size-4" strokeWidth={1.75} aria-hidden />
                <span className="hidden sm:inline">Mi perfil</span>
              </Link>
            )}
            <button
              type="button"
              onClick={cerrarSesion}
              title="Cerrar sesión"
              className="flex items-center gap-2 rounded-sm border border-sidebar-border px-3 py-2 text-xs text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            >
              <LogOut className="size-4" strokeWidth={1.75} aria-hidden />
              <span className="hidden sm:inline">Cerrar sesión</span>
            </button>
          </div>
        </div>
        <nav className="border-t border-sidebar-border bg-sidebar">
          <ul className="mx-auto flex max-w-7xl gap-1 overflow-x-auto px-4">
            {items.map(({ to, label, icon: Icon }) => (
              <li key={to}>
                <Link
                  to={to}
                  activeOptions={{ exact: to === "/" }}
                  className="flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-4 py-3 text-sm text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                  activeProps={{
                    className:
                      "border-b-2 border-accent bg-sidebar-accent font-semibold text-sidebar-accent-foreground",
                  }}
                >
                  <Icon className="size-4" strokeWidth={1.75} aria-hidden />
                  {label}
                </Link>
              </li>
            ))}
            {gestion !== null && (
              <li>
                <Link
                  to="/gestion/$idSemillero"
                  params={{ idSemillero: String(gestion) }}
                  search={{ seccion: "informacion" }}
                  className="flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-4 py-3 text-sm text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                  activeProps={{
                    className:
                      "border-b-2 border-accent bg-sidebar-accent font-semibold text-sidebar-accent-foreground",
                  }}
                >
                  <Settings2
                    className="size-4"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                  Mi semillero
                </Link>
              </li>
            )}
          </ul>
        </nav>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">{children}</main>

      <footer className="mt-8 border-t border-border bg-surface">
        <div className="mx-auto max-w-7xl px-6 py-6 text-xs text-muted-foreground">
          Universidad de Antioquia · Vicerrectoría de Investigación · Plataforma
          Agora UdeA
        </div>
      </footer>
    </div>
  );
}
