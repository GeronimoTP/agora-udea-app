import { useSuspenseQuery } from "@tanstack/react-query";
import { ExternalLink, Plus } from "lucide-react";
import { useState } from "react";
import {
  catalogoGestionQuery,
  gestionQueries,
  gestionService,
  type NuevoReconocimiento,
  type ReconocimientoGestion,
} from "@/lib/api/gestion";
import type { ItemCatalogo } from "@/lib/api/services";
import {
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

export function ReconocimientosGestion({
  idSemillero,
}: {
  idSemillero: number;
}) {
  const { data: reconocimientos } = useSuspenseQuery(
    gestionQueries.reconocimientos(idSemillero),
  );
  const { data: catalogo } = useSuspenseQuery(catalogoGestionQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [editando, setEditando] = useState<
    ReconocimientoGestion | "nuevo" | null
  >(null);

  return (
    <section>
      <EncabezadoSeccion
        titulo="Reconocimientos"
        descripcion="Premios, distinciones y certificaciones obtenidas por el semillero."
        accion={
          <button
            type="button"
            onClick={() => setEditando("nuevo")}
            className={`inline-flex items-center gap-1.5 ${claseBotonPrimario}`}
          >
            <Plus className="size-4" strokeWidth={1.75} aria-hidden />
            Nuevo reconocimiento
          </button>
        }
      />

      {editando !== null && (
        <FormularioReconocimiento
          key={editando === "nuevo" ? "nuevo" : editando.id}
          idSemillero={idSemillero}
          reconocimiento={editando === "nuevo" ? null : editando}
          tipos={catalogo.tiposReconocimiento}
          entidades={catalogo.entidadesOtorgantes}
          enCurso={enCurso}
          onCancelar={() => setEditando(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(
              () =>
                editando === "nuevo"
                  ? gestionService.crearReconocimiento(datos)
                  : gestionService.actualizarReconocimiento(editando.id, datos),
              editando === "nuevo"
                ? "Reconocimiento registrado."
                : "Reconocimiento actualizado.",
            );
            if (ok) setEditando(null);
          }}
        />
      )}

      {reconocimientos.length === 0 ? (
        <Vacio>El semillero aún no tiene reconocimientos registrados.</Vacio>
      ) : (
        <ul className="space-y-3">
          {reconocimientos.map((r) => (
            <li key={r.id} className="panel p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="max-w-2xl">
                  <p className="label-caps">
                    {r.tipoReconocimiento} · {r.entidadOtorgante}
                  </p>
                  <h3 className="mt-1 font-display text-base font-semibold text-foreground">
                    {r.titulo}
                  </h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {r.descripcion}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Obtenido el {formatearFecha(r.fechaObtencion)}
                  </p>
                </div>
                <a
                  href={r.urlCertificado}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1 text-sm font-medium text-accent underline-offset-4 hover:underline"
                >
                  Certificado
                  <ExternalLink
                    className="size-3.5"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                </a>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setEditando(r)}
                  disabled={enCurso}
                  className={claseBotonSecundario}
                >
                  Editar
                </button>
                <Confirmar
                  titulo="¿Eliminar el reconocimiento?"
                  descripcion="Esta acción no se puede deshacer."
                  textoAccion="Eliminar"
                  onConfirmar={() =>
                    void ejecutar(
                      () => gestionService.eliminarReconocimiento(r.id),
                      "Reconocimiento eliminado.",
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
          ))}
        </ul>
      )}
    </section>
  );
}

function FormularioReconocimiento({
  idSemillero,
  reconocimiento,
  tipos,
  entidades,
  enCurso,
  onCancelar,
  onGuardar,
}: {
  idSemillero: number;
  reconocimiento: ReconocimientoGestion | null;
  tipos: ItemCatalogo[];
  entidades: ItemCatalogo[];
  enCurso: boolean;
  onCancelar: () => void;
  onGuardar: (datos: NuevoReconocimiento) => void;
}) {
  const hoy = fechaHoy();
  const [idTipo, setIdTipo] = useState(
    String(reconocimiento?.idTipoReconocimiento ?? ""),
  );
  const [idEntidad, setIdEntidad] = useState(
    String(reconocimiento?.idEntidadOtorgante ?? ""),
  );
  const [titulo, setTitulo] = useState(reconocimiento?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(
    reconocimiento?.descripcion ?? "",
  );
  const [fecha, setFecha] = useState(reconocimiento?.fechaObtencion ?? hoy);
  const [url, setUrl] = useState(reconocimiento?.urlCertificado ?? "");

  const urlValida = /^https?:\/\/.+/.test(url);
  const fechaValida = fecha !== "" && fecha <= hoy;
  const valido =
    idTipo !== "" &&
    idEntidad !== "" &&
    titulo.trim() !== "" &&
    descripcion.trim() !== "" &&
    fechaValida &&
    urlValida;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valido) return;
        onGuardar({
          idSemillero,
          idTipoReconocimiento: Number(idTipo),
          idEntidadOtorgante: Number(idEntidad),
          titulo: titulo.trim(),
          descripcion: descripcion.trim(),
          fechaObtencion: fecha,
          urlCertificado: url.trim(),
        });
      }}
      className="panel mb-6 space-y-4 p-6"
    >
      <h3 className="font-display text-sm font-semibold text-foreground">
        {reconocimiento ? "Editar reconocimiento" : "Nuevo reconocimiento"}
      </h3>
      <div className="grid gap-4 sm:grid-cols-3">
        <Campo etiqueta="Tipo" htmlFor="rec-tipo">
          <select
            id="rec-tipo"
            value={idTipo}
            onChange={(e) => setIdTipo(e.target.value)}
            className={claseCampo}
            required
          >
            <option value="">Selecciona…</option>
            {tipos.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nombre}
              </option>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Entidad otorgante" htmlFor="rec-entidad">
          <select
            id="rec-entidad"
            value={idEntidad}
            onChange={(e) => setIdEntidad(e.target.value)}
            className={claseCampo}
            required
          >
            <option value="">Selecciona…</option>
            {entidades.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nombre}
              </option>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Fecha de obtención" htmlFor="rec-fecha">
          <input
            id="rec-fecha"
            type="date"
            max={hoy}
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
      </div>
      <Campo etiqueta="Título" htmlFor="rec-titulo">
        <input
          id="rec-titulo"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          className={claseCampo}
          required
        />
      </Campo>
      <Campo etiqueta="Descripción" htmlFor="rec-descripcion">
        <textarea
          id="rec-descripcion"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          rows={3}
          className={claseCampo}
          required
        />
      </Campo>
      <Campo etiqueta="URL del certificado" htmlFor="rec-url">
        <input
          id="rec-url"
          type="url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://"
          className={claseCampo}
          required
        />
        {url && !urlValida && (
          <p className="mt-1 text-xs text-destructive">
            Debe comenzar con http:// o https://
          </p>
        )}
      </Campo>
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={!valido || enCurso}
          className={claseBotonPrimario}
        >
          {reconocimiento ? "Guardar cambios" : "Registrar reconocimiento"}
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
