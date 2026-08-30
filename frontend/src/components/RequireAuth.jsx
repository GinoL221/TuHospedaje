import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import RouteLoadingFallback from "./RouteLoadingFallback";

export default function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <RouteLoadingFallback />;

  if (!user) {
    return (
      <Navigate
        to="/login"
        state={{
          from: location,
          message: "Necesitás iniciar sesión para continuar. Si no tenés cuenta, podés registrarte.",
        }}
        replace
      />
    );
  }

  return <Outlet />;
}
