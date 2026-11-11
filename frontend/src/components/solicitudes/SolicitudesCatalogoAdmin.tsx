import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { catalogoSemilleroQuery } from "@/lib/api/queries";
import {
  solicitudesQueries,
  solicitudesService,
  TIPOS_CATALOGO,
  type SolicitudCatalogo,
} from "@/lib/api/solicitudes";
import {
  Campo,
  EncabezadoSeccion,
  Vacio,
  claseBotonPrimario,
  claseBotonSecundario,
  claseCampo,
} from "@/components/gestion/comunes";
import { TarjetaSolicitudCatalogo } from "./TarjetaSolicitudCatalogo";
import { useAccionSolicitud } from "./useAccionSolicitud";

/**
 * Solicitudes de estudiantes y profesores para agregar elementos al catálogo. El
 * administrador evalúa si son válidos e importantes: al aprobar, el elemento se crea (puede
 * ajustar el nombre, la descripción o el área antes); al rechazar, explica el motivo.
 */
export function SolicitudesCatalogoAdmin() {
  const [soloPendientes, setSoloPendientes] = useState(true);
  const { data: solicitudes } = useSuspenseQuery(
    solicitudesQueries.catalogo(soloPendientes),
  );

  return (
    <section>
      <EncabezadoSeccion
        titulo="Solicitudes de catálogo"
        descripcion="Habilidades, líneas, áreas, entidades y tipos de reconocimiento que proponen estudiantes y profesores. Al aprobar una, queda disponible para todos."
        accion={
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={!soloPendientes}
              onChange={(e) => setSoloPendientes(!e.target.checked)}
            />
            Ver también las respondidas
          </label>
        }
      />
      {solicitudes.length === 0 ? (
        <Vacio>
          {soloPendientes
            ? "No hay solicitudes pendientes."
            : "Aún no hay solicitudes."}
        </Vacio>
      ) : (
        <ul className="space-y-4">
          {solicitudes.map((s) =>
            s.estado === "Pendiente" ? (
              <SolicitudPendiente key={s.id} solicitud={s} />
            ) : (
              <TarjetaSolicitudCatalogo
                key={s.id}
                solicitud={s}
                mostrarSolicitante
              />
            ),
          )}
        </ul>
      )}
    </section>
  );
}

function SolicitudPendiente({ solicitud }: { solicitud: SolicitudCatalogo }) {
  const { ejecutar, enCurso } = useAccionSolicitud();
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const [modo, setModo] = useState<"ver" | "aprobar" | "rechazar">("ver");
  const [nombre, setNombre] = useState(solicitud.nombre);
  const [descripcion, setDescripcion] = useState(solicitud.descripcion ?? "");
  const [idArea, setIdArea] = useState(
    solicitud.idAreaEspecialidad ? String(solicitud.idAreaEspecialidad) : "",
  );
  const [comentario, setComentario] = useState("");
  const requiereDescripcion =
    TIPOS_CATALOGO.find((t) => t.tipo === solicitud.tipo)
      ?.requiereDescripcion ?? true;
  const esLinea = solicitud.tipo === "LINEA_INVESTIGACION";

  return (
    <TarjetaSolicitudCatalogo solicitud={solicitud} mostrarSolicitante>
      {modo === "ver" && (
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setModo("aprobar")}
            className={claseBotonPrimario}
          >
            Aprobar…
          </button>
          <button
            type="button"
            onClick={() => setModo("rechazar")}
            className={claseBotonSecundario}
          >
            Rechazar…
          </button>
        </div>
      )}

      {modo === "aprobar" && (
        <div className="mt-4 space-y-3 border-t border-border pt-4">
          <p className="text-xs text-muted-foreground">
            Puedes ajustar los datos antes de agregarlo al catálogo.
          </p>
          <div className="grid gap-3 sm:grid-cols-2">
            <Campo etiqueta="Nombre" htmlFor={`nombre-cat-${solicitud.id}`}>
              <input
                id={`nombre-cat-${solicitud.id}`}
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={150}
                className={claseCampo}
              />
            </Campo>
            {esLinea && (
              <Campo
                etiqueta="Área de especialidad"
                htmlFor={`area-cat-${solicitud.id}`}
              >
                <select
                  id={`area-cat-${solicitud.id}`}
                  value={idArea}
                  onChange={(e) => setIdArea(e.target.value)}
                  className={claseCampo}
                >
                  {catalogo.areas.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.nombre}
                    </option>
                  ))}
                </select>
              </Campo>
            )}
          </div>
          {requiereDescripcion && (
            <Campo
              etiqueta="Descripción"
              htmlFor={`descripcion-cat-${solicitud.id}`}
            >
              <textarea
                id={`descripcion-cat-${solicitud.id}`}
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                rows={2}
                maxLength={1000}
                className={claseCampo}
              />
            </Campo>
          )}
          <Campo
            etiqueta="Comentario para quien lo solicitó (opcional)"
            htmlFor={`comentario-cat-${solicitud.id}`}
          >
            <input
              id={`comentario-cat-${solicitud.id}`}
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              maxLength={1000}
              className={claseCampo}
            />
          </Campo>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={
                enCurso ||
                nombre.trim() === "" ||
                (requiereDescripcion && descripcion.trim() === "")
              }
              onClick={() =>
                void ejecutar(
                  () =>
                    solicitudesService.aprobarCatalogo(solicitud.id, {
                      nombre: nombre.trim(),
                      ...(requiereDescripcion
                        ? { descripcion: descripcion.trim() }
                        : {}),
                      ...(esLinea && idArea
                        ? { idAreaEspecialidad: Number(idArea) }
                        : {}),
                      ...(comentario.trim()
                        ? { comentario: comentario.trim() }
                        : {}),
                    }),
                  `"${nombre.trim()}" agregado al catálogo.`,
                )
              }
              className={claseBotonPrimario}
            >
              Agregar al catálogo
            </button>
            <button
              type="button"
              onClick={() => setModo("ver")}
              className={claseBotonSecundario}
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      {modo === "rechazar" && (
        <div className="mt-4 space-y-3 border-t border-border pt-4">
          <Campo
            etiqueta="Motivo del rechazo"
            htmlFor={`motivo-cat-${solicitud.id}`}
          >
            <textarea
              id={`motivo-cat-${solicitud.id}`}
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              rows={2}
              maxLength={1000}
              placeholder="Por ejemplo: ya existe una habilidad equivalente."
              className={claseCampo}
            />
          </Campo>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={enCurso || comentario.trim() === ""}
              onClick={() =>
                void ejecutar(
                  () =>
                    solicitudesService.rechazarCatalogo(
                      solicitud.id,
                      comentario.trim(),
                    ),
                  "Solicitud rechazada.",
                )
              }
              className={claseBotonPrimario}
            >
              Rechazar solicitud
            </button>
            <button
              type="button"
              onClick={() => setModo("ver")}
              className={claseBotonSecundario}
            >
              Cancelar
            </button>
          </div>
        </div>
      )}
    </TarjetaSolicitudCatalogo>
  );
}
