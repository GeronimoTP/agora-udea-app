import { Link } from "@tanstack/react-router";
import {
  BarChart3,
  FileText,
  LayoutDashboard,
  Library,
  Target,
  type LucideIcon,
} from "lucide-react";
import type { ReactNode } from "react";
import { SESION_DEMO } from "@/lib/api/queries";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

const navItems: NavItem[] = [
  { to: "/", label: "Tablero", icon: LayoutDashboard },
  { to: "/semilleros", label: "Semilleros", icon: Library },
  { to: "/postulaciones", label: "Postulaciones", icon: FileText },
  { to: "/compatibilidad", label: "Compatibilidad", icon: Target },
  { to: "/analitica", label: "Analítica", icon: BarChart3 },
];

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <header className="bg-primary text-primary-foreground">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <Link to="/" className="flex items-baseline gap-3">
            <span className="font-display text-lg font-semibold tracking-tight">Agora UdeA</span>
            <span className="hidden border-l border-sidebar-border pl-3 text-xs text-sidebar-foreground/80 sm:block">
              Sistema de Gestión de Semilleros de Investigación
            </span>
          </Link>
          <div className="text-right">
            <p className="text-sm font-medium">{SESION_DEMO.nombre}</p>
            <p className="text-xs text-sidebar-foreground/75">
              {SESION_DEMO.programa} · {SESION_DEMO.rol}
            </p>
          </div>
        </div>
        <nav className="border-t border-sidebar-border bg-sidebar">
          <ul className="mx-auto flex max-w-7xl gap-1 overflow-x-auto px-4">
            {navItems.map(({ to, label, icon: Icon }) => (
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
          </ul>
        </nav>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">{children}</main>

      <footer className="mt-8 border-t border-border bg-surface">
        <div className="mx-auto max-w-7xl px-6 py-6 text-xs text-muted-foreground">
          Universidad de Antioquia · Vicerrectoría de Investigación · Plataforma Agora UdeA
        </div>
      </footer>
    </div>
  );
}
