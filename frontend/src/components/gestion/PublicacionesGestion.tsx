import { useSuspenseQuery } from "@tanstack/react-query";
import { ExternalLink, Plus, X } from "lucide-react";
import { useState } from "react";
import {
  catalogoGestionQuery,
  gestionQueries,
  gestionService,
  type NuevaPublicacion,
  type PublicacionGestion,
} from "@/lib/api/gestion";
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

export function PublicacionesGestion({ idSemillero }: { idSemillero: number }) {
  const { data: publicaciones } = useSuspenseQuery(
    gestionQueries.publicaciones(idSemillero),
  );
  const { data: proyectos } = useSuspenseQuery(
    gestionQueries.proyectos(idSemillero),
  );
  const { data: semillero } = useSuspenseQuery(
    gestionQueries.semillero(idSemillero),
  );
  const { data: integrantes } = useSuspenseQuery(
    gestionQueries.integrantes(idSemillero),
  );
  const { data: tutores } = useSuspenseQuery(
    gestionQueries.tutores(idSemillero),
  );
  const { data: catalogo } = useSuspenseQuery(catalogoGestionQuery());
  const { ejecutar, enCurso } = useAccionGestion(idSemillero);
  const [editando, setEditando] = useState<PublicacionGestion | "nueva" | null>(
    null,
  );

  // Pueden ser autores: el líder, los integrantes activos y los tutores (por id de usuario)
  const personas = [
    {
      idUsuario: semillero.idUsuarioLider,
      nombre: `${semillero.nombreEstudianteLider} (líder)`,
    },
    ...integrantes
      .filter((i) => i.fechaSalida === null)
      .map((i) => ({ idUsuario: i.idUsuario, nombre: i.nombreEstudiante })),
    ...tutores.map((t) => ({
      idUsuario: t.idUsuario,
      nombre: `${t.nombreProfesor} (tutor)`,
    })),
  ];

  return (
    <section>
      <EncabezadoSeccion
        titulo="Producción académica"
        descripcion="Registra las publicaciones de los proyectos del semillero y sus autores."
        accion={
          <button
            type="button"
            onClick={() => setEditando("nueva")}
            disabled={proyectos.length === 0}
            title={
              proyectos.length === 0
                ? "Primero registra un proyecto"
                : undefined
            }
            className={`inline-flex items-center gap-1.5 ${claseBotonPrimario}`}
          >
            <Plus className="size-4" strokeWidth={1.75} aria-hidden />
            Nueva publicación
          </button>
        }
      />

      {proyectos.length === 0 && (
        <p className="mb-4 text-sm text-muted-foreground">
          Las publicaciones pertenecen a un proyecto: registra primero un
          proyecto.
        </p>
      )}

      {editando !== null && (
        <FormularioPublicacion
          key={editando === "nueva" ? "nueva" : editando.id}
          publicacion={editando === "nueva" ? null : editando}
          proyectos={proyectos.map((p) => ({ id: p.id, nombre: p.titulo }))}
          enCurso={enCurso}
          onCancelar={() => setEditando(null)}
          onGuardar={async (datos) => {
            const ok = await ejecutar(
              () =>
                editando === "nueva"
                  ? gestionService.crearPublicacion(datos)
                  : gestionService.actualizarPublicacion(editando.id, datos),
              editando === "nueva"
                ? "Publicación registrada."
                : "Publicación actualizada.",
            );
            if (ok) setEditando(null);
          }}
        />
      )}

      {publicaciones.length === 0 ? (
        <Vacio>El semillero aún no tiene publicaciones registradas.</Vacio>
      ) : (
        <ul className="space-y-3">
          {publicaciones.map((pub) => (
            <li key={pub.id} className="panel p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="max-w-2xl">
                  <p className="label-caps">{pub.nombreProyecto}</p>
                  <h3 className="mt-1 font-display text-base font-semibold text-foreground">
                    {pub.titulo}
                  </h3>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Publicada el {formatearFecha(pub.fechaPublicacion)}
                  </p>
                </div>
                <a
                  href={pub.urlDocumento}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1 text-sm font-medium text-accent underline-offset-4 hover:underline"
                >
                  Ver documento
                  <ExternalLink
                    className="size-3.5"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                </a>
              </div>

              <div className="mt-4 border-t border-border pt-4">
                <p className="label-caps mb-2">Autores</p>
                {pub.autoresDetalle.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Sin autores registrados.
                  </p>
                ) : (
                  <ul className="flex flex-wrap gap-2">
                    {pub.autoresDetalle.map((a) => (
                      <li key={a.idUsuario}>
                        <button
                          type="button"
                          disabled={enCurso}
                          onClick={() =>
                            void ejecutar(
                              () =>
                                gestionService.quitarAutor(pub.id, a.idUsuario),
                              `${a.nombre} ya no figura como autor.`,
                            )
                          }
                          aria-label={`Quitar a ${a.nombre} como autor`}
                          className="inline-flex items-center gap-1.5 rounded-sm border border-border bg-secondary px-2.5 py-1 text-sm text-secondary-foreground hover:bg-secondary/70"
                        >
                          {a.nombre} · {a.rolAutoria}
                          <X
                            className="size-3.5"
                            strokeWidth={1.75}
                            aria-hidden
                          />
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <AgregarAutor
                  personas={personas.filter(
                    (p) =>
                      !pub.autoresDetalle.some(
                        (a) => a.idUsuario === p.idUsuario,
                      ),
                  )}
                  roles={catalogo.rolesAutor}
                  enCurso={enCurso}
                  onAgregar={(idUsuario, idRol) =>
                    ejecutar(
                      () =>
                        gestionService.agregarAutor(pub.id, idUsuario, idRol),
                      "Autor agregado.",
                    )
                  }
                />
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setEditando(pub)}
                  disabled={enCurso}
                  className={claseBotonSecundario}
                >
                  Editar
                </button>
                <Confirmar
                  titulo="¿Eliminar la publicación?"
                  descripcion="Se eliminarán también sus registros de autoría."
                  textoAccion="Eliminar"
                  onConfirmar={() =>
                    void ejecutar(
                      () => gestionService.eliminarPublicacion(pub.id),
                      "Publicación eliminada.",
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

function AgregarAutor({
  personas,
  roles,
  enCurso,
  onAgregar,
}: {
  personas: { idUsuario: number; nombre: string }[];
  roles: { id: number; nombre: string }[];
  enCurso: boolean;
  onAgregar: (idUsuario: number, idRol: number) => Promise<boolean>;
}) {
  const [idUsuario, setIdUsuario] = useState("");
  const [idRol, setIdRol] = useState("");
  if (personas.length === 0) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!idUsuario || !idRol) return;
        void onAgregar(Number(idUsuario), Number(idRol)).then((ok) => {
          if (ok) {
            setIdUsuario("");
            setIdRol("");
          }
        });
      }}
      className="mt-3 flex flex-wrap items-end gap-2"
    >
      <select
        value={idUsuario}
        onChange={(e) => setIdUsuario(e.target.value)}
        aria-label="Autor"
        className={`${claseCampo} w-auto`}
      >
        <option value="">Autor…</option>
        {personas.map((p) => (
          <option key={p.idUsuario} value={p.idUsuario}>
            {p.nombre}
          </option>
        ))}
      </select>
      <select
        value={idRol}
        onChange={(e) => setIdRol(e.target.value)}
        aria-label="Rol de autoría"
        className={`${claseCampo} w-auto`}
      >
        <option value="">Rol de autoría…</option>
        {roles.map((r) => (
          <option key={r.id} value={r.id}>
            {r.nombre}
          </option>
        ))}
      </select>
      <button
        type="submit"
        disabled={!idUsuario || !idRol || enCurso}
        className={claseBotonSecundario}
      >
        Agregar autor
      </button>
    </form>
  );
}

function FormularioPublicacion({
  publicacion,
  proyectos,
  enCurso,
  onCancelar,
  onGuardar,
}: {
  publicacion: PublicacionGestion | null;
  proyectos: { id: number; nombre: string }[];
  enCurso: boolean;
  onCancelar: () => void;
  onGuardar: (datos: NuevaPublicacion) => void;
}) {
  const [idProyecto, setIdProyecto] = useState(
    String(publicacion?.idProyecto ?? proyectos[0]?.id ?? ""),
  );
  const [titulo, setTitulo] = useState(publicacion?.titulo ?? "");
  const [url, setUrl] = useState(publicacion?.urlDocumento ?? "");
  const [fecha, setFecha] = useState(
    publicacion?.fechaPublicacion ?? fechaHoy(),
  );

  const urlValida = /^https?:\/\/.+/.test(url);
  const valido =
    idProyecto !== "" && titulo.trim() !== "" && urlValida && fecha !== "";

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valido) return;
        onGuardar({
          idProyecto: Number(idProyecto),
          titulo: titulo.trim(),
          urlDocumento: url.trim(),
          fechaPublicacion: fecha,
        });
      }}
      className="panel mb-6 space-y-4 p-6"
    >
      <h3 className="font-display text-sm font-semibold text-foreground">
        {publicacion ? "Editar publicación" : "Nueva publicación"}
      </h3>
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo etiqueta="Proyecto" htmlFor="pub-proyecto">
          <select
            id="pub-proyecto"
            value={idProyecto}
            onChange={(e) => setIdProyecto(e.target.value)}
            className={claseCampo}
          >
            {proyectos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Fecha de publicación" htmlFor="pub-fecha">
          <input
            id="pub-fecha"
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className={claseCampo}
            required
          />
        </Campo>
      </div>
      <Campo etiqueta="Título" htmlFor="pub-titulo">
        <input
          id="pub-titulo"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          className={claseCampo}
          required
        />
      </Campo>
      <Campo etiqueta="URL del documento" htmlFor="pub-url">
        <input
          id="pub-url"
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
          {publicacion ? "Guardar cambios" : "Registrar publicación"}
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
