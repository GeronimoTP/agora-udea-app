/** Fila con etiqueta, cantidad, porcentaje del total y una barra proporcional. */
export function BarraProporcion({
  etiqueta,
  cantidad,
  total,
}: {
  etiqueta: string;
  cantidad: number;
  total: number;
}) {
  const porcentaje = total > 0 ? (cantidad / total) * 100 : 0;
  return (
    <li>
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="font-medium text-foreground">{etiqueta}</span>
        <span className="tabular-nums text-muted-foreground">
          {cantidad} · {porcentaje.toFixed(1)}%
        </span>
      </div>
      <div className="mt-1.5 h-2 w-full overflow-hidden rounded-sm bg-secondary">
        <div
          className="h-full bg-primary"
          style={{ width: `${porcentaje}%` }}
        />
      </div>
    </li>
  );
}
