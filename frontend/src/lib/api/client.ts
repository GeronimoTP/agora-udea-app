/**
 * Cliente HTTP para el backend Spring Boot de Agora UdeA.
 * Configurar la URL base en la variable de entorno VITE_API_URL.
 */

export const API_BASE_URL: string =
  (import.meta.env["VITE_API_URL"] as string | undefined) ?? "http://localhost:8081/api";

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function authHeaders(): Record<string, string> {
  if (typeof window === "undefined") return {};
  const token = window.localStorage.getItem("agora_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function apiRequest<T>(
  path: string,
  options: { method?: string; body?: unknown; query?: Record<string, unknown> } = {},
): Promise<T> {
  const { method = "GET", body, query } = options;

  const url = new URL(`${API_BASE_URL.replace(/\/$/, "")}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const response = await fetch(url.toString(), {
    method,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...authHeaders(),
    },
    body: body === undefined ? null : JSON.stringify(body),
  });

  if (!response.ok) {
    throw new ApiError(`Error ${response.status} al consultar ${path}`, response.status);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/**
 * Intenta la llamada real al backend; si la API no está disponible en el
 * entorno actual, devuelve datos de demostración para no bloquear la interfaz.
 */
export async function apiRequestWithFallback<T>(
  path: string,
  fallback: T,
  options?: { method?: string; body?: unknown; query?: Record<string, unknown> },
): Promise<T> {
  try {
    return await apiRequest<T>(path, options);
  } catch {
    return fallback;
  }
}
