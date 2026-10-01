import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { Loader2 } from "lucide-react";

export const Route = createFileRoute("/oauth2/redirect")({
  validateSearch: (search: Record<string, unknown>) => ({
    token: typeof search["token"] === "string" ? search["token"] : "",
    onboarding:
      search["onboarding"] === true || search["onboarding"] === "true",
  }),
  component: OAuthRedirectPage,
});

function OAuthRedirectPage() {
  const navigate = useNavigate();
  const { token, onboarding } = Route.useSearch();

  useEffect(() => {
    if (!token) {
      navigate({ to: "/auth/login", replace: true });
      return;
    }

    window.localStorage.setItem("access_token", token);

    if (onboarding) {
      navigate({ to: "/auth/select-role", replace: true });
    } else {
      navigate({ to: "/", replace: true });
    }
  }, [navigate, onboarding, token]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f3f5f1] px-5 text-[#173c2e]">
      <div className="flex items-center gap-3 text-sm font-medium">
        <Loader2 className="h-5 w-5 animate-spin" />
        Validando tu cuenta institucional...
      </div>
    </main>
  );
}
