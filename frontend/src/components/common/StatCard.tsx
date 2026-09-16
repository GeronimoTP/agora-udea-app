import type { LucideIcon } from "lucide-react";

export function StatCard({
  label,
  value,
  unit,
  hint,
  icon: Icon,
}: {
  label: string;
  value: string | number;
  unit?: string;
  hint?: string;
  icon?: LucideIcon;
}) {
  return (
    <div className="panel p-5">
      <div className="flex items-start justify-between gap-3">
        <p className="label-caps">{label}</p>
        {Icon ? (
          <Icon className="size-4 text-muted-foreground" strokeWidth={1.75} aria-hidden />
        ) : null}
      </div>
      <p className="mt-3 font-display text-3xl font-semibold tabular-nums text-primary">
        {value}
        {unit ? <span className="ml-1 text-base font-medium text-muted-foreground">{unit}</span> : null}
      </p>
      {hint ? <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{hint}</p> : null}
    </div>
  );
}
