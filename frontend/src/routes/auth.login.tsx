import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Loader2 } from "lucide-react";
import { apiRequest } from "@/lib/api/client";

export const Route = createFileRoute("/auth/login")({
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      // Verificar si el usuario existe
      const response = await apiRequest<{ exists: boolean; id?: number; rol?: string }>(
        "/api/auth/check-email",
        {
          method: "POST",
          body: { email },
        }
      );

      if (response.exists) {
        // Usuario ya existe, redirigir a dashboard
        localStorage.setItem("email", email);
        navigate({ to: "/auth/complete-profile", search: { email, isNew: "false" } });
      } else {
        // Usuario nuevo, ir a seleccionar rol
        localStorage.setItem("email", email);
        navigate({ to: "/auth/select-role", search: { email } });
      }
    } catch (err) {
      setError("Error al verificar el correo. Intenta nuevamente.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow-lg">
        {/* Logo/Header */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900">Agora UdeA</h1>
          <p className="mt-2 text-sm text-gray-600">
            Plataforma de Gestión de Semilleros de Investigación
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
              Correo Institucional
            </label>
            <Input
              id="email"
              type="email"
              placeholder="tu@udea.edu.co"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="mt-2"
              disabled={loading}
            />
          </div>

          {error && <div className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</div>}

          <Button type="submit" className="w-full" disabled={loading || !email.trim()}>
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Verificando...
              </>
            ) : (
              "Continuar"
            )}
          </Button>
        </form>

        {/* OAuth Alternative */}
        <div className="mt-6 space-y-3">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="bg-white px-2 text-gray-500">O continúa con</span>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            className="w-full"
            onClick={() => {
              // Redirigir a Google OAuth del backend
              window.location.href = `${import.meta.env.VITE_API_URL}/oauth2/authorization/google`;
            }}
          >
            <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="currentColor"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="currentColor"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="currentColor"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
            Google
          </Button>
        </div>

        {/* Footer */}
        <p className="mt-6 text-center text-xs text-gray-500">
          Al continuar, aceptas nuestros términos de servicio y política de privacidad.
        </p>
      </div>
    </div>
  );
}
