import { createFileRoute, useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { BookOpen, Users } from "lucide-react";

export const Route = createFileRoute("/auth/select-role")({
  component: SelectRolePage,
});

interface SearchParams {
  email: string;
}

function SelectRolePage() {
  const navigate = useNavigate();
  const { email } = useSearch({ from: "/auth/select-role" }) as SearchParams;
  const [selectedRole, setSelectedRole] = useState<"PROFESOR" | "ESTUDIANTE" | null>(null);

  if (!email) {
    navigate({ to: "/auth/login" });
    return null;
  }

  const handleContinue = () => {
    if (selectedRole) {
      localStorage.setItem("selectedRole", selectedRole);
      navigate({
        to: "/auth/complete-profile",
        search: { email, isNew: "true", role: selectedRole },
      });
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-2xl rounded-lg bg-white p-8 shadow-lg">
        {/* Header */}
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-gray-900">¿Cuál es tu rol en Agora?</h1>
          <p className="mt-2 text-gray-600">{email}</p>
          <p className="mt-1 text-sm text-gray-500">Selecciona tu perfil para continuar</p>
        </div>

        {/* Role Selection Cards */}
        <div className="grid gap-6 sm:grid-cols-2">
          {/* Profesor Card */}
          <Card
            className={`cursor-pointer border-2 p-6 transition-all ${
              selectedRole === "PROFESOR"
                ? "border-blue-600 bg-blue-50"
                : "border-gray-200 hover:border-blue-300"
            }`}
            onClick={() => setSelectedRole("PROFESOR")}
          >
            <div className="flex flex-col items-center text-center">
              <div
                className={`rounded-full p-3 ${selectedRole === "PROFESOR" ? "bg-blue-200" : "bg-gray-100"}`}
              >
                <BookOpen className="h-8 w-8 text-blue-600" />
              </div>
              <h3 className="mt-4 text-lg font-semibold text-gray-900">Profesor</h3>
              <p className="mt-2 text-sm text-gray-600">
                Crear y gestionar semilleros de investigación, revisar postulaciones y evaluar
                estudiantes
              </p>
              <ul className="mt-4 space-y-1 text-xs text-gray-500">
                <li>✓ Gestionar semilleros</li>
                <li>✓ Revisar postulaciones</li>
                <li>✓ Crear convocatorias</li>
              </ul>
            </div>
          </Card>

          {/* Estudiante Card */}
          <Card
            className={`cursor-pointer border-2 p-6 transition-all ${
              selectedRole === "ESTUDIANTE"
                ? "border-green-600 bg-green-50"
                : "border-gray-200 hover:border-green-300"
            }`}
            onClick={() => setSelectedRole("ESTUDIANTE")}
          >
            <div className="flex flex-col items-center text-center">
              <div
                className={`rounded-full p-3 ${selectedRole === "ESTUDIANTE" ? "bg-green-200" : "bg-gray-100"}`}
              >
                <Users className="h-8 w-8 text-green-600" />
              </div>
              <h3 className="mt-4 text-lg font-semibold text-gray-900">Estudiante</h3>
              <p className="mt-2 text-sm text-gray-600">
                Explorar semilleros, postularse a convocatorias y ver recomendaciones personalizadas
              </p>
              <ul className="mt-4 space-y-1 text-xs text-gray-500">
                <li>✓ Ver semilleros</li>
                <li>✓ Postularse</li>
                <li>✓ Ver compatibilidad</li>
              </ul>
            </div>
          </Card>
        </div>

        {/* Buttons */}
        <div className="mt-8 flex gap-4">
          <Button
            variant="outline"
            className="flex-1"
            onClick={() => navigate({ to: "/auth/login" })}
          >
            Atrás
          </Button>
          <Button
            className="flex-1"
            disabled={!selectedRole}
            onClick={handleContinue}
          >
            Continuar como {selectedRole === "PROFESOR" ? "Profesor" : selectedRole === "ESTUDIANTE" ? "Estudiante" : ""}
          </Button>
        </div>
      </div>
    </div>
  );
}
