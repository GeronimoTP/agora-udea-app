import { redirect } from "@tanstack/react-router";
import { ApiError, apiRequest } from "./client";
import type { UsuarioSesion } from "@/types/api";

/**
 * Sesión para el `loader` de una ruta protegida:
 * - sin token, al login;
 * - con cuenta pero sin perfil (403/404 en /api/usuarios/me), a elegir rol.
 * Se consulta siempre al backend (no a la caché) porque el rol y los semilleros
 * liderados cambian con las acciones del usuario.
 */
export async function requerirSesion(): Promise<UsuarioSesion> {
  if (!localStorage.getItem("access_token")) {
    throw redirect({ to: "/auth/login" });
  }
  try {
    return await apiRequest<UsuarioSesion>("/api/usuarios/me");
  } catch (error) {
    if (error instanceof ApiError && [403, 404].includes(error.status)) {
      throw redirect({ to: "/auth/select-role" });
    }
    throw error;
  }
}
