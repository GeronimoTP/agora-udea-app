import { Search } from "lucide-react";
import { useMemo, useState, type ReactNode } from "react";
import type {
  ItemCatalogo,
  ProgramaCatalogo,
  Profesor,
} from "@/lib/api/services";
import { claseCampo, normalizar } from "./comunes";

const MAX_RESULTADOS = 30;

/**
 * Lista de profesores registrados con filtros por nombre/correo, facultad, programa y
 * área de especialidad. Lo que se hace con cada profesor lo decide `renderAccion`.
 */
export function BuscadorProfesores({
  profesores,
  programas,
  facultades,
  areas,
  excluir = [],
  renderAccion,
}: {
  profesores: Profesor[];
  programas: ProgramaCatalogo[];
  facultades: ItemCatalogo[];
  areas: ItemCatalogo[];
  excluir?: number[];
  renderAccion: (profesor: Profesor) => ReactNode;
}) {
  const [busqueda, setBusqueda] = useState("");
  const [idFacultad, setIdFacultad] = useState("");
  const [idPrograma, setIdPrograma] = useState("");
  const [area, setArea] = useState("");

  const facultad = facultades.find((f) => String(f.id) === idFacultad);
  const programasDeFacultad = facultad
    ? programas.filter((p) => p.nombreFacultad === facultad.nombre)
    : programas;

  const resultados = useMemo(() => {
    const texto = normalizar(busqueda);
    return profesores.filter(
      (p) =>
        !excluir.includes(p.id) &&
        (!texto ||
          normalizar(p.nombreCompleto).includes(texto) ||
          normalizar(p.email).includes(texto)) &&
        (!idFacultad || String(p.idFacultad) === idFacultad) &&
        (!idPrograma || String(p.idPrograma) === idPrograma) &&
        (!area || p.areasEspecialidad.includes(area)),
    );
  }, [profesores, excluir, busqueda, idFacultad, idPrograma, area]);

  return (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="relative sm:col-span-2">
          <Search
            className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            strokeWidth={1.75}
            aria-hidden
          />
          <input
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre o correo"
            aria-label="Buscar profesor por nombre o correo"
            className={`${claseCampo} pl-8`}
          />
        </div>
        <select
          value={idFacultad}
          onChange={(e) => {
            setIdFacultad(e.target.value);
            setIdPrograma("");
          }}
          aria-label="Filtrar por facultad"
          className={claseCampo}
        >
          <option value="">Todas las facultades</option>
          {facultades.map((f) => (
            <option key={f.id} value={f.id}>
              {f.nombre}
            </option>
          ))}
        </select>
        <select
          value={idPrograma}
          onChange={(e) => setIdPrograma(e.target.value)}
          aria-label="Filtrar por programa"
          className={claseCampo}
        >
          <option value="">Todos los programas</option>
          {programasDeFacultad.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nombre}
            </option>
          ))}
        </select>
        <select
          value={area}
          onChange={(e) => setArea(e.target.value)}
          aria-label="Filtrar por área de especialidad"
          className={`${claseCampo} sm:col-span-2`}
        >
          <option value="">Todas las áreas de especialidad</option>
          {areas.map((a) => (
            <option key={a.id} value={a.nombre}>
              {a.nombre}
            </option>
          ))}
        </select>
      </div>

      <ul className="max-h-96 divide-y divide-border overflow-y-auto rounded-sm border border-border">
        {resultados.length === 0 && (
          <li className="p-4 text-sm text-muted-foreground">
            Ningún profesor coincide con los filtros.
          </li>
        )}
        {resultados.slice(0, MAX_RESULTADOS).map((p) => (
          <li key={p.id} className="flex items-start justify-between gap-3 p-3">
            <div className="min-w-0">
              <p className="text-sm font-medium text-foreground">
                {p.nombreCompleto}
              </p>
              <p className="text-xs text-muted-foreground">
                {p.email}
                {p.nombrePrograma ? ` · ${p.nombrePrograma}` : ""}
              </p>
              {p.areasEspecialidad.length > 0 && (
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {p.areasEspecialidad.join(" · ")}
                </p>
              )}
            </div>
            <div className="shrink-0">{renderAccion(p)}</div>
          </li>
        ))}
        {resultados.length > MAX_RESULTADOS && (
          <li className="p-3 text-xs text-muted-foreground">
            Mostrando {MAX_RESULTADOS} de {resultados.length}. Usa los filtros
            para acotar la búsqueda.
          </li>
        )}
      </ul>
    </div>
  );
}
