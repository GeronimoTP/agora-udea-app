import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import {
  catalogoGestionQuery,
  gestionQueries,
  gestionService,
  type NuevoProyecto,
  type ProyectoGestion,
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
  formatearFecha,
  useAccionGestion,
} from "./comunes";

const formatoPesos = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

export function ProyectosGestion({ idSemillero }: { idSemillero: number }) {
  const { data: proyectos } = useSuspenseQuery(
    gestionQueries.proyectos(idSemillero),
  );
  const { data: catalogo } = useSuspenseQuery(catalogoGestionQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [editando, setEditando] = useState<ProyectoGestion | "nuevo" | null>(
    null,
  );
  const [abierto, setAbierto] = useState<number | null>(null);

  return (
    <section>
      <EncabezadoSeccion
        titulo="Proyectos de investigación"
        descripcion="Registra los proyectos del semillero y asigna a sus integrantes con un rol. Un proyecto con publicaciones no se elimina: cámbialo a 'Completado' o 'Cancelado'."
        accion={
          <button
            type="button"
            onClick={() => setEditando("nuevo")}
            className={`inline-flex items-center gap-1.5 ${claseBotonPrimario}`}
          >
            <Plus className="size-4" strokeWidth={1.75} aria-hidden />
            Nuevo proyecto
          </button>
        }
      />

      {editando !== null && (
        <FormularioProyecto
          key={editando === "nuevo" ? "nuevo" : editando.id}
          idSemillero={idSemillero}
          proyecto={editando === "nuevo" ? null : editando}
          enCurso={enCurso}
          onCancelar={() => setEditando(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(
              () =>
                editando === "nuevo"
                  ? gestionService.crearProyecto(datos)
                  : gestionService.actualizarProyecto(editando.id, datos),
              editando === "nuevo"
                ? "Proyecto registrado."
                : "Proyecto actualizado.",
            );
            if (ok) setEditando(null);
          }}
        />
      )}

      {proyectos.length === 0 ? (
        <Vacio>El semillero aún no tiene proyectos registrados.</Vacio>
      ) : (
        <ul className="space-y-3">
          {proyectos.map((p) => (
            <li key={p.id} className="panel p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="max-w-2xl">
                  <h3 className="font-display text-base font-semibold text-foreground">
                    {p.titulo}
                  </h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {p.descripcion}
                  </p>
                </div>
                <BadgeEstado nombre={p.estadoActual} />
              </div>
              <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4 text-sm sm:grid-cols-4">
                <div>
                  <dt className="label-caps">Inicio</dt>
                  <dd className="mt-1 text-foreground">
                    {formatearFecha(p.fechaInicio)}
                  </dd>
                </div>
                <div>
                  <dt className="label-caps">Fin</dt>
                  <dd className="mt-1 text-foreground">
                    {formatearFecha(p.fechaFin)}
                  </dd>
                </div>
                <div>
                  <dt className="label-caps">Presupuesto</dt>
                  <dd className="mt-1 tabular-nums text-foreground">
                    {formatoPesos.format(p.presupuestoAsignado)}
                  </dd>
                </div>
                <div>
                  <dt className="label-caps">Integrantes</dt>
                  <dd className="mt-1 tabular-nums text-foreground">
                    {p.totalIntegrantes}
                  </dd>
                </div>
              </dl>
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => setAbierto(abierto === p.id ? null : p.id)}
                  aria-expanded={abierto === p.id}
                  className={claseBotonSecundario}
                >
                  {abierto === p.id ? "Ocultar integrantes" : "Integrantes"}
                </button>
                <button
                  type="button"
                  onClick={() => setEditando(p)}
                  disabled={enCurso}
                  className={claseBotonSecundario}
                >
                  Editar
                </button>
                <select
                  value=""
                  onChange={(e) => {
                    const estado = catalogo.estadosProyecto.find(
                      (x) => String(x.id) === e.target.value,
                    );
                    if (!estado) return;
                    void ejecutar(
                      () =>
                        gestionService.cambiarEstadoProyecto(p.id, estado.id),
                      `Proyecto en estado "${estado.nombre}".`,
                    );
                  }}
                  disabled={enCurso}
                  aria-label={`Cambiar estado de ${p.titulo}`}
                  className={`${claseCampo} w-auto py-1.5`}
                >
                  <option value="">Cambiar estado…</option>
                  {catalogo.estadosProyecto
                    .filter((e) => e.nombre !== p.estadoActual)
                    .map((e) => (
                      <option key={e.id} value={e.id}>
                        {e.nombre}
                      </option>
                    ))}
                </select>
                <Confirmar
                  titulo="¿Eliminar el proyecto?"
                  descripcion="Se quitarán también sus integrantes. No es posible si el proyecto tiene publicaciones."
                  textoAccion="Eliminar"
                  onConfirmar={() =>
                    void ejecutar(
                      () => gestionService.eliminarProyecto(p.id),
                      "Proyecto eliminado.",
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
              {abierto === p.id && (
                <IntegrantesProyecto
                  idSemillero={idSemillero}
                  idProyecto={p.id}
                />
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function IntegrantesProyecto({
  idSemillero,
  idProyecto,
}: {
  idSemillero: number;
  idProyecto: number;
}) {
  const { data: vinculados = [], isLoading } = useQuery(
    gestionQueries.integrantesProyecto(idSemillero, idProyecto),
  );
  const { data: semillero } = useSuspenseQuery(
    gestionQueries.semillero(idSemillero),
  );
  const { data: integrantes } = useSuspenseQuery(
    gestionQueries.integrantes(idSemillero),
  );
  const { data: catalogo } = useSuspenseQuery(catalogoGestionQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [idEstudiante, setIdEstudiante] = useState("");
  const [idRol, setIdRol] = useState("");

  // Pueden trabajar en el proyecto el estudiante líder y los integrantes activos
  const candidatos = [
    {
      idEstudiante: semillero.idEstudianteLider,
      nombre: `${semillero.nombreEstudianteLider} (líder)`,
    },
    ...integrantes
      .filter((i) => i.fechaSalida === null)
      .map((i) => ({
        idEstudiante: i.idEstudiante,
        nombre: i.nombreEstudiante,
      })),
  ].filter((c) => !vinculados.some((v) => v.idEstudiante === c.idEstudiante));

  return (
    <div className="mt-4 border-t border-border pt-4">
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Cargando integrantes…</p>
      ) : vinculados.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Nadie está vinculado a este proyecto.
        </p>
      ) : (
        <ul className="divide-y divide-border rounded-sm border border-border">
          {vinculados.map((v) => (
            <li
              key={v.idEstudiante}
              className="flex items-center justify-between gap-3 p-3"
            >
              <div>
                <p className="text-sm font-medium text-foreground">
                  {v.nombreEstudiante}
                </p>
                <p className="text-xs text-muted-foreground">
                  {v.rolProyecto} · Desde {formatearFecha(v.fechaVinculacion)}
                </p>
              </div>
              <button
                type="button"
                disabled={enCurso}
                onClick={() =>
                  void ejecutar(
                    () =>
                      gestionService.desvincularDeProyecto(
                        idProyecto,
                        v.idEstudiante,
                      ),
                    `${v.nombreEstudiante} fue desvinculado del proyecto.`,
                  )
                }
                className={claseBotonPeligro}
              >
                Quitar
              </button>
            </li>
          ))}
        </ul>
      )}

      {candidatos.length > 0 && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!idEstudiante || !idRol) return;
            void ejecutar(
              () =>
                gestionService.vincularAProyecto(
                  idProyecto,
                  Number(idEstudiante),
                  Number(idRol),
                ),
              "Estudiante vinculado al proyecto.",
            ).then((ok) => {
              if (ok) {
                setIdEstudiante("");
                setIdRol("");
              }
            });
          }}
          className="mt-3 flex flex-wrap items-end gap-2"
        >
          <select
            value={idEstudiante}
            onChange={(e) => setIdEstudiante(e.target.value)}
            aria-label="Estudiante a vincular"
            className={`${claseCampo} w-auto`}
          >
            <option value="">Estudiante…</option>
            {candidatos.map((c) => (
              <option key={c.idEstudiante} value={c.idEstudiante}>
                {c.nombre}
              </option>
            ))}
          </select>
          <select
            value={idRol}
            onChange={(e) => setIdRol(e.target.value)}
            aria-label="Rol en el proyecto"
            className={`${claseCampo} w-auto`}
          >
            <option value="">Rol…</option>
            {catalogo.rolesProyecto.map((r) => (
              <option key={r.id} value={r.id}>
                {r.nombre}
              </option>
            ))}
          </select>
          <button
            type="submit"
            disabled={!idEstudiante || !idRol || enCurso}
            className={claseBotonSecundario}
          >
            Vincular
          </button>
        </form>
      )}
    </div>
  );
}

function FormularioProyecto({
  idSemillero,
  proyecto,
  enCurso,
  onCancelar,
  onGuardar,
}: {
  idSemillero: number;
  proyecto: ProyectoGestion | null;
  enCurso: boolean;
  onCancelar: () => void;
  onGuardar: (datos: NuevoProyecto) => void;
}) {
  const [titulo, setTitulo] = useState(proyecto?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(proyecto?.descripcion ?? "");
  const [presupuesto, setPresupuesto] = useState(
    String(proyecto?.presupuestoAsignado ?? 0),
  );
  const [fechaInicio, setFechaInicio] = useState(proyecto?.fechaInicio ?? "");
  const [fechaFin, setFechaFin] = useState(proyecto?.fechaFin ?? "");

  const presupuestoNumero = Number(presupuesto);
  const fechasValidas = !fechaInicio || !fechaFin || fechaFin >= fechaInicio;
  const valido =
    titulo.trim() !== "" &&
    descripcion.trim() !== "" &&
    Number.isInteger(presupuestoNumero) &&
    presupuestoNumero >= 0 &&
    fechaInicio !== "" &&
    fechaFin !== "" &&
    fechasValidas;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valido) return;
        onGuardar({
          idSemillero,
          titulo: titulo.trim(),
          descripcion: descripcion.trim(),
          presupuestoAsignado: presupuestoNumero,
          fechaInicio,
          fechaFin,
        });
      }}
      className="panel mb-6 space-y-4 p-6"
    >
      <h3 className="font-display text-sm font-semibold text-foreground">
        {proyecto ? "Editar proyecto" : "Nuevo proyecto"}
      </h3>
      <Campo etiqueta="Título" htmlFor="proy-titulo">
        <input
          id="proy-titulo"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          className={claseCampo}
          required
        />
      </Campo>
      <Campo etiqueta="Descripción" htmlFor="proy-descripcion">
        <textarea
          id="proy-descripcion"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          rows={3}
          className={claseCampo}
          required
        />
      </Campo>
      <div className="grid gap-4 sm:grid-cols-3">
        <Campo etiqueta="Presupuesto asignado (COP)" htmlFor="proy-presupuesto">
          <input
            id="proy-presupuesto"
            type="number"
            min={0}
            step={1}
            value={presupuesto}
            onChange={(e) => setPresupuesto(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
        <Campo etiqueta="Fecha de inicio" htmlFor="proy-inicio">
          <input
            id="proy-inicio"
            type="date"
            value={fechaInicio}
            onChange={(e) => setFechaInicio(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
        <Campo etiqueta="Fecha de fin" htmlFor="proy-fin">
          <input
            id="proy-fin"
            type="date"
            min={fechaInicio || undefined}
            value={fechaFin}
            onChange={(e) => setFechaFin(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
      </div>
      {!fechasValidas && (
        <p className="text-xs text-destructive">
          La fecha de fin no puede ser anterior al inicio.
        </p>
      )}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={!valido || enCurso}
          className={claseBotonPrimario}
        >
          {proyecto ? "Guardar cambios" : "Registrar proyecto"}
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
