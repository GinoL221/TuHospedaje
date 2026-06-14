import { useState, useEffect } from "react";
import { get } from "../../services/api";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

export default function AdminReservations() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);

  const { pageItems, sortKey, sortDir, requestSort, page, totalPages, setPage } =
    useTableData(reservations);

  useEffect(() => {
    let cancelled = false;
    get("/reservations")
      .then((data) => {
        if (!cancelled) {
          setReservations(Array.isArray(data) ? data : []);
          setLoading(false);
        }
      })
      .catch((err) => {
        console.error(err);
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <div className="admin-section-header">
        <h2>Reservas</h2>
      </div>

      {loading ? (
        <p className="empty-state">Cargando reservas...</p>
      ) : reservations.length === 0 ? (
        <p className="empty-state">No hay reservas registradas.</p>
      ) : (
        <>
          <table data-testid="reservations-table">
            <thead>
              <tr>
                <SortableTh
                  columnKey="id"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  ID
                </SortableTh>
                <SortableTh
                  columnKey="lodgingName"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Alojamiento
                </SortableTh>
                <SortableTh
                  columnKey="guestName"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Huésped
                </SortableTh>
                <SortableTh
                  columnKey="checkIn"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Check-in
                </SortableTh>
                <SortableTh
                  columnKey="checkOut"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Check-out
                </SortableTh>
                <SortableTh
                  columnKey="totalPrice"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Total
                </SortableTh>
                <SortableTh
                  columnKey="status"
                  sortKey={sortKey}
                  sortDir={sortDir}
                  onSort={requestSort}
                >
                  Estado
                </SortableTh>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((r) => (
                <tr key={r.id} data-testid={`row-${r.id}`}>
                  <td>{r.id}</td>
                  <td>{r.lodgingName}</td>
                  <td>{r.guestName}</td>
                  <td>{r.checkIn}</td>
                  <td>{r.checkOut}</td>
                  <td>${r.totalPrice}</td>
                  <td>
                    <span className={`status-badge status-${r.status.toLowerCase()}`}>
                      {r.status === "CONFIRMED" ? "Confirmada" : "Cancelada"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </>
  );
}
