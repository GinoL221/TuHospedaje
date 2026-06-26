import { createContext, useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { get, post } from "../services/api";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  // Kept in a ref (updated on every render) instead of a useEffect dependency
  // so the auth:unauthorized listener below doesn't get torn down and
  // re-registered on every route change — it always reads the current route
  // without needing to resubscribe.
  const locationRef = useRef(location);
  locationRef.current = location;
  const [{ user, loading }, setAuth] = useState({ user: null, loading: true });

  const logout = async () => {
    await post("/auth/logout");
    setAuth({ user: null, loading: false });
  };

  useEffect(() => {
    let cancelled = false;

    get("/auth/me")
      .then((data) => {
        if (!cancelled) setAuth({ user: data, loading: false });
      })
      .catch(() => {
        if (!cancelled) setAuth({ user: null, loading: false });
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      setAuth({ user: null, loading: false });
      navigate("/login", {
        state: {
          from: locationRef.current,
          message: "Tu sesión expiró. Volvé a iniciar sesión para continuar.",
        },
      });
    };

    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () => {
      window.removeEventListener("auth:unauthorized", handleUnauthorized);
    };
  }, [navigate]);

  const login = async (email, password) => {
    const data = await post("/auth/login", { email, password });
    setAuth({ user: data, loading: false });
  };

  const register = async (firstName, lastName, email, password) => {
    const data = await post("/auth/register", {
      firstName,
      lastName,
      email,
      password,
    });
    setAuth({ user: data, loading: false });
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export { AuthContext };
