import { useState, useEffect } from "react";
import { Building2, Tag, Star, Users } from "lucide-react";
import { get } from "../../services/api";

const STATS = [
  { key: "lodgings",   label: "Alojamientos",    icon: Building2, endpoint: "/lodgings",   tab: "lodgings" },
  { key: "categories", label: "Categorías",       icon: Tag,       endpoint: "/categories", tab: "categories" },
  { key: "features",   label: "Características",  icon: Star,      endpoint: "/features",   tab: "features" },
  { key: "users",      label: "Usuarios",         icon: Users,     endpoint: "/users",      tab: "users" },
];

export default function AdminDashboard({ onTabChange }) {
  const [counts, setCounts] = useState({});
  const [recentLodgings, setRecentLodgings] = useState([]);

  useEffect(() => {
    STATS.forEach(({ key, endpoint }) => {
      get(endpoint)
        .then((data) => {
          const arr = Array.isArray(data) ? data : data?.content ?? data?.lodgings ?? [];
          setCounts((prev) => ({ ...prev, [key]: arr.length }));
        })
        .catch(() => setCounts((prev) => ({ ...prev, [key]: "—" })));
    });

    get("/lodgings?page=0&size=4")
      .then((data) => {
        const arr = Array.isArray(data) ? data : data?.lodgings ?? [];
        setRecentLodgings(arr.slice(0, 4));
      })
      .catch(() => {});
  }, []);

  return (
    <div>
      <div className="admin-section-header">
        <h2>Dashboard</h2>
      </div>

      <div className="dashboard-grid">
        {STATS.map(({ key, label, icon: Icon, tab }) => (
          <div
            key={key}
            className="stat-card"
            role="button"
            tabIndex={0}
            onClick={() => onTabChange(tab)}
            onKeyDown={(e) => e.key === "Enter" && onTabChange(tab)}
          >
            <Icon size={32} className="stat-icon" />
            <span className="stat-count">{counts[key] ?? "…"}</span>
            <span className="stat-label">{label}</span>
          </div>
        ))}
      </div>

      {recentLodgings.length > 0 && (
        <div className="dashboard-recent">
          <h3 className="dashboard-recent-title">Últimos alojamientos</h3>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
              </tr>
            </thead>
            <tbody>
              {recentLodgings.map((l) => (
                <tr key={l.id}>
                  <td>{l.id}</td>
                  <td>{l.name}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
