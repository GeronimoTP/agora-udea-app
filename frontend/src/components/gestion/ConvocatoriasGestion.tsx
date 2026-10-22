import { Link } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import {
  gestionQueries,
  gestionService,
  type ConvocatoriaGestion,
} from "@/lib/api/gestion";
import {
  BadgeEstado,
  Campo,
  Confirmar,
  EncabezadoSeccion,
  Vacio,
  claseBotonPeligro,
  claseBotonPrimario,
  claseBotonSecundario,
  claseCampo,
  fechaHoy,
  formatearFecha,
  useAccionGestion,
} from "./comunes";

export function ConvocatoriasGestion({ idSemillero }: { idSemillero: number }) {
  const { data: semillero } = useSuspenseQuery(
    gestionQueries.semillero(idSemillero),
  );
  const { data: convocatorias } = useSuspenseQuery(
    gestionQueries.convocatorias(idSemillero),
  );
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  // null: formulario cerrado; "nueva": creando; una convocatoria: editándola
  const [editando, setEditando] = useState<
    ConvocatoriaGestion | "nueva" | null
  >(null);

  const semilleroActivo = semillero.estadoActual.toLowerCase() === "activo";
  const ordenadas = [...convocatorias].sort((a, b) =>
    b.fechaInicio.localeCompare(a.fechaInicio),
  );

  return (
    <section>
      <EncabezadoSeccion
        titulo="Convocatorias"
        descripcion="Abre convocatorias para vincular estudiantes. Pre-aprobar una postulación no descuenta cupos: el cupo se asigna cuando el estudiante confirma."
        accion={
          <div className="flex flex-wrap gap-2">
            <Link to="/postulaciones" className={claseBotonSecundario}>
              Revisar postulaciones
            </Link>
            <button
              type="button"
              onClick={() => setEditando("nueva")}
              disabled={!semilleroActivo}
              title={
                semilleroActivo ? undefined : "El semillero debe estar activo"
              }
              className={`inline-flex items-center gap-1.5 ${claseBotonPrimario}`}
            >
              <Plus className="size-4" strokeWidth={1.75} aria-hidden />
              Nueva convocatoria
            </button>
          </div>
        }
      />

      {!semilleroActivo && (
        <p className="mb-4 text-sm text-muted-foreground">
          El semillero está en estado "{semillero.estadoActual}": no puede abrir
          convocatorias nuevas.
        </p>
      )}

      {editando !== null && (
        <FormularioConvocatoria
          key={editando === "nueva" ? "nueva" : editando.id}
          idSemillero={idSemillero}
          convocatoria={editando === "nueva" ? null : editando}
          enCurso={enCurso}
          onCancelar={() => setEditando(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(
              () =>
                editando === "nueva"
                  ? gestionService.crearConvocatoria(datos)
                  : gestionService.actualizarConvocatoria(editando.id, datos),
              editando === "nueva"
                ? "Convocatoria publicada."
                : "Convocatoria actualizada.",
            );
            if (ok) setEditando(null);
          }}
        />
      )}

      {ordenadas.length === 0 ? (
        <Vacio>El semillero aún no tiene convocatorias.</Vacio>
      ) : (
        <ul className="space-y-3">
          {ordenadas.map((c) => {
            const estado = c.estadoActual.toLowerCase();
            const abierta = estado === "abierta";
            const editable = estado === "abierta" || estado === "cerrada";
            const ocupados = c.cuposTotales - c.cuposDisponibles;
            return (
              <li key={c.id} className="panel p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="max-w-2xl">
                    <h3 className="font-display text-base font-semibold text-foreground">
                      {c.titulo}
                    </h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {c.descripcion}
                    </p>
                  </div>
                  <BadgeEstado nombre={c.estadoActual} />
                </div>
                <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4 text-sm sm:grid-cols-4">
                  <div>
                    <dt className="label-caps">Apertura</dt>
                    <dd className="mt-1 text-foreground">
                      {formatearFecha(c.fechaInicio)}
                    </dd>
                  </div>
                  <div>
                    <dt className="label-caps">Cierre</dt>
                    <dd className="mt-1 text-foreground">
                      {formatearFecha(c.fechaCierre)}
                    </dd>
                  </div>
                  <div>
                    <dt className="label-caps">Cupos asignados</dt>
                    <dd className="mt-1 tabular-nums text-foreground">
                      {ocupados} de {c.cuposTotales}
                    </dd>
                  </div>
                  <div>
                    <dt className="label-caps">Disponibles</dt>
                    <dd className="mt-1 tabular-nums text-foreground">
                      {c.cuposDisponibles}
                    </dd>
                  </div>
                </dl>
                <div className="mt-4 flex flex-wrap gap-2">
                  {editable && (
                    <button
                      type="button"
                      onClick={() => setEditando(c)}
                      disabled={enCurso}
                      className={claseBotonSecundario}
                    >
                      Editar
                    </button>
                  )}
                  {abierta && (
                    <Confirmar
                      titulo="¿Cerrar la convocatoria?"
                      descripcion="Dejará de recibir postulaciones. Las postulaciones pendientes y las ofertas sin respuesta expirarán; los cupos ya asignados se conservan."
                      textoAccion="Cerrar convocatoria"
                      onConfirmar={() =>
                        void ejecutar(
                          () => gestionService.cerrarConvocatoria(c.id),
                          "Convocatoria cerrada.",
                        )
                      }
                    >
                      <button
                        type="button"
                        disabled={enCurso}
                        className={claseBotonSecundario}
                      >
                        Cerrar
                      </button>
                    </Confirmar>
                  )}
                  {abierta && (
                    <Confirmar
                      titulo="¿Cancelar la convocatoria?"
                      descripcion="La convocatoria no se llevará a cabo y ya no podrá editarse. Las postulaciones pendientes y las ofertas sin respuesta expirarán."
                      textoAccion="Cancelar convocatoria"
                      onConfirmar={() =>
                        void ejecutar(
                          () => gestionService.cancelarConvocatoria(c.id),
                          "Convocatoria cancelada.",
                        )
                      }
                    >
                      <button
                        type="button"
                        disabled={enCurso}
                        className={claseBotonPeligro}
                      >
                        Cancelar
                      </button>
                    </Confirmar>
                  )}
                  <Confirmar
                    titulo="¿Eliminar la convocatoria?"
                    descripcion="Solo se puede eliminar si nadie se ha postulado. Si ya tiene postulaciones, ciérrala o cancélala."
                    textoAccion="Eliminar"
                    onConfirmar={() =>
                      void ejecutar(
                        () => gestionService.eliminarConvocatoria(c.id),
                        "Convocatoria eliminada.",
                      )
                    }
                  >
                    <button
                      type="button"
                      disabled={enCurso}
                      className={claseBotonPeligro}
                    >
                      Eliminar
                    </button>
                  </Confirmar>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function FormularioConvocatoria({
  idSemillero,
  convocatoria,
  enCurso,
  onCancelar,
  onGuardar,
}: {
  idSemillero: number;
  convocatoria: ConvocatoriaGestion | null;
  enCurso: boolean;
  onCancelar: () => void;
  onGuardar: (datos: {
    idSemillero: number;
    titulo: string;
    descripcion: string;
    cuposTotales: number;
    fechaInicio: string;
    fechaCierre: string;
  }) => void;
}) {
  const hoy = fechaHoy();
  const [titulo, setTitulo] = useState(convocatoria?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(
    convocatoria?.descripcion ?? "",
  );
  const [cupos, setCupos] = useState(String(convocatoria?.cuposTotales ?? 1));
  const [fechaInicio, setFechaInicio] = useState(
    convocatoria?.fechaInicio ?? hoy,
  );
  const [fechaCierre, setFechaCierre] = useState(
    convocatoria?.fechaCierre ?? "",
  );

  const ocupados = convocatoria
    ? convocatoria.cuposTotales - convocatoria.cuposDisponibles
    : 0;
  const cuposNumero = Number(cupos);
  const errores: string[] = [];
  if (!Number.isInteger(cuposNumero) || cuposNumero < 1)
    errores.push("Debe haber al menos 1 cupo.");
  if (cuposNumero < ocupados)
    errores.push(`Ya hay ${ocupados} cupos asignados.`);
  if (!convocatoria && fechaInicio < hoy)
    errores.push("La fecha de inicio no puede ser pasada.");
  if (fechaCierre && fechaCierre < fechaInicio)
    errores.push("El cierre no puede ser antes del inicio.");
  if (fechaCierre && fechaCierre < hoy)
    errores.push("La fecha de cierre ya pasó.");
  const valido =
    titulo.trim() !== "" &&
    descripcion.trim() !== "" &&
    fechaCierre !== "" &&
    errores.length === 0;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valido) return;
        onGuardar({
          idSemillero,
          titulo: titulo.trim(),
          descripcion: descripcion.trim(),
          cuposTotales: cuposNumero,
          fechaInicio,
          fechaCierre,
        });
      }}
      className="panel mb-6 space-y-4 p-6"
    >
      <h3 className="font-display text-sm font-semibold text-foreground">
        {convocatoria ? "Editar convocatoria" : "Nueva convocatoria"}
      </h3>
      {convocatoria?.estadoActual.toLowerCase() === "cerrada" && (
        <p className="text-xs text-muted-foreground">
          Esta convocatoria está cerrada. Si al guardar queda con cupos
          disponibles y su fecha de cierre no ha pasado, se reabrirá.
        </p>
      )}
      <Campo etiqueta="Título" htmlFor="conv-titulo">
        <input
          id="conv-titulo"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          className={claseCampo}
          required
        />
      </Campo>
      <Campo etiqueta="Descripción y requisitos" htmlFor="conv-descripcion">
        <textarea
          id="conv-descripcion"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          rows={3}
          className={claseCampo}
          required
        />
      </Campo>
      <div className="grid gap-4 sm:grid-cols-3">
        <Campo
          etiqueta="Cupos totales"
          htmlFor="conv-cupos"
          ayuda={ocupados > 0 ? `${ocupados} ya asignados` : undefined}
        >
          <input
            id="conv-cupos"
            type="number"
            min={Math.max(1, ocupados)}
            value={cupos}
            onChange={(e) => setCupos(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
        <Campo etiqueta="Fecha de inicio" htmlFor="conv-inicio">
          <input
            id="conv-inicio"
            type="date"
            min={convocatoria ? undefined : hoy}
            value={fechaInicio}
            onChange={(e) => setFechaInicio(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
        <Campo etiqueta="Fecha de cierre" htmlFor="conv-cierre">
          <input
            id="conv-cierre"
            type="date"
            min={fechaInicio > hoy ? fechaInicio : hoy}
            value={fechaCierre}
            onChange={(e) => setFechaCierre(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
      </div>
      {errores.length > 0 && (
        <ul className="space-y-1 text-xs text-destructive">
          {errores.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={!valido || enCurso}
          className={claseBotonPrimario}
        >
          {convocatoria ? "Guardar cambios" : "Publicar convocatoria"}
        </button>
        <button
          type="button"
          onClick={onCancelar}
          className={claseBotonSecundario}
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
