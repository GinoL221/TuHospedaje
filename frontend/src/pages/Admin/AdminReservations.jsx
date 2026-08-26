import { useEffect, useState } from "react";
import { getAdminReservations } from "../../services/reservationService";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";
import { hasReservationNotes, reservationCreatedAtLabel } from "../../utils/reservationPresentation";

const PAGE_SIZE = 10;

const SORTABLE_COLUMNS = ["id", "checkIn", "checkOut", "status", "totalPrice"];

const STATUS_CONFIG = {
	CONFIRMED: { optionLabel: "Confirmadas", rowLabel: "Confirmada" },
	CANCELLED: { optionLabel: "Canceladas", rowLabel: "Cancelada" },
};

const STATUS_OPTIONS = [
	{ value: "", label: "Todos los estados" },
	...Object.entries(STATUS_CONFIG).map(([value, labels]) => ({
		value,
		label: labels.optionLabel,
	})),
];

export default function AdminReservations() {
	const [reservations, setReservations] = useState([]);
	const [loading, setLoading] = useState(true);
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);
	const [sortKey, setSortKey] = useState("id");
	const [sortDir, setSortDir] = useState("asc");
	const [status, setStatus] = useState("");
	const [search, setSearch] = useState("");
	const [reloadTick, setReloadTick] = useState(0);
	const [error, setError] = useState("");

	useEffect(() => {
		let cancelled = false;

		getAdminReservations({
			page,
			size: PAGE_SIZE,
			sort: sortKey,
			direction: sortDir,
			status,
			q: search,
		})
			.then((data) => {
				if (cancelled) return;
				setReservations(Array.isArray(data?.items) ? data.items : []);
				setPage(data?.currentPage ?? page);
				setTotalPages(data?.totalPages ?? 0);
				setLoading(false);
			})
			.catch((err) => {
				console.error(err);
				if (cancelled) return;
				setReservations([]);
				setTotalPages(0);
				setError("No se pudieron cargar las reservas. Intenta nuevamente.");
				setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [page, reloadTick, search, sortDir, sortKey, status]);

	const handleSort = (key) => {
		if (!SORTABLE_COLUMNS.includes(key)) return;
		setError("");
		setLoading(true);
		setPage(0);

		if (key === sortKey) {
			setSortDir((current) => (current === "asc" ? "desc" : "asc"));
		} else {
			setSortKey(key);
			setSortDir("asc");
		}
	};

	const handleSearchChange = (event) => {
		setError("");
		setLoading(true);
		setPage(0);
		setSearch(event.target.value);
	};

	const handleStatusChange = (event) => {
		setError("");
		setLoading(true);
		setPage(0);
		setStatus(event.target.value);
	};

	const resetFilters = () => {
		setError("");
		setLoading(true);
		setPage(0);
		setSortKey("id");
		setSortDir("asc");
		setStatus("");
		setSearch("");
		setReloadTick((current) => current + 1);
	};

	const retryFetch = () => {
		setError("");
		setLoading(true);
		setReloadTick((current) => current + 1);
	};

	const handlePageChange = (nextPage) => {
		setError("");
		setLoading(true);
		setPage(nextPage);
	};

	return (
		<>
			<div className="admin-section-header">
				<h2>Reservas</h2>
			</div>

			<div className="admin-table-controls">
				<label htmlFor="admin-reservations-search">
					Buscar reservas
					<input
						id="admin-reservations-search"
						aria-label="Buscar reservas"
						value={search}
						onChange={handleSearchChange}
						placeholder="Buscar por huésped o contacto"
					/>
				</label>

				<label htmlFor="admin-reservations-status">
					Estado
					<select
						id="admin-reservations-status"
						aria-label="Filtrar por estado"
						value={status}
						onChange={handleStatusChange}
					>
						{STATUS_OPTIONS.map((option) => (
							<option key={option.value || "all"} value={option.value}>
								{option.label}
							</option>
						))}
					</select>
				</label>

				<button type="button" onClick={resetFilters}>
					Limpiar filtros
				</button>
			</div>

			{error ? (
				<div className="empty-state" role="alert">
					<p>{error}</p>
					<button type="button" onClick={retryFetch}>
						Reintentar
					</button>
				</div>
			) : null}

			{loading ? (
				<p className="empty-state">Cargando reservas...</p>
			) : error ? null : reservations.length === 0 ? (
				<p className="empty-state">No hay reservas registradas.</p>
			) : (
				<>
					<table data-testid="reservations-table">
						<thead>
							<tr>
								<SortableTh columnKey="id" sortKey={sortKey} sortDir={sortDir} onSort={handleSort}>
									ID
								</SortableTh>
								<th>Alojamiento</th>
								<th>Huésped</th>
								<th>Creación</th>
								<th>Notas</th>
								<SortableTh columnKey="checkIn" sortKey={sortKey} sortDir={sortDir} onSort={handleSort}>
									Check-in
								</SortableTh>
								<SortableTh columnKey="checkOut" sortKey={sortKey} sortDir={sortDir} onSort={handleSort}>
									Check-out
								</SortableTh>
								<SortableTh columnKey="totalPrice" sortKey={sortKey} sortDir={sortDir} onSort={handleSort}>
									Total
								</SortableTh>
								<SortableTh columnKey="status" sortKey={sortKey} sortDir={sortDir} onSort={handleSort}>
									Estado
								</SortableTh>
							</tr>
						</thead>
						<tbody>
							{reservations.map((r) => (
								<tr key={r.id} data-testid={`row-${r.id}`}>
									<td>{r.id}</td>
									<td>{r.lodgingName}</td>
									<td>{r.guestName}</td>
									<td>{reservationCreatedAtLabel(r)}</td>
									<td>{hasReservationNotes(r.notes) ? r.notes.trim() : "-"}</td>
									<td>{r.checkIn}</td>
									<td>{r.checkOut}</td>
									<td>${r.totalPrice}</td>
									<td>
										<span className={`status-badge status-${r.status.toLowerCase()}`}>
											{STATUS_CONFIG[r.status]?.rowLabel ?? r.status}
										</span>
									</td>
								</tr>
							))}
						</tbody>
					</table>
					<Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
				</>
			)}
		</>
	);
}
