import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { apiRequest } from "@/lib/api/client";
import type { ItemCatalogo } from "@/lib/api/catalogos";
import { catalogoSemilleroQuery } from "@/lib/api/queries";
import {
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

// Endpoint de cada catálogo: el GET lista y el POST crea (solo administrador)
const RUTAS: Record<TipoElementoCatalogo, string> = {
  HABILIDAD: "/api/habilidades",
  LINEA_INVESTIGACION: "/api/lineas-investigacion",
  AREA_ESPECIALIDAD: "/api/areas-especialidad",
  ENTIDAD_OTORGANTE: "/api/catalogos/entidades-otorgantes",
  TIPO_RECONOCIMIENTO: "/api/catalogos/tipos-reconocimiento",
};

/** El administrador agrega elementos al catálogo directamente, sin solicitud previa. */
export function ElementosCatalogoAdmin() {
  const [tipo, setTipo] = useState<TipoElementoCatalogo>("HABILIDAD");
  const definicion = TIPOS_CATALOGO.find((t) => t.tipo === tipo)!;
  const { data: catalogo } = useSuspenseQuery(catalogoSemilleroQuery());
  const { data: existentes = [] } = useQuery({
    queryKey: ["catalogo", "administracion", tipo],
    queryFn: () => apiRequest<ItemCatalogo[]>(RUTAS[tipo]),
  });
  const { ejecutar, enCurso } = useAccionSolicitud();
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [idArea, setIdArea] = useState("");
  const esLinea = tipo === "LINEA_INVESTIGACION";
  const valido =
    nombre.trim() !== "" &&
    (!definicion.requiereDescripcion || descripcion.trim() !== "") &&
    (!esLinea || idArea !== "");

  const crear = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!valido) return;
    const ok = await ejecutar(
      () =>
        apiRequest(RUTAS[tipo], {
          method: "POST",
          body: {
            nombre: nombre.trim(),
            ...(definicion.requiereDescripcion
              ? { descripcion: descripcion.trim() }
              : {}),
            ...(esLinea ? { idAreaEspecialidad: Number(idArea) } : {}),
          },
        }),
      `"${nombre.trim()}" agregado al catálogo.`,
    );
    if (ok) {
      setNombre("");
      setDescripcion("");
    }
  };

  return (
    <section className="grid gap-8 lg:grid-cols-2">
      <form onSubmit={crear} className="panel space-y-4 p-6">
        <EncabezadoSeccion
          titulo="Agregar al catálogo"
          descripcion="Crea un elemento directamente. Lo que proponen estudiantes y profesores llega como solicitud."
        />
        <Campo etiqueta="Tipo" htmlFor="tipo-elemento">
          <select
            id="tipo-elemento"
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
        <Campo etiqueta="Nombre" htmlFor="nombre-elemento">
          <input
            id="nombre-elemento"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            maxLength={150}
            className={claseCampo}
          />
        </Campo>
        {esLinea && (
          <Campo etiqueta="Área de especialidad" htmlFor="area-elemento">
            <select
              id="area-elemento"
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
          <Campo etiqueta="Descripción" htmlFor="descripcion-elemento">
            <textarea
              id="descripcion-elemento"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              rows={3}
              maxLength={1000}
              className={claseCampo}
            />
          </Campo>
        )}
        <button
          type="submit"
          disabled={!valido || enCurso}
          className={claseBotonPrimario}
        >
          Agregar
        </button>
      </form>

      <div className="panel p-6">
        <p className="label-caps mb-3">
          {definicion.etiqueta}: {existentes.length} en el catálogo
        </p>
        <ul className="flex max-h-96 flex-wrap gap-1.5 overflow-y-auto">
          {[...existentes]
            .sort((a, b) => a.nombre.localeCompare(b.nombre))
            .map((e) => (
              <li
                key={e.id}
                className="rounded-sm border border-border bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
              >
                {e.nombre}
              </li>
            ))}
        </ul>
      </div>
    </section>
  );
}
