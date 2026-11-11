import type { ReactNode } from "react";
import { BadgeEstado } from "@/components/gestion/comunes";
import type { SolicitudCatalogo } from "@/lib/api/solicitudes";
import { formatearFecha } from "@/lib/formato";

/** Datos de una solicitud de catálogo; las acciones las pone quien la muestra. */
export function TarjetaSolicitudCatalogo({
  solicitud,
  mostrarSolicitante = false,
  children,
}: {
  solicitud: SolicitudCatalogo;
  mostrarSolicitante?: boolean;
  children?: ReactNode;
}) {
  return (
    <li className="panel p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="max-w-3xl">
          <p className="label-caps">
            {solicitud.etiquetaTipo}
            {solicitud.nombreAreaEspecialidad
              ? ` · Área: ${solicitud.nombreAreaEspecialidad}`
              : ""}
          </p>
          <h3 className="mt-1 font-display text-base font-semibold text-foreground">
            {solicitud.nombre}
          </h3>
          {solicitud.descripcion && (
            <p className="mt-1 text-sm text-muted-foreground">
              {solicitud.descripcion}
            </p>
          )}
        </div>
        <BadgeEstado nombre={solicitud.estado} />
      </div>
      <p className="mt-3 text-sm text-foreground">
        <span className="label-caps mr-2">Justificación</span>
        {solicitud.justificacion}
      </p>
      <p className="mt-2 text-xs text-muted-foreground">
        {mostrarSolicitante
          ? `${solicitud.nombreSolicitante} (${solicitud.rolSolicitante}) · `
          : ""}
        Enviada el {formatearFecha(solicitud.fechaSolicitud)}
      </p>
      {solicitud.fechaRespuesta && (
        <p className="mt-1 text-xs text-muted-foreground">
          Respondió {solicitud.nombreQuienResponde ?? "el administrador"} el{" "}
          {formatearFecha(solicitud.fechaRespuesta)}
          {solicitud.comentarioRespuesta
            ? `: "${solicitud.comentarioRespuesta}"`
            : "."}
        </p>
      )}
      {children}
    </li>
  );
}
