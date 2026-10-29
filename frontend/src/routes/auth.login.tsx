import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import {
  ArrowRight,
  CalendarClock,
  FlaskConical,
  Library,
  Loader2,
  Target,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { API_BASE_URL, ApiError, apiRequest } from "@/lib/api/client";

// Códigos que envían el backend (OAuth2LoginFailureHandler) y el cliente HTTP (sesión vencida)
const mensajesError: Record<string, string> = {
  invalid_google_account:
    "Debes ingresar con una cuenta Google institucional @udea.edu.co.",
  account_conflict:
    "Este correo ya está asociado a otra cuenta de Google. Contacta al administrador.",
  login_fallido: "No se pudo completar el inicio de sesión con Google.",
  sesion_expirada: "Tu sesión expiró. Vuelve a ingresar.",
};

const beneficios = [
  {
    icon: Library,
    titulo: "Explora semilleros",
    detalle:
      "Líneas de investigación, proyectos y publicaciones de cada semillero.",
  },
  {
    icon: CalendarClock,
    titulo: "Postúlate a convocatorias",
    detalle: "Sigue tus postulaciones y responde las ofertas de admisión.",
  },
  {
    icon: Target,
    titulo: "Recomendaciones a tu medida",
    detalle: "Semilleros afines a tu perfil según tus intereses y habilidades.",
  },
];

export const Route = createFileRoute("/auth/login")({
  validateSearch: (search: Record<string, unknown>): { error?: string } =>
    typeof search["error"] === "string" ? { error: search["error"] } : {},
  component: LoginPage,
});

function LoginPage() {
  const [loading, setLoading] = useState(false);
  const { error } = Route.useSearch();
  const mensajeError = error
    ? (mensajesError[error] ?? mensajesError["login_fallido"])
    : null;

  return (
    <main className="grid min-h-screen bg-[#f3f5f1] text-[#18221d] lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)]">
      {/* Panel institucional: solo en pantallas grandes */}
      <aside className="relative hidden overflow-hidden bg-[#173c2e] px-12 py-14 text-white lg:flex lg:flex-col lg:justify-between xl:px-20">
        <div className="absolute inset-x-0 top-0 h-2 bg-[#e0a12e]" />
        <div className="absolute -bottom-40 -right-40 h-[28rem] w-[28rem] rounded-full border-[64px] border-white/5" />
        <div className="absolute -left-24 top-1/3 h-64 w-64 rounded-full border-[40px] border-white/5" />

        <p className="relative text-xs font-semibold uppercase tracking-[0.18em] text-[#e0a12e]">
          Universidad de Antioquia
        </p>

        <div className="relative max-w-lg">
          <h1 className="font-display text-5xl font-semibold leading-tight">
            Agora UdeA
          </h1>
          <p className="mt-4 text-lg leading-8 text-white/80">
            La plataforma para encontrar, gestionar y hacer crecer los
            semilleros de investigación de la universidad.
          </p>
          <ul className="mt-10 space-y-6">
            {beneficios.map(({ icon: Icon, titulo, detalle }) => (
              <li key={titulo} className="flex gap-4">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-sm bg-white/10">
                  <Icon
                    className="h-5 w-5 text-[#e0a12e]"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                </span>
                <div>
                  <p className="font-semibold">{titulo}</p>
                  <p className="mt-1 text-sm leading-6 text-white/70">
                    {detalle}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-white/60">
          Vicerrectoría de Investigación · Sistema de Gestión de Semilleros
        </p>
      </aside>

      <section className="relative flex items-center justify-center px-5 py-12 sm:px-10">
        <div className="absolute inset-x-0 top-0 h-2 bg-[#e0a12e] lg:hidden" />
        <div className="w-full max-w-md">
          {/* En pantallas pequeñas no hay panel institucional: la marca va aquí */}
          <div className="lg:hidden">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#52665a]">
              Universidad de Antioquia
            </p>
            <p className="mt-4 font-display text-4xl font-semibold leading-tight text-[#173c2e]">
              Agora UdeA
            </p>
          </div>

          <h2 className="mt-8 font-display text-2xl font-semibold text-[#173c2e] lg:mt-0">
            Ingresa a tu cuenta
          </h2>
          <p className="mt-2 text-base leading-7 text-[#5a665e]">
            Usa tu cuenta institucional para explorar y gestionar semilleros de
            investigación.
          </p>

          {mensajeError && (
            <p
              role="alert"
              className="mt-6 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
            >
              {mensajeError}
            </p>
          )}

          <Button
            type="button"
            className="mt-8 h-12 w-full justify-between rounded-sm bg-[#173c2e] px-5 text-white hover:bg-[#245641]"
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

          <LoginPruebas />
        </div>
      </section>
    </main>
  );
}

// Solo aparece si el backend tiene APP_LOGIN_PRUEBAS_CLAVE configurada (usuarios precargados "seed-")
function LoginPruebas() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [correo, setCorreo] = useState("");
  const [clave, setClave] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: estado } = useQuery({
    queryKey: ["login-pruebas"],
    queryFn: () =>
      apiRequest<{ habilitado: boolean }>("/api/auth/login-pruebas"),
    retry: false,
  });

  if (!estado?.habilitado) return null;

  const enviar = async (event: FormEvent) => {
    event.preventDefault();
    setEnviando(true);
    setError(null);
    try {
      const { accessToken } = await apiRequest<{ accessToken: string }>(
        "/api/auth/login-pruebas",
        { method: "POST", body: { correo, clave } },
      );
      window.localStorage.setItem("access_token", accessToken);
      // Evita mostrar datos en caché del usuario anterior
      queryClient.clear();
      navigate({ to: "/", replace: true });
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? "Correo o clave de pruebas inválidos."
          : err instanceof ApiError && err.status === 422
            ? "El usuario de prueba no tiene perfil."
            : "No se pudo iniciar sesión de prueba.",
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <form
      onSubmit={enviar}
      className="mt-8 space-y-3 border-t border-dashed border-[#d8ded8] pt-6"
    >
      <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.14em] text-[#a06d12]">
        <FlaskConical className="h-4 w-4" aria-hidden />
        Acceso de pruebas
      </p>
      <Input
        type="email"
        placeholder="Correo del usuario de prueba"
        autoComplete="username"
        value={correo}
        onChange={(e) => setCorreo(e.target.value)}
        required
      />
      <Input
        type="password"
        placeholder="Clave de pruebas"
        autoComplete="current-password"
        value={clave}
        onChange={(e) => setClave(e.target.value)}
        required
      />
      {error && <p className="text-xs text-red-700">{error}</p>}
      <Button
        type="submit"
        variant="outline"
        className="h-10 w-full rounded-sm"
        disabled={enviando}
      >
        {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
        Entrar como usuario de prueba
      </Button>
    </form>
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
