import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";

/**
 * Ejecuta una respuesta a una solicitud (aprobar, rechazar, solicitar): muestra el resultado y
 * refresca los avisos del menú, los semilleros y los catálogos que la respuesta puede cambiar.
 */
export function useAccionSolicitud() {
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
        queryClient.invalidateQueries({ queryKey: ["solicitudes"] }),
        queryClient.invalidateQueries({ queryKey: ["semilleros"] }),
        queryClient.invalidateQueries({ queryKey: ["catalogo"] }),
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
