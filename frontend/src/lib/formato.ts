/**
 * Formato de fechas para mostrar en pantalla (es-CO).
 *
 * Las fechas sin hora del backend (LocalDate, "AAAA-MM-DD") se interpretan en hora local:
 * `new Date("2026-11-28")` las tomaría como medianoche UTC y en Colombia (UTC−5) mostraría
 * el día anterior.
 */
export function formatearFecha(
  valor: string | null | undefined,
  {
    siVacio = "—",
    mes = "short",
  }: { siVacio?: string; mes?: "short" | "long" } = {},
) {
  if (!valor) return siVacio;
  return aFecha(valor).toLocaleDateString("es-CO", {
    year: "numeric",
    month: mes,
    day: "numeric",
  });
}

/** Fecha y hora corta, para marcas de tiempo (ZonedDateTime). */
export function formatearFechaHora(valor: string) {
  return aFecha(valor).toLocaleString("es-CO", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function aFecha(valor: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(valor)
    ? new Date(`${valor}T00:00:00`)
    : new Date(valor);
}
