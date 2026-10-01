import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowRight, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { API_BASE_URL } from "@/lib/api/client";

export const Route = createFileRoute("/auth/login")({
  component: LoginPage,
});

function LoginPage() {
  const [loading, setLoading] = useState(false);

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#f3f5f1] px-5 py-12 text-[#18221d]">
      <div className="absolute inset-x-0 top-0 h-2 bg-[#e0a12e]" />
      <div className="absolute -right-28 -top-32 h-96 w-96 rounded-full border-[48px] border-[#dce8dd] opacity-70" />
      <section className="relative w-full max-w-lg border border-[#d8ded8] bg-white p-8 shadow-[0_24px_80px_-48px_rgba(24,34,29,0.45)] sm:p-12">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#52665a]">
          Universidad de Antioquia
        </p>
        <h1 className="mt-5 font-display text-4xl font-semibold leading-tight text-[#173c2e]">
          Agora UdeA
        </h1>
        <p className="mt-3 max-w-sm text-base leading-7 text-[#5a665e]">
          Ingresa con tu cuenta institucional para explorar y gestionar
          semilleros de investigación.
        </p>

        <Button
          type="button"
          className="mt-9 h-12 w-full justify-between rounded-sm bg-[#173c2e] px-5 text-white hover:bg-[#245641]"
          disabled={loading}
          onClick={() => {
            setLoading(true);
            window.location.assign(
              `${API_BASE_URL}/oauth2/authorization/google`,
            );
          }}
        >
          <span className="flex items-center gap-3">
            {loading ? (
              <Loader2 className="h-5 w-5 animate-spin" />
            ) : (
              <GoogleMark />
            )}
            Continuar con Google
          </span>
          <ArrowRight className="h-4 w-4" />
        </Button>

        <p className="mt-5 text-center text-xs leading-5 text-[#68756c]">
          Usa un correo institucional terminado en @udea.edu.co.
        </p>
      </section>
    </main>
  );
}

function GoogleMark() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 48 48">
      <path
        fill="#4285F4"
        d="M43.6 24.5c0-1.4-.1-2.8-.4-4.1H24v7.8h11a9.4 9.4 0 0 1-4.1 6.2v5h6.6c3.9-3.6 6.1-8.8 6.1-14.9Z"
      />
      <path
        fill="#34A853"
        d="M24 44c5.5 0 10.1-1.8 13.5-4.8l-6.6-5c-1.8 1.2-4.1 2-6.9 2-5.3 0-9.8-3.6-11.4-8.4H5.8v5.2A20 20 0 0 0 24 44Z"
      />
      <path
        fill="#FBBC05"
        d="M12.6 27.8a12 12 0 0 1 0-7.6V15H5.8a20 20 0 0 0 0 18Z"
      />
      <path
        fill="#EA4335"
        d="M24 11.8c3 0 5.6 1 7.7 3l5.8-5.8A19.4 19.4 0 0 0 24 4 20 20 0 0 0 5.8 15l6.8 5.2c1.6-4.8 6.1-8.4 11.4-8.4Z"
      />
    </svg>
  );
}
