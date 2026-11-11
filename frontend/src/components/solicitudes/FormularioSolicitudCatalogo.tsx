import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { catalogoSemilleroQuery } from "@/lib/api/queries";
import {
  solicitudesService,
  TIPOS_CATALOGO,
  type TipoElementoCatalogo,
} from "@/lib/api/solicitudes";
import {
  Campo,
  EncabezadoSeccion,
  claseBotonPrimario,
  claseCampo,
} from "@/components/gestion/comunes";
import { useAccionSolicitud } from "./useAccionSolicitud";

/**
 * Estudiantes y profesores proponen un elemento nuevo del catálogo; el administrador decide si
 * se agrega oficialmente.
 */
export function FormularioSolicitudCatalogo() {
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { ejecutar, enCurso } = useAccionSolicitud();
  const [tipo, setTipo] = useState<TipoElementoCatalogo>("HABILIDAD");
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [idArea, setIdArea] = useState("");
  const [justificacion, setJustificacion] = useState("");
  const definicion = TIPOS_CATALOGO.find((t) => t.tipo === tipo)!;
  const esLinea = tipo === "LINEA_INVESTIGACION";
  const valido =
    nombre.trim() !== "" &&
    justificacion.trim().length >= 10 &&
    (!definicion.requiereDescripcion || descripcion.trim() !== "") &&
    (!esLinea || idArea !== "");

  const enviar = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!valido) return;
    const ok = await ejecutar(
      () =>
        solicitudesService.solicitarCatalogo({
          tipo,
          nombre: nombre.trim(),
          justificacion: justificacion.trim(),
          ...(definicion.requiereDescripcion
            ? { descripcion: descripcion.trim() }
            : {}),
          ...(esLinea ? { idAreaEspecialidad: Number(idArea) } : {}),
        }),
      "Solicitud enviada al administrador.",
    );
    if (ok) {
      setNombre("");
      setDescripcion("");
      setJustificacion("");
    }
  };

  return (
    <form onSubmit={enviar} className="panel space-y-4 p-6">
      <EncabezadoSeccion
        titulo="Proponer un elemento del catálogo"
        descripcion="¿No encuentras una habilidad, línea de investigación, área, entidad o tipo de reconocimiento? Propónla y el administrador evaluará si se agrega."
      />
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo etiqueta="Tipo" htmlFor="tipo-solicitud">
          <select
            id="tipo-solicitud"
            value={tipo}
            onChange={(e) => setTipo(e.target.value as TipoElementoCatalogo)}
            className={claseCampo}
          >
            {TIPOS_CATALOGO.map((t) => (
              <option key={t.tipo} value={t.tipo}>
                {t.etiqueta}
              </option>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Nombre" htmlFor="nombre-solicitud">
          <input
            id="nombre-solicitud"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            maxLength={150}
            className={claseCampo}
          />
        </Campo>
      </div>
      {esLinea && (
        <Campo etiqueta="Área de especialidad" htmlFor="area-solicitud">
          <select
            id="area-solicitud"
            value={idArea}
            onChange={(e) => setIdArea(e.target.value)}
            className={claseCampo}
          >
            <option value="">Selecciona un área</option>
            {catalogo.areas.map((a) => (
              <option key={a.id} value={a.id}>
                {a.nombre}
              </option>
            ))}
          </select>
        </Campo>
      )}
      {definicion.requiereDescripcion && (
        <Campo etiqueta="Descripción" htmlFor="descripcion-solicitud">
          <textarea
            id="descripcion-solicitud"
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            rows={2}
            maxLength={1000}
            className={claseCampo}
          />
        </Campo>
      )}
      <Campo
        etiqueta="¿Por qué debería agregarse?"
        htmlFor="justificacion-solicitud"
      >
        <textarea
          id="justificacion-solicitud"
          value={justificacion}
          onChange={(e) => setJustificacion(e.target.value)}
          rows={3}
          maxLength={1000}
          placeholder="Explica para qué se usaría y por qué lo existente no lo cubre."
          className={claseCampo}
        />
      </Campo>
      <button
        type="submit"
        disabled={!valido || enCurso}
        className={claseBotonPrimario}
      >
        Enviar solicitud
      </button>
    </form>
  );
}
