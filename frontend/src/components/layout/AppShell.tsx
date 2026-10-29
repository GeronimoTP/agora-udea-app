import { Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  BarChart3,
  FileText,
  LayoutDashboard,
  Library,
  LogOut,
  Menu,
  Settings2,
  Target,
  UserRound,
  X,
  type LucideIcon,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";
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
  // Menú lateral desplegable en pantallas pequeñas; fijo desde lg
  const [menuAbierto, setMenuAbierto] = useState(false);
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

  const claseItem =
    "flex items-center gap-3 rounded-sm border-l-2 border-transparent px-3 py-2.5 text-sm text-sidebar-foreground/85 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground";
  const claseItemActivo =
    "border-l-2 border-accent bg-sidebar-accent font-semibold text-sidebar-accent-foreground";
  const cerrarMenu = () => setMenuAbierto(false);

  return (
    <div className="min-h-screen bg-background lg:flex">
      {/* Barra superior solo en pantallas pequeñas: abre el menú lateral */}
      <header className="sticky top-0 z-30 flex items-center justify-between bg-sidebar px-4 py-3 text-sidebar-foreground lg:hidden">
        <Link
          to="/"
          className="font-display text-lg font-semibold tracking-tight"
        >
          Agora UdeA
        </Link>
        <button
          type="button"
          onClick={() => setMenuAbierto((abierto) => !abierto)}
          aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={menuAbierto}
          className="rounded-sm p-2 hover:bg-sidebar-accent"
        >
          {menuAbierto ? (
            <X className="size-5" strokeWidth={1.75} aria-hidden />
          ) : (
            <Menu className="size-5" strokeWidth={1.75} aria-hidden />
          )}
        </button>
      </header>

      {menuAbierto && (
        <div
          className="fixed inset-0 z-30 bg-black/40 lg:hidden"
          onClick={cerrarMenu}
          aria-hidden
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 flex-col bg-sidebar text-sidebar-foreground transition-transform duration-200 lg:sticky lg:top-0 lg:h-screen lg:translate-x-0",
          menuAbierto ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <Link
          to="/"
          onClick={cerrarMenu}
          className="block border-b border-sidebar-border px-6 py-5"
        >
          <span className="block font-display text-xl font-semibold tracking-tight text-sidebar-primary-foreground">
            Agora UdeA
          </span>
          <span className="mt-1 block text-xs leading-snug text-sidebar-foreground/75">
            Sistema de Gestión de Semilleros de Investigación
          </span>
        </Link>

        <nav
          aria-label="Navegación principal"
          className="flex-1 overflow-y-auto px-3 py-4"
        >
          <ul className="space-y-1">
            {items.map(({ to, label, icon: Icon }) => (
              <li key={to}>
                <Link
                  to={to}
                  onClick={cerrarMenu}
                  activeOptions={{ exact: to === "/" }}
                  className={claseItem}
                  activeProps={{ className: claseItemActivo }}
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
                  onClick={cerrarMenu}
                  className={claseItem}
                  activeProps={{ className: claseItemActivo }}
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

        <div className="border-t border-sidebar-border px-3 py-4">
          <div className="px-3 pb-3">
            <p className="truncate text-sm font-medium text-sidebar-primary-foreground">
              {sesion?.nombreCompleto ?? ""}
            </p>
            <p className="truncate text-xs text-sidebar-foreground/75">
              {sesion?.rol ?? ""}
            </p>
          </div>
          {/* El administrador no tiene perfil de estudiante ni de profesor */}
          {(perfil === "ESTUDIANTE" || perfil === "PROFESOR") && (
            <Link
              to="/perfil"
              onClick={cerrarMenu}
              className={claseItem}
              activeProps={{ className: claseItemActivo }}
            >
              <UserRound className="size-4" strokeWidth={1.75} aria-hidden />
              Mi perfil
            </Link>
          )}
          <button
            type="button"
            onClick={cerrarSesion}
            className={cn(claseItem, "w-full")}
          >
            <LogOut className="size-4" strokeWidth={1.75} aria-hidden />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <main className="mx-auto w-full max-w-[1600px] flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          {children}
        </main>

        <footer className="border-t border-border bg-surface">
          <div className="mx-auto w-full max-w-[1600px] px-4 py-5 text-xs text-muted-foreground sm:px-6 lg:px-10">
            Universidad de Antioquia · Vicerrectoría de Investigación ·
            Plataforma Agora UdeA
          </div>
        </footer>
      </div>
    </div>
  );
}
