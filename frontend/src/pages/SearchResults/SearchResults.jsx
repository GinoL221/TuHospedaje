import { useState, useEffect, useRef, useTransition } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { Calendar, X, Tag, DollarSign } from "lucide-react";
import DatePicker from "react-datepicker";
import { searchLodgings } from "../../services/lodgingService";
import { getCategories } from "../../services/categoryService";
import { getFavorites } from "../../services/favoriteService";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import Icon from "../../components/Icons/Icon";
import Pagination from "../../components/Pagination/Pagination";
import "../../App.css";
import "./SearchResults.css";

function formatDate(date) {
	return date ? date.toISOString().split("T")[0] : "";
}

function parseDate(str) {
	return str ? new Date(str + "T12:00:00") : null;
}

function fmtDisplay(str) {
	return str ? str.split("-").reverse().join("/") : "";
}

export default function SearchResults() {
	const [searchParams] = useSearchParams();
	const navigate = useNavigate();
	const { user } = useAuth();
	const [results, setResults] = useState([]);
	const [categories, setCategories] = useState([]);
	const [loading, setLoading] = useState(true);
	const [listGeneration, setListGeneration] = useState(0);
	const [error, setError] = useState("");
	const [favoriteIds, setFavoriteIds] = useState(new Set());

	const city = searchParams.get("city") || "";
	const checkIn = searchParams.get("checkIn") || "";
	const checkOut = searchParams.get("checkOut") || "";

	// Sidebar (pending) state — what the user is configuring, not yet applied
	const [sidebarCheckIn, setSidebarCheckIn] = useState(() =>
		parseDate(checkIn),
	);
	const [sidebarCheckOut, setSidebarCheckOut] = useState(() =>
		parseDate(checkOut),
	);
	const [pendingCategories, setPendingCategories] = useState(new Set());
	const [pendingMinPrice, setPendingMinPrice] = useState("");
	const [pendingMaxPrice, setPendingMaxPrice] = useState("");

	// Applied state — what's actually in effect (drives chips + search)
	const [appliedCategories, setAppliedCategories] = useState(new Set());
	const [appliedMinPrice, setAppliedMinPrice] = useState("");
	const [appliedMaxPrice, setAppliedMaxPrice] = useState("");

	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);

	const [, startTransition] = useTransition();
	const skipNextSearchRef = useRef(false);
	const requestIdRef = useRef(0);
	const lastSearchRef = useRef({
		cats: new Set(),
		baseParams: new URLSearchParams(),
	});

	useEffect(() => {
		queueMicrotask(() => {
			setSidebarCheckIn(parseDate(checkIn));
			setSidebarCheckOut(parseDate(checkOut));
		});
	}, [checkIn, checkOut]);

	// Single fetch path for both the initial load and every filter/pagination
	// change: categories are always sent to the backend (no more client-side
	// intersection), and pagination is server-driven via `page`/`currentPage`.
	function runSearch(cats, baseParams, pageNum = 0) {
		lastSearchRef.current = { cats, baseParams };
		const params = new URLSearchParams(baseParams);
		cats.forEach((id) => params.append("categories", id));
		params.set("page", pageNum);
		const requestId = ++requestIdRef.current;

		startTransition(() => {
			setLoading(true);
			setError("");
		});
		searchLodgings(params)
			.then((data) => {
				if (requestId !== requestIdRef.current) return;
				setResults(Array.isArray(data?.lodgings) ? data.lodgings : []);
				setPage(data?.currentPage ?? pageNum);
				setTotalPages(data?.totalPages ?? 0);
				setListGeneration((generation) => generation + 1);
				setLoading(false);
			})
			.catch((err) => {
				if (requestId !== requestIdRef.current) return;
				setError(err.message);
				setLoading(false);
			});
	}

	useEffect(() => {
		getCategories()
			.then((data) => setCategories(Array.isArray(data) ? data : []))
			.catch(() => {});
	}, []);

	useEffect(() => {
		if (!user) return;
		getFavorites()
			.then((data) => {
				if (Array.isArray(data)) setFavoriteIds(new Set(data.map((l) => l.id)));
			})
			.catch(() => {});
	}, [user]);

	useEffect(() => {
		if (skipNextSearchRef.current) {
			skipNextSearchRef.current = false;
			return;
		}
		const params = new URLSearchParams();
		if (city) params.set("city", city);
		if (checkIn) params.set("checkIn", checkIn);
		if (checkOut) params.set("checkOut", checkOut);
		runSearch(new Set(), params);
	}, [city, checkIn, checkOut]);

	function handleFavoriteToggle(id, add) {
		setFavoriteIds((prev) => {
			const next = new Set(prev);
			if (add) next.add(id);
			else next.delete(id);
			return next;
		});
	}

	function buildParams({ ci, co, min, max } = {}) {
		const _ci = ci !== undefined ? ci : formatDate(sidebarCheckIn);
		const _co = co !== undefined ? co : formatDate(sidebarCheckOut);
		const _min = min !== undefined ? min : appliedMinPrice;
		const _max = max !== undefined ? max : appliedMaxPrice;
		const params = new URLSearchParams();
		if (city) params.set("city", city);
		if (_ci) params.set("checkIn", _ci);
		if (_co) params.set("checkOut", _co);
		if (_min) params.set("minPrice", _min);
		if (_max) params.set("maxPrice", _max);
		return params;
	}

	function handleFilter() {
		// Commit pending → applied
		const cats = new Set(pendingCategories);
		setAppliedCategories(cats);
		setAppliedMinPrice(pendingMinPrice);
		setAppliedMaxPrice(pendingMaxPrice);

		const params = buildParams({ min: pendingMinPrice, max: pendingMaxPrice });
		skipNextSearchRef.current = true;
		navigate(`/search?${params.toString()}`, { replace: true });
		runSearch(cats, params);
	}

	function clearFilters() {
		setPendingCategories(new Set());
		setPendingMinPrice("");
		setPendingMaxPrice("");
		setAppliedCategories(new Set());
		setAppliedMinPrice("");
		setAppliedMaxPrice("");
		const params = new URLSearchParams();
		if (city) params.set("city", city);
		navigate(`/search?${params.toString()}`);
	}

	function removeCategoryChip(id) {
		const next = new Set(appliedCategories);
		next.delete(id);
		setAppliedCategories(next);
		setPendingCategories(new Set(next));
		runSearch(
			next,
			buildParams({ min: appliedMinPrice, max: appliedMaxPrice }),
		);
	}

	function removePriceChip() {
		setAppliedMinPrice("");
		setAppliedMaxPrice("");
		setPendingMinPrice("");
		setPendingMaxPrice("");
		runSearch(appliedCategories, buildParams({ min: "", max: "" }));
	}

	function removeDateChip() {
		setSidebarCheckIn(null);
		setSidebarCheckOut(null);
		skipNextSearchRef.current = true;
		navigate(`/search${city ? `?city=${encodeURIComponent(city)}` : ""}`, {
			replace: true,
		});
		runSearch(
			appliedCategories,
			buildParams({
				ci: "",
				co: "",
				min: appliedMinPrice,
				max: appliedMaxPrice,
			}),
		);
	}

	const hasChips =
		checkIn || appliedCategories.size > 0 || appliedMinPrice || appliedMaxPrice;

	return (
		<main className="search-results page-container">
			<aside className="search-filters" aria-labelledby="search-filters-title">
				<h3 id="search-filters-title">Filtros</h3>

				<div className="filter-section">
					<p className="filter-section-title">Fechas</p>
					<label htmlFor="filter-check-in">Check-in</label>
					<DatePicker
						id="filter-check-in"
						selected={sidebarCheckIn}
						onChange={(date) => setSidebarCheckIn(date)}
						selectsStart
						startDate={sidebarCheckIn}
						endDate={sidebarCheckOut}
						minDate={new Date()}
						placeholderText="Seleccioná fecha"
						dateFormat="dd/MM/yyyy"
					/>
					<label htmlFor="filter-check-out">Check-out</label>
					<DatePicker
						id="filter-check-out"
						selected={sidebarCheckOut}
						onChange={(date) => setSidebarCheckOut(date)}
						selectsEnd
						startDate={sidebarCheckIn}
						endDate={sidebarCheckOut}
						minDate={sidebarCheckIn || new Date()}
						placeholderText="Seleccioná fecha"
						dateFormat="dd/MM/yyyy"
					/>
				</div>

				<div className="filter-section">
					<p className="filter-section-title">Categorías</p>
					<div className="filter-checkboxes">
						{categories.map((c) => (
							<label key={c.id} className="filter-checkbox-label">
								<input
									type="checkbox"
									checked={pendingCategories.has(c.id)}
									onChange={() => {
										setPendingCategories((prev) => {
											const next = new Set(prev);
											if (next.has(c.id)) next.delete(c.id);
											else next.add(c.id);
											return next;
										});
									}}
								/>
								{c.icon && (
									<span className="filter-category-icon" aria-hidden="true">
										<Icon name={c.icon} size={16} />
									</span>
								)}
								<span className="filter-category-name">{c.name}</span>
							</label>
						))}
					</div>
				</div>

				<div className="filter-section">
					<p className="filter-section-title">Precio por noche</p>
					<label htmlFor="filter-min-price">Mínimo</label>
					<input
						id="filter-min-price"
						type="number"
						value={pendingMinPrice}
						onChange={(e) => setPendingMinPrice(e.target.value)}
						placeholder="$"
					/>
					<label htmlFor="filter-max-price">Máximo</label>
					<input
						id="filter-max-price"
						type="number"
						value={pendingMaxPrice}
						onChange={(e) => setPendingMaxPrice(e.target.value)}
						placeholder="$"
					/>
				</div>

				<div className="filter-actions">
					<button onClick={handleFilter} className="btn-filter">
						Aplicar filtros
					</button>
					<button onClick={clearFilters} className="btn-clear">
						Limpiar filtros
					</button>
				</div>
			</aside>

			<section className="search-results-list">
				<h2>{city ? `Resultados para "${city}"` : "Todos los alojamientos"}</h2>

				{hasChips && (
					<div className="active-filters">
						{checkIn && checkOut && (
							<span className="filter-chip">
								<Calendar size={13} />
								{fmtDisplay(checkIn)} — {fmtDisplay(checkOut)}
								<button
									className="chip-remove"
									onClick={removeDateChip}
									aria-label="Quitar fechas"
								>
									<X size={12} />
								</button>
							</span>
						)}
						{[...appliedCategories].map((id) => {
							const cat = categories.find((c) => c.id === id);
							return cat ? (
								<span key={id} className="filter-chip">
									<Tag size={13} />
									{cat.name}
									<button
										className="chip-remove"
										onClick={() => removeCategoryChip(id)}
										aria-label={`Quitar ${cat.name}`}
									>
										<X size={12} />
									</button>
								</span>
							) : null;
						})}
						{(appliedMinPrice || appliedMaxPrice) && (
							<span className="filter-chip">
								<DollarSign size={13} />
								{appliedMinPrice ? `$${appliedMinPrice}` : "sin mín"} —{" "}
								{appliedMaxPrice ? `$${appliedMaxPrice}` : "sin máx"}
								<button
									className="chip-remove"
									onClick={removePriceChip}
									aria-label="Quitar filtro de precio"
								>
									<X size={12} />
								</button>
							</span>
						)}
					</div>
				)}

				{loading && (
					<p className="empty-state" role="status">
						Buscando...
					</p>
				)}
				{error && <p className="empty-state error">{error}</p>}
				{!loading && !error && results.length === 0 && (
					<p className="empty-state">
						No se encontraron resultados para tu búsqueda.
					</p>
				)}
				{results.length > 0 && (
					<>
						<div
							key={listGeneration}
							className={"hotel-list" + (loading ? " is-pending" : "")}
							role="list"
							aria-label={
								city ? `Resultados para "${city}"` : "Todos los alojamientos"
							}
							aria-busy={loading}
						>
							{results.map((lodging) => (
								<ProductCard
									key={lodging.id}
									lodging={lodging}
									defaultFavorite={user ? favoriteIds.has(lodging.id) : false}
									onFavoriteToggle={handleFavoriteToggle}
								/>
							))}
						</div>
						<Pagination
							page={page}
							totalPages={totalPages}
							onPageChange={(newPage) =>
								runSearch(
									lastSearchRef.current.cats,
									lastSearchRef.current.baseParams,
									newPage,
								)
							}
							className="search-pagination"
						/>
					</>
				)}
			</section>
		</main>
	);
}
