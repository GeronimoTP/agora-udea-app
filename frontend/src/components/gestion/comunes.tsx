import { useQueryClient } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { EstadoBadge } from "@/components/common/EstadoBadge";
import { claveGestion } from "@/lib/api/gestion";

export const claseCampo =
  "w-full rounded-sm border border-input bg-surface p-2.5 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring disabled:opacity-60";

export const claseBotonPrimario =
  "rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary-hover disabled:opacity-50";

export const claseBotonSecundario =
  "rounded-sm border border-border-strong px-3 py-1.5 text-sm font-medium text-foreground transition-colors hover:bg-secondary disabled:opacity-50";

export const claseBotonPeligro =
  "rounded-sm border border-destructive/40 px-3 py-1.5 text-sm font-medium text-destructive transition-colors hover:bg-destructive/10 disabled:opacity-50";

export function Campo({
  etiqueta,
  htmlFor,
  ayuda,
  children,
  className,
}: {
  etiqueta: string;
  htmlFor?: string | undefined;
  ayuda?: string | undefined;
  children: ReactNode;
  className?: string | undefined;
}) {
  return (
    <div className={className}>
      <label htmlFor={htmlFor} className="label-caps mb-1.5 block">
        {etiqueta}
      </label>
      {children}
      {ayuda ? (
        <p className="mt-1 text-xs text-muted-foreground">{ayuda}</p>
      ) : null}
    </div>
  );
}

export function EncabezadoSeccion({
  titulo,
  descripcion,
  accion,
}: {
  titulo: string;
  descripcion?: string;
  accion?: ReactNode;
}) {
  return (
    <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div className="max-w-2xl">
        <h2 className="font-display text-base font-semibold text-foreground">
          {titulo}
        </h2>
        {descripcion ? (
          <p className="mt-1 text-sm text-muted-foreground">{descripcion}</p>
        ) : null}
      </div>
      {accion}
    </div>
  );
}

export function Vacio({ children }: { children: ReactNode }) {
  return <p className="panel p-5 text-sm text-muted-foreground">{children}</p>;
}

/**
 * Botón que pide confirmación antes de ejecutar una acción destructiva o irreversible.
 */
export function Confirmar({
  titulo,
  descripcion,
  textoAccion,
  onConfirmar,
  children,
}: {
  titulo: string;
  descripcion: string;
  textoAccion: string;
  onConfirmar: () => void;
  children: ReactNode;
}) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>{children}</AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{titulo}</AlertDialogTitle>
          <AlertDialogDescription>{descripcion}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Volver</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirmar}>
            {textoAccion}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

/**
 * Ejecuta una mutación de gestión: muestra el resultado y refresca los datos del
 * semillero (gestión, directorio, detalle, convocatorias públicas y sesión).
 */
export function useAccionGestion(idSemillero: number) {
  const queryClient = useQueryClient();
  const [enCurso, setEnCurso] = useState(false);

  const ejecutar = async (
    accion: () => Promise<unknown>,
    mensajeExito: string,
  ): Promise<boolean> => {
    setEnCurso(true);
    try {
      await accion();
      toast.success(mensajeExito);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claveGestion(idSemillero) }),
        queryClient.invalidateQueries({ queryKey: ["semilleros"] }),
        queryClient.invalidateQueries({ queryKey: ["semillero", idSemillero] }),
        queryClient.invalidateQueries({ queryKey: ["convocatorias"] }),
        queryClient.invalidateQueries({ queryKey: ["proyectos", idSemillero] }),
        queryClient.invalidateQueries({
          queryKey: ["publicaciones", idSemillero],
        }),
      ]);
      return true;
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "No se pudo completar la acción.",
      );
      return false;
    } finally {
      setEnCurso(false);
    }
  };

  return { ejecutar, enCurso };
}

// Estados que EstadoBadge sabe pintar con su tono; el resto se muestra con su nombre real
const ESTADOS_CON_TONO = new Set([
  "ACTIVO",
  "EN_PAUSA",
  "INACTIVO",
  "ABIERTA",
  "CERRADA",
]);

export function BadgeEstado({ nombre }: { nombre: string | null | undefined }) {
  if (!nombre) return null;
  const clave = nombre
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toUpperCase()
    .trim()
    .replace(/\s+/g, "_");
  return <EstadoBadge estado={ESTADOS_CON_TONO.has(clave) ? clave : nombre} />;
}

export function formatearFecha(valor?: string | null) {
  if (!valor) return "—";
  // Las fechas LocalDate llegan como "AAAA-MM-DD": se interpretan en hora local
  const fecha = /^\d{4}-\d{2}-\d{2}$/.test(valor)
    ? new Date(`${valor}T00:00:00`)
    : new Date(valor);
  return fecha.toLocaleDateString("es-CO", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export const fechaHoy = () => {
  const ahora = new Date();
  const desfase = ahora.getTimezoneOffset() * 60000;
  return new Date(ahora.getTime() - desfase).toISOString().slice(0, 10);
};

export function normalizar(texto: string) {
  return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLocaleLowerCase().trim();
}
