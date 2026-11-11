import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { gestionQueries, gestionService } from "@/lib/api/gestion";
import {
  ESTADO_SOLICITUD_PENDIENTE,
  type SolicitudRetiro,
} from "@/lib/api/solicitudes";
import { catalogoSemilleroQuery, profesoresQuery } from "@/lib/api/queries";
import { BuscadorProfesores } from "./BuscadorProfesores";
import {
  Confirmar,
  EncabezadoSeccion,
  Vacio,
  claseBotonPeligro,
  claseBotonPrimario,
  claseBotonSecundario,
  claseCampo,
  useAccionGestion,
} from "./comunes";
import { formatearFecha } from "@/lib/formato";

export function MiembrosGestion({ idSemillero }: { idSemillero: number }) {
  return (
    <div className="space-y-10">
      <SolicitudesRetiro idSemillero={idSemillero} />
      <Integrantes idSemillero={idSemillero} />
      <Tutores idSemillero={idSemillero} />
    </div>
  );
}

/**
 * Retiro voluntario: el integrante pide salir y cualquier líder o tutor responde. Rechazar
 * exige un comentario (por ejemplo, para acordar una conversación); el estudiante sigue
 * como integrante y puede volver a pedirlo.
 */
function SolicitudesRetiro({ idSemillero }: { idSemillero: number }) {
  const { data: solicitudes } = useSuspenseQuery(
    gestionQueries.solicitudesRetiro(idSemillero),
  );
  const [verHistorial, setVerHistorial] = useState(false);
  const pendientes = solicitudes.filter(
    (s) => s.estado === ESTADO_SOLICITUD_PENDIENTE,
  );
  const respondidas = solicitudes.filter(
    (s) => s.estado !== ESTADO_SOLICITUD_PENDIENTE,
  );
  if (solicitudes.length === 0) return null;

  return (
    <section>
      <EncabezadoSeccion
        titulo={`Solicitudes de retiro (${pendientes.length} pendientes)`}
        descripcion="Integrantes que pidieron salir del semillero. Antes de responder puedes hablar con ellos; si apruebas, quedan retirados."
      />
      {pendientes.length === 0 ? (
        <Vacio>No hay solicitudes de retiro pendientes.</Vacio>
      ) : (
        <ul className="space-y-3">
          {pendientes.map((s) => (
            <SolicitudRetiroPendiente
              key={s.id}
              idSemillero={idSemillero}
              solicitud={s}
            />
          ))}
        </ul>
      )}
      {respondidas.length > 0 && (
        <div className="mt-4">
          <button
            type="button"
            onClick={() => setVerHistorial((v) => !v)}
            className="text-sm font-medium text-accent underline-offset-4 hover:underline"
            aria-expanded={verHistorial}
          >
            {verHistorial ? "Ocultar" : "Ver"} solicitudes respondidas (
            {respondidas.length})
          </button>
          {verHistorial && (
            <ul className="panel mt-3 divide-y divide-border">
              {respondidas.map((s) => (
                <li key={s.id} className="p-4 text-sm">
                  <p className="font-medium text-foreground">
                    {s.nombreEstudiante} · {s.estado}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Pidió el {formatearFecha(s.fechaSolicitud)}: "{s.motivo}"
                  </p>
                  {s.fechaRespuesta && (
                    <p className="text-xs text-muted-foreground">
                      Respondió {s.nombreQuienResponde ?? "un líder"} el{" "}
                      {formatearFecha(s.fechaRespuesta)}
                      {s.comentarioRespuesta
                        ? `: "${s.comentarioRespuesta}"`
                        : ""}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}

function SolicitudRetiroPendiente({
  idSemillero,
  solicitud,
}: {
  idSemillero: number;
  solicitud: SolicitudRetiro;
}) {
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [comentario, setComentario] = useState("");

  return (
    <li className="panel border-warning/50 p-5">
      <p className="text-sm font-medium text-foreground">
        {solicitud.nombreEstudiante}
      </p>
      <p className="text-xs text-muted-foreground">
        Solicitó retirarse el {formatearFecha(solicitud.fechaSolicitud)}
      </p>
      <p className="mt-2 rounded-sm border border-border bg-secondary p-3 text-sm text-foreground">
        {solicitud.motivo}
      </p>
      <label
        htmlFor={`comentario-retiro-${solicitud.id}`}
        className="label-caps mb-1.5 mt-3 block"
      >
        Comentario para el estudiante
      </label>
      <textarea
        id={`comentario-retiro-${solicitud.id}`}
        value={comentario}
        onChange={(e) => setComentario(e.target.value)}
        rows={2}
        maxLength={1000}
        placeholder="Obligatorio si rechazas; por ejemplo, para acordar una reunión."
        className={claseCampo}
      />
      <div className="mt-3 flex flex-wrap gap-2">
        <Confirmar
          titulo={`¿Aprobar el retiro de ${solicitud.nombreEstudiante}?`}
          descripcion="Quedará retirado con fecha de hoy. Su vinculación se conserva en el historial y podrá volver a postularse más adelante."
          textoAccion="Aprobar retiro"
          onConfirmar={() =>
            void ejecutar(
              () =>
                gestionService.aprobarRetiro(solicitud.id, comentario.trim()),
              `${solicitud.nombreEstudiante} fue retirado del semillero.`,
            )
          }
        >
          <button
            type="button"
            disabled={enCurso}
            className={claseBotonPrimario}
          >
            Aprobar retiro
          </button>
        </Confirmar>
        <button
          type="button"
          disabled={enCurso || comentario.trim() === ""}
          title={
            comentario.trim() === ""
              ? "Escribe un comentario para el estudiante"
              : undefined
          }
          onClick={() =>
            void ejecutar(
              () =>
                gestionService.rechazarRetiro(solicitud.id, comentario.trim()),
              "Solicitud rechazada. El estudiante sigue como integrante.",
            )
          }
          className={claseBotonSecundario}
        >
          Rechazar
        </button>
      </div>
    </li>
  );
}

function Integrantes({ idSemillero }: { idSemillero: number }) {
  const { data: semillero } = useSuspenseQuery(
    gestionQueries.semillero(idSemillero),
  );
  const { data: integrantes } = useSuspenseQuery(
    gestionQueries.integrantes(idSemillero),
  );
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [verHistorial, setVerHistorial] = useState(false);

  const activos = integrantes.filter((i) => i.fechaSalida === null);
  const retirados = integrantes.filter((i) => i.fechaSalida !== null);

  return (
    <section>
      <EncabezadoSeccion
        titulo={`Integrantes activos (${activos.length})`}
        descripcion={`Los estudiantes ingresan al aceptar una oferta de una convocatoria. Líder: ${semillero.nombreEstudianteLider}.`}
      />
      {activos.length === 0 ? (
        <Vacio>
          Aún no hay integrantes. Publica una convocatoria y pre-aprueba
          postulaciones para sumar estudiantes.
        </Vacio>
      ) : (
        <ul className="panel divide-y divide-border">
          {activos.map((i) => (
            <li
              key={i.idEstudiante}
              className="flex flex-wrap items-center justify-between gap-3 p-4"
            >
              <div>
                <p className="text-sm font-medium text-foreground">
                  {i.nombreEstudiante}
                </p>
                <p className="text-xs text-muted-foreground">
                  {i.email}
                  {i.nombrePrograma ? ` · ${i.nombrePrograma}` : ""}
                  {i.semestre ? ` · Semestre ${i.semestre}` : ""}
                </p>
                <p className="text-xs text-muted-foreground">
                  Ingresó el {formatearFecha(i.fechaIngreso)}
                </p>
              </div>
              <Confirmar
                titulo={`¿Retirar a ${i.nombreEstudiante}?`}
                descripcion="Su vinculación queda en el historial con fecha de salida de hoy. Podrá volver a postularse en una convocatoria futura."
                textoAccion="Retirar"
                onConfirmar={() =>
                  void ejecutar(
                    () =>
                      gestionService.retirarIntegrante(
                        idSemillero,
                        i.idEstudiante,
                      ),
                    `${i.nombreEstudiante} fue retirado del semillero.`,
                  )
                }
              >
                <button
                  type="button"
                  disabled={enCurso}
                  className={claseBotonPeligro}
                >
                  Retirar
                </button>
              </Confirmar>
            </li>
          ))}
        </ul>
      )}

      {retirados.length > 0 && (
        <div className="mt-4">
          <button
            type="button"
            onClick={() => setVerHistorial((v) => !v)}
            className="text-sm font-medium text-accent underline-offset-4 hover:underline"
            aria-expanded={verHistorial}
          >
            {verHistorial ? "Ocultar" : "Ver"} historial de integrantes (
            {retirados.length})
          </button>
          {verHistorial && (
            <ul className="panel mt-3 divide-y divide-border">
              {retirados.map((i) => (
                <li key={i.idEstudiante} className="p-4 text-sm">
                  <p className="font-medium text-foreground">
                    {i.nombreEstudiante}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatearFecha(i.fechaIngreso)} –{" "}
                    {formatearFecha(i.fechaSalida)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}

function Tutores({ idSemillero }: { idSemillero: number }) {
  const { data: tutores } = useSuspenseQuery(
    gestionQueries.tutores(idSemillero),
  );
  const { data: profesores } = useSuspenseQuery(profesoresQuery());
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [agregando, setAgregando] = useState(false);

  const unico = tutores.length <= 1;

  return (
    <section>
      <EncabezadoSeccion
        titulo={`Profesores tutores (${tutores.length})`}
        descripcion="Los tutores también gestionan el semillero. Debe haber al menos uno."
        accion={
          <button
            type="button"
            onClick={() => setAgregando((v) => !v)}
            className={claseBotonSecundario}
            aria-expanded={agregando}
          >
            {agregando ? "Cerrar buscador" : "Agregar tutor"}
          </button>
        }
      />
      <ul className="panel divide-y divide-border">
        {tutores.map((t) => (
          <li
            key={t.idProfesor}
            className="flex flex-wrap items-center justify-between gap-3 p-4"
          >
            <div>
              <p className="text-sm font-medium text-foreground">
                {t.nombreProfesor}
              </p>
              <p className="text-xs text-muted-foreground">
                {t.email} · Desde {formatearFecha(t.fechaAsignacion)}
              </p>
            </div>
            <Confirmar
              titulo={`¿Quitar a ${t.nombreProfesor} como tutor?`}
              descripcion="Dejará de gestionar el semillero."
              textoAccion="Quitar tutor"
              onConfirmar={() =>
                void ejecutar(
                  () => gestionService.quitarTutor(idSemillero, t.idProfesor),
                  `${t.nombreProfesor} ya no es tutor del semillero.`,
                )
              }
            >
              <button
                type="button"
                disabled={unico || enCurso}
                title={
                  unico
                    ? "El semillero debe conservar al menos un tutor"
                    : undefined
                }
                className={claseBotonPeligro}
              >
                Quitar
              </button>
            </Confirmar>
          </li>
        ))}
      </ul>

      {agregando && (
        <div className="panel mt-4 p-5">
          <BuscadorProfesores
            profesores={profesores}
            programas={catalogo.programas}
            facultades={catalogo.facultades}
            areas={catalogo.areas}
            excluir={tutores.map((t) => t.idProfesor)}
            renderAccion={(p) => (
              <button
                type="button"
                disabled={enCurso}
                onClick={() =>
                  void ejecutar(
                    () => gestionService.agregarTutor(idSemillero, p.id),
                    `${p.nombreCompleto} es ahora tutor del semillero.`,
                  )
                }
                className={claseBotonSecundario}
              >
                Agregar
              </button>
            )}
          />
        </div>
      )}
    </section>
  );
}
