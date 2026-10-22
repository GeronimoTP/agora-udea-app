import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import {
  catalogoGestionQuery,
  gestionQueries,
  gestionService,
} from "@/lib/api/gestion";
import { catalogoSemilleroQuery } from "@/lib/api/queries";
import {
  BadgeEstado,
  Campo,
  Confirmar,
  EncabezadoSeccion,
  claseBotonPeligro,
  claseBotonPrimario,
  claseBotonSecundario,
  claseCampo,
  useAccionGestion,
} from "./comunes";

export function InformacionSemillero({
  idSemillero,
  onLiderazgoPerdido,
}: {
  idSemillero: number;
  // El usuario deja de ser líder (transfirió el liderazgo o eliminó el semillero)
  onLiderazgoPerdido: (esEliminacion: boolean) => void;
}) {
  const { data: semillero } = useSuspenseQuery(
    gestionQueries.semillero(idSemillero),
  );
  const { data: lineasActuales } = useSuspenseQuery(
    gestionQueries.lineas(idSemillero),
  );
  const { data: integrantes } = useSuspenseQuery(
    gestionQueries.integrantes(idSemillero),
  );
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { data: catalogoGestion } = useSuspenseQuery(catalogoGestionQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);

  const [codigo, setCodigo] = useState(semillero.codigoIdentificador);
  const [nombre, setNombre] = useState(semillero.nombre);
  const [descripcion, setDescripcion] = useState(semillero.descripcion);
  const [idPrograma, setIdPrograma] = useState(String(semillero.idPrograma));
  const [lineas, setLineas] = useState<number[]>(
    lineasActuales.map((l) => l.idLineaInvestigacion),
  );

  const estadoActual = catalogoGestion.estadosSemillero.find(
    (e) => e.nombre.toLowerCase() === semillero.estadoActual.toLowerCase(),
  );
  const [idEstado, setIdEstado] = useState(
    estadoActual ? String(estadoActual.id) : "",
  );
  const [idNuevoLider, setIdNuevoLider] = useState("");

  const activos = integrantes.filter((i) => i.fechaSalida === null);
  const codigoValido = /^[A-Z0-9-]+$/.test(codigo);
  const nombreValido = nombre.trim().length >= 5 && nombre.trim().length <= 150;
  const datosValidos =
    codigoValido &&
    nombreValido &&
    descripcion.trim() !== "" &&
    lineas.length > 0;

  const alternarLinea = (id: number) =>
    setLineas((actuales) =>
      actuales.includes(id)
        ? actuales.filter((l) => l !== id)
        : [...actuales, id],
    );

  const guardar = (event: React.FormEvent) => {
    event.preventDefault();
    if (!datosValidos) return;
    void ejecutar(
      () =>
        gestionService.actualizarSemillero(idSemillero, {
          codigoIdentificador: codigo,
          nombre: nombre.trim(),
          descripcion: descripcion.trim(),
          idEstudianteLider: semillero.idEstudianteLider,
          idPrograma: Number(idPrograma),
          idLineasInvestigacion: lineas,
        }),
      "Datos del semillero actualizados.",
    );
  };

  const nombreEstadoElegido =
    catalogoGestion.estadosSemillero.find((e) => String(e.id) === idEstado)
      ?.nombre ?? "";
  const nombreNuevoLider =
    activos.find((i) => String(i.idEstudiante) === idNuevoLider)
      ?.nombreEstudiante ?? "";

  return (
    <div className="grid gap-8 lg:grid-cols-3">
      <form onSubmit={guardar} className="panel space-y-4 p-6 lg:col-span-2">
        <EncabezadoSeccion
          titulo="Datos del semillero"
          descripcion="Información pública que ven los estudiantes en el directorio."
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Código identificador" htmlFor="codigo">
            <input
              id="codigo"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.toUpperCase())}
              className={claseCampo}
              required
            />
            {!codigoValido && (
              <p className="mt-1 text-xs text-destructive">
                Solo letras mayúsculas, números y guiones.
              </p>
            )}
          </Campo>
          <Campo etiqueta="Programa académico" htmlFor="programa">
            <select
              id="programa"
              value={idPrograma}
              onChange={(e) => setIdPrograma(e.target.value)}
              className={claseCampo}
            >
              {catalogo.programas.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </Campo>
        </div>
        <Campo etiqueta="Nombre" htmlFor="nombre">
          <input
            id="nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            maxLength={150}
            className={claseCampo}
            required
          />
          {!nombreValido && (
            <p className="mt-1 text-xs text-destructive">
              Entre 5 y 150 caracteres.
            </p>
          )}
        </Campo>
        <Campo etiqueta="Descripción" htmlFor="descripcion">
          <textarea
            id="descripcion"
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            rows={4}
            className={claseCampo}
            required
          />
        </Campo>
        <fieldset>
          <legend className="label-caps mb-1.5">
            Líneas de investigación (mínimo 1)
          </legend>
          <div className="flex flex-wrap gap-2">
            {catalogo.lineas.map((linea) => {
              const activa = lineas.includes(linea.id);
              return (
                <button
                  key={linea.id}
                  type="button"
                  onClick={() => alternarLinea(linea.id)}
                  aria-pressed={activa}
                  className={
                    activa
                      ? "rounded-sm border border-primary bg-primary px-2.5 py-1 text-sm text-primary-foreground"
                      : "rounded-sm border border-border bg-surface px-2.5 py-1 text-sm text-foreground hover:bg-secondary"
                  }
                >
                  {linea.nombre}
                </button>
              );
            })}
          </div>
        </fieldset>
        <button
          type="submit"
          disabled={!datosValidos || enCurso}
          className={claseBotonPrimario}
        >
          Guardar cambios
        </button>
      </form>

      <div className="space-y-6">
        <section className="panel space-y-3 p-6">
          <h2 className="font-display text-sm font-semibold text-foreground">
            Estado
          </h2>
          <p className="text-sm text-muted-foreground">
            Estado actual: <BadgeEstado nombre={semillero.estadoActual} />
          </p>
          <p className="text-xs text-muted-foreground">
            Solo un semillero activo publica convocatorias y aparece en las
            recomendaciones. Para pausarlo o inactivarlo, primero cierra sus
            convocatorias abiertas.
          </p>
          <select
            value={idEstado}
            onChange={(e) => setIdEstado(e.target.value)}
            aria-label="Nuevo estado del semillero"
            className={claseCampo}
          >
            {catalogoGestion.estadosSemillero.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nombre}
              </option>
            ))}
          </select>
          <Confirmar
            titulo={`¿Cambiar el estado a "${nombreEstadoElegido}"?`}
            descripcion="El cambio afecta la visibilidad del semillero y la posibilidad de abrir convocatorias."
            textoAccion="Cambiar estado"
            onConfirmar={() =>
              void ejecutar(
                () =>
                  gestionService.cambiarEstado(idSemillero, Number(idEstado)),
                `Semillero en estado "${nombreEstadoElegido}".`,
              )
            }
          >
            <button
              type="button"
              disabled={
                !idEstado || String(estadoActual?.id) === idEstado || enCurso
              }
              className={claseBotonSecundario}
            >
              Cambiar estado
            </button>
          </Confirmar>
        </section>

        <section className="panel space-y-3 p-6">
          <h2 className="font-display text-sm font-semibold text-foreground">
            Liderazgo estudiantil
          </h2>
          <p className="text-sm text-muted-foreground">
            Líder actual:{" "}
            <span className="font-medium text-foreground">
              {semillero.nombreEstudianteLider}
            </span>
          </p>
          <p className="text-xs text-muted-foreground">
            Transfiere el liderazgo a un integrante activo, por ejemplo antes de
            graduarte. El líder saliente queda como integrante del semillero.
          </p>
          {activos.length === 0 ? (
            <p className="text-xs text-muted-foreground">
              El semillero aún no tiene integrantes activos a quienes
              transferir.
            </p>
          ) : (
            <>
              <select
                value={idNuevoLider}
                onChange={(e) => setIdNuevoLider(e.target.value)}
                aria-label="Nuevo líder"
                className={claseCampo}
              >
                <option value="">Selecciona un integrante</option>
                {activos.map((i) => (
                  <option key={i.idEstudiante} value={i.idEstudiante}>
                    {i.nombreEstudiante}
                  </option>
                ))}
              </select>
              <Confirmar
                titulo={`¿Transferir el liderazgo a ${nombreNuevoLider}?`}
                descripcion="Si eres el estudiante líder, perderás la gestión del semillero y quedarás como integrante. Los profesores tutores conservan la gestión."
                textoAccion="Transferir"
                onConfirmar={() =>
                  void ejecutar(
                    () =>
                      gestionService.transferirLiderazgo(
                        idSemillero,
                        Number(idNuevoLider),
                      ),
                    `${nombreNuevoLider} es ahora el líder del semillero.`,
                  ).then((ok) => ok && onLiderazgoPerdido(false))
                }
              >
                <button
                  type="button"
                  disabled={!idNuevoLider || enCurso}
                  className={claseBotonSecundario}
                >
                  Transferir liderazgo
                </button>
              </Confirmar>
            </>
          )}
        </section>

        <section className="panel space-y-3 border-destructive/30 p-6">
          <h2 className="font-display text-sm font-semibold text-destructive">
            Eliminar semillero
          </h2>
          <p className="text-xs text-muted-foreground">
            Solo es posible si el semillero no tiene convocatorias, proyectos,
            integrantes ni reconocimientos. Si ya tiene historial, cámbialo a
            "Inactivo".
          </p>
          <Confirmar
            titulo="¿Eliminar el semillero?"
            descripcion="Esta acción no se puede deshacer."
            textoAccion="Eliminar"
            onConfirmar={() =>
              void ejecutar(
                () => gestionService.eliminarSemillero(idSemillero),
                "Semillero eliminado.",
              ).then((ok) => ok && onLiderazgoPerdido(true))
            }
          >
            <button
              type="button"
              disabled={enCurso}
              className={claseBotonPeligro}
            >
              Eliminar semillero
            </button>
          </Confirmar>
        </section>
      </div>
    </div>
  );
}
