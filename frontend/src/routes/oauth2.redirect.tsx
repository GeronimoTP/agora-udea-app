import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";

export const Route = createFileRoute("/oauth2/redirect")({
  ssr: false,
  component: OAuthRedirectPage,
});

function OAuthRedirectPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  // El backend envía el token en el fragmento (#token=...&onboarding=...): el navegador no
  // lo manda a ningún servidor. Se lee una sola vez al montar: el efecto borra el fragmento
  // y, si se ejecuta de nuevo (StrictMode en desarrollo), debe seguir viendo el mismo token.
  const [{ token, onboarding }] = useState(() => {
    const parametros = new URLSearchParams(window.location.hash.slice(1));
    return {
      token: parametros.get("token"),
      onboarding: parametros.get("onboarding") === "true",
    };
  });

  useEffect(() => {
    // Se borra el token de la barra de direcciones y del historial
    window.history.replaceState(null, "", window.location.pathname);

    if (!token) {
      navigate({ to: "/auth/login", replace: true });
      return;
    }

    window.localStorage.setItem("access_token", token);
    // Evita mostrar datos en caché de una sesión anterior
    queryClient.clear();

    if (onboarding) {
      navigate({ to: "/auth/select-role", replace: true });
    } else {
      navigate({ to: "/", replace: true });
    }
  }, [navigate, queryClient, token, onboarding]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f3f5f1] px-5 text-[#173c2e]">
      <div className="flex items-center gap-3 text-sm font-medium">
        <Loader2 className="h-5 w-5 animate-spin" />
        Validando tu cuenta institucional...
      </div>
    </main>
  );
}
