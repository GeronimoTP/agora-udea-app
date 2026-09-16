import { cn } from "@/lib/utils";

export function MatchScore({
  porcentaje,
  className,
  compact = false,
}: {
  porcentaje: number;
  className?: string;
  compact?: boolean;
}) {
  const valor = Math.max(0, Math.min(100, porcentaje));

  return (
    <div className={cn("w-full", className)}>
      <div className="flex items-baseline justify-between gap-2">
        {compact ? null : <span className="label-caps">Afinidad</span>}
        <span className="font-display text-sm font-semibold tabular-nums text-primary">
          {valor.toFixed(1)}%
        </span>
      </div>
      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-sm bg-secondary">
        <div
          className="h-full bg-accent"
          style={{ width: `${valor}%` }}
          role="progressbar"
          aria-valuenow={Math.round(valor)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Porcentaje de afinidad"
        />
      </div>
    </div>
  );
}
