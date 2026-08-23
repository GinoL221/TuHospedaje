import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Building2,
  Tag,
  Star,
  ShieldCheck,
  Users,
  LogOut,
  Home,
  CalendarCheck,
} from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import AdminDashboard from "./AdminDashboard";
import AdminLodgings from "./AdminLodgings";
import AdminCategories from "./AdminCategories";
import AdminFeatures from "./AdminFeatures";
import AdminUsers from "./AdminUsers";
import AdminPolicies from "./AdminPolicies";
import AdminReservations from "./AdminReservations";
import "./Admin.css";

const NAV_ITEMS = [
  { key: "dashboard",  label: "Dashboard",       icon: LayoutDashboard },
  { key: "lodgings",   label: "Alojamientos",    icon: Building2 },
  { key: "categories", label: "Categorías",      icon: Tag },
  { key: "features",   label: "Características", icon: Star },
  { key: "policies",   label: "Políticas",       icon: ShieldCheck },
  { key: "users",      label: "Usuarios",        icon: Users },
  { key: "reservations", label: "Reservas",      icon: CalendarCheck },
];

export default function Admin() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState("dashboard");
  const [isMobile, setIsMobile] = useState(false);
  const mobileHeadingRef = useRef(null);

  useEffect(() => {
    const check = () => {
      const isTouch = "ontouchstart" in window || navigator.maxTouchPoints > 0;
      setIsMobile(isTouch && window.innerWidth <= 1024);
    };
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  // Focus the heading so a screen reader announces the unavailable state
  // immediately when this route is opened directly (deep link/refresh).
  useEffect(() => {
    if (isMobile) mobileHeadingRef.current?.focus();
  }, [isMobile]);

  if (isMobile) {
    return (
      <div className="admin-mobile-block" role="status">
        <h2 tabIndex={-1} ref={mobileHeadingRef}>
          Panel no disponible en móvil
        </h2>
        <p>Accedé desde una computadora.</p>
      </div>
    );
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-brand">
          <span className="admin-sidebar-brand-text">TuHospedaje</span>
          <span className="admin-sidebar-brand-sub">Admin</span>
        </div>
        <nav className="admin-nav">
          {NAV_ITEMS.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              className={"admin-nav-item" + (tab === key ? " active" : "")}
              data-testid={`admin-nav-${key}`}
              onClick={() => setTab(key)}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <div className="admin-main">
        <div className="admin-topbar">
          <span className="admin-topbar-title">Panel de Administración</span>
          <div className="admin-topbar-user">
            <button className="admin-btn-logout" onClick={() => navigate("/")}>
              <Home size={16} />
              Ir al inicio
            </button>
            <span className="admin-topbar-name">{user?.firstName}</span>
            <button className="admin-btn-logout" onClick={logout}>
              <LogOut size={16} />
              Salir
            </button>
          </div>
        </div>

        <div className="admin-content">
          {tab === "dashboard"  && <AdminDashboard onTabChange={setTab} />}
          {tab === "lodgings"   && <AdminLodgings />}
          {tab === "categories" && <AdminCategories />}
          {tab === "features"   && <AdminFeatures />}
          {tab === "policies"   && <AdminPolicies />}
          {tab === "users"      && <AdminUsers />}
          {tab === "reservations" && <AdminReservations />}
        </div>
      </div>
    </div>
  );
}
