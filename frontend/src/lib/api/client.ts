/**
 * Cliente HTTP para el backend Spring Boot de Agora UdeA.
 * 
 * Configuración:
 * - URL base desde VITE_API_URL (.env)
 * - Los endpoints incluyen "/api" en sus rutas
 * - Manejo automático de JWT en Authorization header
 * - Timeout configurable
 */

export const API_BASE_URL: string =
  (import.meta.env["VITE_API_URL"] as string | undefined) ?? "http://localhost:8080";

export const API_TIMEOUT: number = parseInt(
  (import.meta.env["VITE_API_TIMEOUT"] as string | undefined) ?? "10000"
);

export const DEBUG_API: boolean =
  (import.meta.env["VITE_DEBUG_API"] as string | undefined) === "true";

export class ApiError extends Error {
  status: number;
  path: string;
  timestamp: Date;

  constructor(message: string, status: number, path: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.path = path;
    this.timestamp = new Date();
  }
}

/**
 * Obtiene el token JWT del localStorage
 */
function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem("access_token");
}

/**
 * Construye los headers con autenticación
 */
function getHeaders(additionalHeaders?: Record<string, string>): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...additionalHeaders,
  };

  const token = getAuthToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return headers;
}

/**
 * Ejecuta una petición HTTP contra el backend.
 * 
 * @param path - Ruta del endpoint (ej. "/api/estudiantes", "/api/semilleros/123")
 * @param options - Opciones de la petición (método, body, query params)
 * @returns Promesa con la respuesta parseada
 * 
 * @example
 * const estudiantes = await apiRequest<EstudianteDTO[]>("/api/estudiantes");
 */
export async function apiRequest<T>(
  path: string,
  options: {
    method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
    body?: unknown;
    query?: Record<string, unknown>;
    headers?: Record<string, string>;
  } = {},
): Promise<T> {
  const { method = "GET", body, query, headers: additionalHeaders } = options;

  // Construir URL completa
  const url = new URL(`${API_BASE_URL}${path}`);

  // Añadir query parameters
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const fullUrl = url.toString();

  if (DEBUG_API) {
    console.log(`[API] ${method} ${fullUrl}`, body);
  }

  try {
    const response = await fetch(fullUrl, {
      method,
      headers: getHeaders(additionalHeaders),
      body: body === undefined ? null : JSON.stringify(body),
      credentials: "include", // Enviar cookies si existen
      signal: AbortSignal.timeout(API_TIMEOUT),
    });

    // Manejo de errores HTTP
    if (!response.ok) {
      let errorMessage = `Error ${response.status} en ${path}`;
      let errorData: unknown = null;

      // Intentar parsear el cuerpo de error del backend
      try {
        errorData = await response.json();
        if (typeof errorData === "object" && errorData !== null) {
          const err = errorData as Record<string, unknown>;
          errorMessage = (err["message"] as string) || errorMessage;
        }
      } catch {
        // Si no se puede parsear, usar el mensaje genérico
      }

      if (DEBUG_API) {
        console.error(`[API ERROR] ${errorMessage}`, errorData);
      }

      throw new ApiError(errorMessage, response.status, path);
    }

    // Códigos de estado sin contenido
    if (response.status === 204) {
      return undefined as T;
    }

    // Parsear y retornar la respuesta
    const data = (await response.json()) as T;

    if (DEBUG_API) {
      console.log(`[API RESPONSE] ${method} ${path}`, data);
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof TypeError && (error.message.includes("network") || error.message.includes("fetch"))) {
      throw new ApiError(
        `No se pudo conectar al servidor. Asegúrate de que el backend está ejecutándose en ${API_BASE_URL}`,
        0,
        path
      );
    }

    throw new ApiError(`Error desconocido: ${String(error)}`, 0, path);
  }
}

/**
 * Ejecuta una petición con fallback a datos de demostración.
 * Útil para desarrollo cuando el backend no está disponible.
 * 
 * @param path - Ruta del endpoint
 * @param fallback - Datos por defecto si la petición falla
 * @param options - Opciones de la petición
 * @returns Promesa con la respuesta o los datos de fallback
 */
export async function apiRequestWithFallback<T>(
  path: string,
  fallback: T,
  options?: {
    method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
    body?: unknown;
    query?: Record<string, unknown>;
    headers?: Record<string, string>;
  },
): Promise<T> {
  try {
    return await apiRequest<T>(path, options);
  } catch (error) {
    console.warn(`[API FALLBACK] Usando datos de demostración para ${path}`, error);
    return fallback;
  }
}

export default { apiRequest, apiRequestWithFallback, API_BASE_URL, API_TIMEOUT };