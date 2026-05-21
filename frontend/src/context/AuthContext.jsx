import { createContext, useState } from "react";
import { jwtDecode } from "jwt-decode";
import { post } from "../services/api";

const AuthContext = createContext();

function getInitialAuth() {
  const savedToken = localStorage.getItem("token");
  if (savedToken) {
    try {
      const decoded = jwtDecode(savedToken);
      if (decoded.exp * 1000 > Date.now()) {
        return {
          token: savedToken,
          user: {
            firstName: decoded.firstName,
            lastName: decoded.lastName,
            email: decoded.sub,
            role: decoded.role,
            imageUrl: decoded.imageUrl,
          },
        };
      }
      localStorage.removeItem("token");
    } catch {
      localStorage.removeItem("token");
    }
  }
  return {
    token: null,
    user: null,
  };
}

export function AuthProvider({ children }) {
  const [{ token, user }, setAuth] = useState(getInitialAuth);
  const login = async (email, password) => {
    const data = await post("/auth/login", { email, password });
    const decoded = jwtDecode(data.token);
    const newUser = {
      firstName: decoded.firstName,
      lastName: decoded.lastName,
      email: decoded.sub,
      role: decoded.role,
      imageUrl: decoded.imageUrl,
    };
    setAuth({ token: data.token, user: newUser });
    localStorage.setItem("token", data.token);
  };

  const register = async (firstName, lastName, email, password) => {
    const data = await post("/auth/register", {
      firstName,
      lastName,
      email,
      password,
    });
    const decoded = jwtDecode(data.token);
    const newUser = {
      firstName: decoded.firstName,
      lastName: decoded.lastName,
      email: decoded.sub,
      role: decoded.role,
      imageUrl: decoded.imageUrl,
    };
    setAuth({ token: data.token, user: newUser });
    localStorage.setItem("token", data.token);
  };

  const logout = () => {
    setAuth({ token: null, user: null });
    localStorage.removeItem("token");
  };
  
  return (
    <AuthContext.Provider value={{ token, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export { AuthContext };