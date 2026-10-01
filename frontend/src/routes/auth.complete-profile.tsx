import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { apiRequest } from "@/lib/api/client";

export const Route = createFileRoute("/auth/complete-profile")({
  validateSearch: (search: Record<string, unknown>) => ({
    role:
      search["role"] === "PROFESOR"
        ? "PROFESOR"
        : search["role"] === "ESTUDIANTE"
          ? "ESTUDIANTE"
          : "",
  }),
  component: CompleteProfilePage,
});

interface Programa {
  id: number;
  nombre: string;
  nombreFacultad: string | null;
}

interface LineaInvestigacion {
  id: number;
  nombre: string;
}

interface AreaEspecialidad {
  id: number;
  nombre: string;
}

function CompleteProfilePage() {
  const navigate = useNavigate();
  const { role } = Route.useSearch();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [programas, setProgramas] = useState<Programa[]>([]);
  const [lineas, setLineas] = useState<LineaInvestigacion[]>([]);
  const [areas, setAreas] = useState<AreaEspecialidad[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  // Form state
  const [formData, setFormData] = useState({
    idPrograma: "",
    semestre: "1", // Solo estudiantes
    lineasInvestigacion: [] as number[], // Solo estudiantes
    areasEspecialidad: [] as number[], // Solo profesores
  });

  // Cargar datos iniciales
  useEffect(() => {
    if (!window.localStorage.getItem("access_token")) {
      navigate({ to: "/auth/login", replace: true });
      return;
    }

    if (role !== "ESTUDIANTE" && role !== "PROFESOR") {
      navigate({ to: "/auth/select-role", replace: true });
      return;
    }

    const loadData = async () => {
      try {
        const [programasRes, lineasRes, areasRes] = await Promise.all([
          apiRequest<Programa[]>("/api/catalogos/programas-academicos"),
          apiRequest<LineaInvestigacion[]>("/api/lineas-investigacion"),
          apiRequest<AreaEspecialidad[]>("/api/areas-especialidad"),
        ]);

        setProgramas(programasRes || []);
        setLineas(lineasRes || []);
        setAreas(areasRes || []);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "No se pudieron cargar los datos del perfil.",
        );
        console.error("Error cargando datos:", err);
      } finally {
        setLoadingData(false);
      }
    };

    loadData();
  }, [navigate, role]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const isEstudiante = role === "ESTUDIANTE";
      let resultado: { accessToken: string };

      if (isEstudiante) {
        resultado = await apiRequest<{ accessToken: string }>(
          "/api/auth/onboarding/estudiante",
          {
            method: "POST",
            body: {
              idPrograma: parseInt(formData.idPrograma),
              semestre: parseInt(formData.semestre),
              idLineasInvestigacion: formData.lineasInvestigacion,
            },
          },
        );
      } else {
        resultado = await apiRequest<{ accessToken: string }>(
          "/api/auth/onboarding/profesor",
          {
            method: "POST",
            body: {
              idPrograma: parseInt(formData.idPrograma),
              idAreasEspecialidad: formData.areasEspecialidad,
            },
          },
        );
      }

      window.localStorage.setItem("access_token", resultado.accessToken);
      navigate({ to: "/" });
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Error al guardar el perfil. Intenta nuevamente.",
      );
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const isEstudiante = role === "ESTUDIANTE";

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 py-8">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow-lg">
        {/* Header */}
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-gray-900">
            Completa tu perfil
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            Perfil de {isEstudiante ? "estudiante" : "profesor"}
          </p>
        </div>

        {loadingData ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Programa Académico */}
            <div>
              <label
                htmlFor="programa"
                className="block text-sm font-medium text-gray-700"
              >
                Programa Académico
              </label>
              <select
                id="programa"
                value={formData.idPrograma}
                onChange={(e) =>
                  setFormData({ ...formData, idPrograma: e.target.value })
                }
                required
                className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                disabled={loading}
              >
                <option value="">Selecciona un programa</option>
                {programas.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nombre}
                  </option>
                ))}
              </select>
            </div>

            {/* Estudiante: Semestre */}
            {isEstudiante && (
              <div>
                <label
                  htmlFor="semestre"
                  className="block text-sm font-medium text-gray-700"
                >
                  Semestre
                </label>
                <select
                  id="semestre"
                  value={formData.semestre}
                  onChange={(e) =>
                    setFormData({ ...formData, semestre: e.target.value })
                  }
                  className="mt-2 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  disabled={loading}
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((s) => (
                    <option key={s} value={s}>
                      Semestre {s}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Estudiante: Líneas de Investigación */}
            {isEstudiante && (
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Líneas de Investigación (selecciona mínimo 1)
                </label>
                <div className="mt-2 max-h-48 overflow-y-auto space-y-2 rounded-md border border-gray-300 p-3">
                  {lineas.map((linea) => (
                    <label key={linea.id} className="flex items-center">
                      <input
                        type="checkbox"
                        checked={formData.lineasInvestigacion.includes(
                          linea.id,
                        )}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormData({
                              ...formData,
                              lineasInvestigacion: [
                                ...formData.lineasInvestigacion,
                                linea.id,
                              ],
                            });
                          } else {
                            setFormData({
                              ...formData,
                              lineasInvestigacion:
                                formData.lineasInvestigacion.filter(
                                  (id) => id !== linea.id,
                                ),
                            });
                          }
                        }}
                        disabled={loading}
                        className="h-4 w-4 rounded border-gray-300 text-blue-600"
                      />
                      <span className="ml-2 text-sm text-gray-700">
                        {linea.nombre}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Profesor: Áreas de Especialidad */}
            {!isEstudiante && (
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Áreas de Especialidad (selecciona mínimo 1)
                </label>
                <div className="mt-2 max-h-48 overflow-y-auto space-y-2 rounded-md border border-gray-300 p-3">
                  {areas.map((area) => (
                    <label key={area.id} className="flex items-center">
                      <input
                        type="checkbox"
                        checked={formData.areasEspecialidad.includes(area.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormData({
                              ...formData,
                              areasEspecialidad: [
                                ...formData.areasEspecialidad,
                                area.id,
                              ],
                            });
                          } else {
                            setFormData({
                              ...formData,
                              areasEspecialidad:
                                formData.areasEspecialidad.filter(
                                  (id) => id !== area.id,
                                ),
                            });
                          }
                        }}
                        disabled={loading}
                        className="h-4 w-4 rounded border-gray-300 text-blue-600"
                      />
                      <span className="ml-2 text-sm text-gray-700">
                        {area.nombre}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {error && (
              <div className="rounded bg-red-50 p-3 text-sm text-red-700">
                {error}
              </div>
            )}

            {/* Submit Button */}
            <Button
              type="submit"
              className="w-full mt-6"
              disabled={
                loading ||
                !formData.idPrograma ||
                (isEstudiante && formData.lineasInvestigacion.length === 0) ||
                (!isEstudiante && formData.areasEspecialidad.length === 0)
              }
            >
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Guardando...
                </>
              ) : (
                "Completar Registro"
              )}
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
