import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function RequireAdmin() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;

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

  if (user.role !== "ADMIN") return <Navigate to="/unauthorized" replace />;

  return <Outlet />;
}
