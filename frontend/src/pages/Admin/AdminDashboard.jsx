import { useState, useEffect } from "react";
import { Building2, Tag, Star, Users, Calendar } from "lucide-react";
import {
  getAdminLodgings,
  getAdminStats,
} from "../../services/adminCatalogService";
import { getAdminReservations } from "../../services/reservationService";
import {
  hasReservationNotes,
  reservationCreatedAtLabel,
} from "../../utils/reservationPresentation";

// The `key` of each card is the field name in GET /api/admin/stats.
const STATS = [
  { key: "lodgings", label: "Alojamientos", icon: Building2, tab: "lodgings" },
  { key: "categories", label: "Categorías", icon: Tag, tab: "categories" },
  { key: "features", label: "Características", icon: Star, tab: "features" },
  { key: "users", label: "Usuarios", icon: Users, tab: "users" },
  {
    key: "reservations",
    label: "Reservas",
    icon: Calendar,
    tab: "reservations",
  },
];

const RECENT_COUNT = 4;

const UNAVAILABLE = "—";

export default function AdminDashboard({ onTabChange }) {
  const [counts, setCounts] = useState({});
  const [recentLodgings, setRecentLodgings] = useState([]);
  const [recentReservations, setRecentReservations] = useState([]);

  useEffect(() => {
    // One call for all five cards. Counting used to mean downloading each table whole
    // and reading .length, which shipped every user record to render a number and — for
    // lodgings — reported the listing's result cap instead of the real total.
    getAdminStats()
      .then(setCounts)
      .catch(() =>
        setCounts(
          Object.fromEntries(STATS.map(({ key }) => [key, UNAVAILABLE])),
        ),
      );

    getAdminLodgings({ size: RECENT_COUNT, direction: "desc" })
      .then((page) => setRecentLodgings(page?.items ?? []))
      .catch(() => {});

    getAdminReservations({ size: RECENT_COUNT, direction: "desc" })
      .then((page) => setRecentReservations(page?.items ?? []))
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
            onClick={() => tab && onTabChange(tab)}
            onKeyDown={(e) => e.key === "Enter" && tab && onTabChange(tab)}
          >
            <Icon size={32} className="stat-icon" />
            <span className="stat-count">{counts[key] ?? "…"}</span>
            <span className="stat-label">{label}</span>
          </div>
        ))}
      </div>

      <div className="dashboard-recent-container">
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

        {recentReservations.length > 0 && (
          <div className="dashboard-recent">
            <h3 className="dashboard-recent-title">Últimas reservas</h3>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Alojamiento</th>
                  <th>Huésped</th>
                  <th>Creación</th>
                  <th>Notas</th>
                  <th>Check-in</th>
                  <th>Check-out</th>
                  <th>Total</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {recentReservations.map((r) => (
                  <tr key={r.id}>
                    <td>{r.id}</td>
                    <td>{r.lodgingName}</td>
                    <td>{r.guestName}</td>
                    <td>{reservationCreatedAtLabel(r)}</td>
                    <td>
                      {hasReservationNotes(r.notes) ? r.notes.trim() : "-"}
                    </td>
                    <td>{r.checkIn}</td>
                    <td>{r.checkOut}</td>
                    <td>${r.totalPrice}</td>
                    <td>
                      <span
                        className={`status-badge status-${r.status.toLowerCase()}`}
                      >
                        {r.status === "CONFIRMED" ? "Confirmada" : "Cancelada"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
