import { createContext, useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { get, post, bootstrapCsrf } from "../services/api";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const locationRef = useRef(location);
  const [{ user, loading }, setAuth] = useState({ user: null, loading: true });
  const [logoutError, setLogoutError] = useState(null);
  const authGeneration = useRef(0);

  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  const logout = async () => {
    setLogoutError(null);
    try {
      await post("/auth/logout");
      setAuth({ user: null, loading: false });
    } catch (error) {
      setLogoutError(error.message);
      throw error;
    }
  };

  useEffect(() => {
    let cancelled = false;
    const generation = ++authGeneration.current;

    get("/auth/me")
      .then((data) => bootstrapCsrf().then(() => data))
      .then((data) => {
        if (!cancelled && generation === authGeneration.current) {
          setAuth({ user: data, loading: false });
        }
      })
      .catch((error) => {
        if (!cancelled && generation === authGeneration.current) {
          console.error("Auth bootstrap failed on mount:", error);
          setAuth({ user: null, loading: false });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      setAuth({ user: null, loading: false });
      if (locationRef.current.pathname === "/login") return;
      navigate("/login", {
        state: {
          from: locationRef.current,
          message: "Tu sesión expiró. Volvé a iniciar sesión para continuar.",
        },
      });
    };

    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("auth:unauthorized", handleUnauthorized);
  }, [navigate]);

  const login = async (email, password) => {
    const generation = ++authGeneration.current;
    const data = await post("/auth/login", { email, password });
    await bootstrapCsrf();
    if (generation === authGeneration.current) setAuth({ user: data, loading: false });
  };

  const register = async (firstName, lastName, email, password) => {
    const generation = ++authGeneration.current;
    const data = await post("/auth/register", { firstName, lastName, email, password });
    try {
      await bootstrapCsrf();
    } catch {
      // The account already exists server-side at this point (post() succeeded),
      // so re-submitting this form would hit the duplicate-email error and hide
      // that. Point the user at login instead, which is safe to retry.
      throw new Error(
        "Tu cuenta se creó correctamente, pero no pudimos iniciar sesión automáticamente. Iniciá sesión con tus credenciales.",
      );
    }
    if (generation === authGeneration.current) setAuth({ user: data, loading: false });
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, logoutError }}>
      {children}
    </AuthContext.Provider>
  );
}

export { AuthContext };
