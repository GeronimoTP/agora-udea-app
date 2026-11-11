import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { gestionService, type SemilleroDetalle } from "@/lib/api/gestion";
import { solicitudesQueries, solicitudesService } from "@/lib/api/solicitudes";
import { formatearFecha } from "@/lib/formato";
import {
  Confirmar,
  EncabezadoSeccion,
  Vacio,
  claseBotonPrimario,
  claseBotonSecundario,
  claseCampo,
} from "@/components/gestion/comunes";
import { useAccionSolicitud } from "./useAccionSolicitud";

/**
 * Semilleros registrados por estudiantes o profesores que esperan la aprobación del
 * administrador. Al aprobarlos pasan a "Activo" y se vuelven públicos; al rechazarlos, sus
 * líderes ven el motivo y pueden corregirlos y reenviarlos.
 */
export function SemillerosPorAprobar() {
  const { data: semilleros } = useSuspenseQuery(
    solicitudesQueries.semillerosPorAprobar(),
  );

  return (
    <section>
      <EncabezadoSeccion
        titulo={`Semilleros por aprobar (${semilleros.length})`}
        descripcion="Revisa que cada semillero tenga un objetivo claro, líneas coherentes y tutores adecuados. Mientras no lo apruebes, no es público ni opera."
      />
      {semilleros.length === 0 ? (
        <Vacio>No hay semilleros esperando aprobación.</Vacio>
      ) : (
        <ul className="space-y-4">
          {semilleros.map((s) => (
            <SemilleroPorAprobar key={s.id} semillero={s} />
          ))}
        </ul>
      )}
    </section>
  );
}

function SemilleroPorAprobar({ semillero }: { semillero: SemilleroDetalle }) {
  const { ejecutar, enCurso } = useAccionSolicitud();
  const [rechazando, setRechazando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const { data: tutores = [] } = useQuery({
    queryKey: ["solicitudes", "semilleros", semillero.id, "tutores"],
    queryFn: () => gestionService.tutores(semillero.id),
  });

  return (
    <li className="panel p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="max-w-3xl">
          <p className="label-caps">
            {semillero.codigoIdentificador} · Enviado el{" "}
            {formatearFecha(semillero.fechaSolicitud)}
          </p>
          <h3 className="mt-1 font-display text-base font-semibold text-foreground">
            {semillero.nombre}
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
            {semillero.descripcion}
          </p>
        </div>
      </div>
      <dl className="mt-4 grid gap-3 border-t border-border pt-4 text-sm sm:grid-cols-2 xl:grid-cols-4">
        <div>
          <dt className="label-caps">Programa</dt>
          <dd className="mt-1 text-foreground">
            {semillero.nombrePrograma}
            {semillero.nombreFacultad ? ` · ${semillero.nombreFacultad}` : ""}
          </dd>
        </div>
        <div>
          <dt className="label-caps">Estudiante líder</dt>
          <dd className="mt-1 text-foreground">
            {semillero.nombreEstudianteLider}
          </dd>
        </div>
        <div>
          <dt className="label-caps">Tutores</dt>
          <dd className="mt-1 text-foreground">
            {tutores.map((t) => t.nombreProfesor).join(", ") || "—"}
          </dd>
        </div>
        <div>
          <dt className="label-caps">Líneas de investigación</dt>
          <dd className="mt-1 text-foreground">
            {semillero.lineasInvestigacion.join(", ")}
          </dd>
        </div>
      </dl>
      {semillero.motivoRechazo && (
        <p className="mt-3 text-xs text-muted-foreground">
          Fue rechazado antes y lo reenviaron. Motivo anterior: "
          {semillero.motivoRechazo}"
        </p>
      )}

      {rechazando ? (
        <div className="mt-4">
          <label
            htmlFor={`motivo-semillero-${semillero.id}`}
            className="label-caps mb-1.5 block"
          >
            Motivo del rechazo (lo verán sus líderes)
          </label>
          <textarea
            id={`motivo-semillero-${semillero.id}`}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            rows={3}
            maxLength={1000}
            className={claseCampo}
            placeholder="Explica qué deben corregir para aprobarlo."
          />
          <div className="mt-3 flex gap-2">
            <button
              type="button"
              disabled={enCurso || motivo.trim() === ""}
              onClick={() =>
                void ejecutar(
                  () =>
                    solicitudesService.rechazarSemillero(
                      semillero.id,
                      motivo.trim(),
                    ),
                  `Semillero "${semillero.nombre}" rechazado.`,
                )
              }
              className={claseBotonPrimario}
            >
              Rechazar semillero
            </button>
            <button
              type="button"
              onClick={() => setRechazando(false)}
              className={claseBotonSecundario}
            >
              Cancelar
            </button>
          </div>
        </div>
      ) : (
        <div className="mt-4 flex flex-wrap gap-2">
          <Confirmar
            titulo={`¿Aprobar "${semillero.nombre}"?`}
            descripcion="Quedará activo y público: podrá abrir convocatorias y aparecerá en las recomendaciones."
            textoAccion="Aprobar"
            onConfirmar={() =>
              void ejecutar(
                () => solicitudesService.aprobarSemillero(semillero.id),
                `Semillero "${semillero.nombre}" aprobado.`,
              )
            }
          >
            <button
              type="button"
              disabled={enCurso}
              className={claseBotonPrimario}
            >
              Aprobar
            </button>
          </Confirmar>
          <button
            type="button"
            disabled={enCurso}
            onClick={() => setRechazando(true)}
            className={claseBotonSecundario}
          >
            Rechazar…
          </button>
        </div>
      )}
    </li>
  );
}
