import { cn } from "@/lib/utils";
import type { EstadoConvocatoria, EstadoPostulacion, EstadoSemillero } from "@/types/api";

const etiquetas: Record<string, string> = {
  ACTIVO: "Activo",
  EN_PAUSA: "En pausa",
  PENDIENTE_APROBACION: "Pendiente de aprobación",
  INACTIVO: "Inactivo",
  ABIERTA: "Convocatoria abierta",
  CERRADA: "Cerrada",
  BORRADOR: "Borrador",
  ENVIADA: "Enviada",
  EN_REVISION: "En revisión",
  PRE_APROBADA: "Pre-aprobada",
  ACEPTADA_POR_ESTUDIANTE: "Aceptada por el estudiante",
  RECHAZADA_POR_LIDER: "Rechazada por el líder",
  RECHAZADA_POR_ESTUDIANTE: "Declinada por el estudiante",
  EXPIRADA: "Expirada",
  CANCELADA_EN_CASCADA: "Cancelada en cascada",
};

const tonos: Record<string, string> = {
  ACTIVO: "border-success/30 bg-success/10 text-success",
  ABIERTA: "border-success/30 bg-success/10 text-success",
  ACEPTADA_POR_ESTUDIANTE: "border-success/30 bg-success/10 text-success",
  PRE_APROBADA: "border-accent/30 bg-accent/10 text-accent",
  EN_REVISION: "border-warning/40 bg-warning/10 text-warning-foreground",
  ENVIADA: "border-border-strong bg-secondary text-secondary-foreground",
  PENDIENTE_APROBACION: "border-warning/40 bg-warning/10 text-warning-foreground",
  EN_PAUSA: "border-border-strong bg-secondary text-muted-foreground",
  BORRADOR: "border-border-strong bg-secondary text-muted-foreground",
  CERRADA: "border-border-strong bg-secondary text-muted-foreground",
  INACTIVO: "border-border-strong bg-secondary text-muted-foreground",
  RECHAZADA_POR_LIDER: "border-destructive/30 bg-destructive/10 text-destructive",
  RECHAZADA_POR_ESTUDIANTE: "border-destructive/30 bg-destructive/10 text-destructive",
  EXPIRADA: "border-destructive/30 bg-destructive/10 text-destructive",
  CANCELADA_EN_CASCADA: "border-destructive/30 bg-destructive/10 text-destructive",
};

export function EstadoBadge({
  estado,
  className,
}: {
  estado: EstadoPostulacion | EstadoConvocatoria | EstadoSemillero | string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-sm border px-2 py-0.5 text-xs font-medium",
        tonos[estado] ?? "border-border-strong bg-secondary text-secondary-foreground",
        className,
      )}
    >
      {etiquetas[estado] ?? estado}
    </span>
  );
}
