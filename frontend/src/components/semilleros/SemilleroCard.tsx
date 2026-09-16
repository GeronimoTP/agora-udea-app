import { Link } from "@tanstack/react-router";
import { ArrowRight, FileText, FlaskConical, Users } from "lucide-react";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import type { Semillero } from "@/types/api";

export function SemilleroCard({ semillero }: { semillero: Semillero }) {
  return (
    <article className="panel flex flex-col p-5">
      <div className="flex items-start justify-between gap-3">
        <p className="label-caps">{semillero.facultad}</p>
        <EstadoBadge estado={semillero.estado} />
      </div>

      <h3 className="mt-3 font-display text-base font-semibold leading-snug text-foreground">
        {semillero.nombre}
      </h3>
      <p className="mt-2 line-clamp-3 text-sm leading-relaxed text-muted-foreground">
        {semillero.descripcion}
      </p>

      <ul className="mt-3 flex flex-wrap gap-1.5">
        {semillero.lineasInvestigacion.map((linea) => (
          <li
            key={linea}
            className="rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
          >
            {linea}
          </li>
        ))}
      </ul>

      <dl className="mt-4 grid grid-cols-3 gap-3 border-t border-border pt-4 text-sm">
        <div>
          <dt className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Users className="size-3.5" strokeWidth={1.75} aria-hidden />
            Integrantes
          </dt>
          <dd className="mt-1 font-medium tabular-nums">{semillero.totalIntegrantes}</dd>
        </div>
        <div>
          <dt className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <FlaskConical className="size-3.5" strokeWidth={1.75} aria-hidden />
            Proyectos
          </dt>
          <dd className="mt-1 font-medium tabular-nums">{semillero.totalProyectos}</dd>
        </div>
        <div>
          <dt className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <FileText className="size-3.5" strokeWidth={1.75} aria-hidden />
            Publicaciones
          </dt>
          <dd className="mt-1 font-medium tabular-nums">{semillero.totalPublicaciones}</dd>
        </div>
      </dl>

      <div className="mt-5 flex items-center justify-between gap-3">
        {semillero.convocatoriaAbierta ? (
          <EstadoBadge estado="ABIERTA" />
        ) : (
          <span className="text-xs text-muted-foreground">Sin convocatoria vigente</span>
        )}
        <Link
          to="/semilleros/$idSemillero"
          params={{ idSemillero: String(semillero.idSemillero) }}
          className="inline-flex items-center gap-1.5 rounded-sm border border-border-strong px-3 py-1.5 text-sm font-medium text-primary transition-colors hover:bg-secondary"
        >
          Ver detalle
          <ArrowRight className="size-3.5" strokeWidth={1.75} aria-hidden />
        </Link>
      </div>
    </article>
  );
}
