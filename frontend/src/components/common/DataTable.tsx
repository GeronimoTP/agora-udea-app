import type { ReactNode } from "react";

export interface Columna<T> {
  header: string;
  cell: (row: T) => ReactNode;
  align?: "left" | "right";
  width?: string;
}

export function DataTable<T>({
  columns,
  rows,
  getRowKey,
  emptyMessage = "No hay registros para mostrar.",
}: {
  columns: Columna<T>[];
  rows: T[];
  getRowKey: (row: T) => string | number;
  emptyMessage?: string;
}) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-border bg-secondary/60">
            {columns.map((col) => (
              <th
                key={col.header}
                scope="col"
                className={`px-4 py-2.5 text-xs font-semibold uppercase tracking-wider text-muted-foreground ${
                  col.align === "right" ? "text-right" : "text-left"
                }`}
                style={col.width ? { width: col.width } : undefined}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-8 text-center text-sm text-muted-foreground"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr
                key={getRowKey(row)}
                className="border-b border-border last:border-0 hover:bg-secondary/40"
              >
                {columns.map((col) => (
                  <td
                    key={col.header}
                    className={`px-4 py-3 align-middle ${
                      col.align === "right" ? "text-right tabular-nums" : "text-left"
                    }`}
                  >
                    {col.cell(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
