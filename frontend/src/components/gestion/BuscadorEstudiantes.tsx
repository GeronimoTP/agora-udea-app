import { Search } from "lucide-react";
import { useMemo, useState, type ReactNode } from "react";
import type { EstudianteResumen, ProgramaCatalogo } from "@/lib/api/services";
import { claseCampo, normalizar } from "./comunes";

const MAX_RESULTADOS = 30;

/**
 * Lista de estudiantes registrados con filtros por nombre/correo y programa.
 * Lo que se hace con cada estudiante lo decide `renderAccion`.
 */
export function BuscadorEstudiantes({
  estudiantes,
  programas,
  excluir = [],
  renderAccion,
}: {
  estudiantes: EstudianteResumen[];
  programas: ProgramaCatalogo[];
  excluir?: number[];
  renderAccion: (estudiante: EstudianteResumen) => ReactNode;
}) {
  const [busqueda, setBusqueda] = useState("");
  const [programa, setPrograma] = useState("");

  const resultados = useMemo(() => {
    const texto = normalizar(busqueda);
    return estudiantes.filter(
      (e) =>
        !excluir.includes(e.id) &&
        (!texto ||
          normalizar(e.nombreCompleto).includes(texto) ||
          normalizar(e.email).includes(texto)) &&
        (!programa || e.nombrePrograma === programa),
    );
  }, [estudiantes, excluir, busqueda, programa]);

  return (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            strokeWidth={1.75}
            aria-hidden
          />
          <input
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre o correo"
            aria-label="Buscar estudiante por nombre o correo"
            className={`${claseCampo} pl-8`}
          />
        </div>
        <select
          value={programa}
          onChange={(e) => setPrograma(e.target.value)}
          aria-label="Filtrar por programa"
          className={claseCampo}
        >
          <option value="">Todos los programas</option>
          {programas.map((p) => (
            <option key={p.id} value={p.nombre}>
              {p.nombre}
            </option>
          ))}
        </select>
      </div>

      <ul className="max-h-80 divide-y divide-border overflow-y-auto rounded-sm border border-border">
        {resultados.length === 0 && (
          <li className="p-4 text-sm text-muted-foreground">
            Ningún estudiante disponible coincide con los filtros.
          </li>
        )}
        {resultados.slice(0, MAX_RESULTADOS).map((e) => (
          <li key={e.id} className="flex items-start justify-between gap-3 p-3">
            <div className="min-w-0">
              <p className="text-sm font-medium text-foreground">
                {e.nombreCompleto}
              </p>
              <p className="text-xs text-muted-foreground">
                {e.email}
                {e.nombrePrograma ? ` · ${e.nombrePrograma}` : ""} · Semestre{" "}
                {e.semestre}
              </p>
            </div>
            <div className="shrink-0">{renderAccion(e)}</div>
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
