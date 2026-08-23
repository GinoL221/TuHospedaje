import { useState, useEffect, useRef, useCallback, startTransition } from "react";
import { useNavigate } from "react-router-dom";
import DatePicker, { registerLocale } from "react-datepicker";
import { es } from "date-fns/locale/es";
import { get } from "../../services/api";
import { getRecommendations } from "../../services/lodgingService";
import { useAuth } from "../../hooks/useAuth";
import ProductCard from "../../components/ProductCard/ProductCard";
import CategoryCard from "./CategoryCard";
import "../../App.css";
import "./Home.css";

registerLocale("es", es);

// Tab-scoped snapshot key (see design.md §1): a seed is generated once per
// browser tab via crypto.randomUUID and reused across reload/back/forward;
// closing the tab ends the session because sessionStorage is tab-scoped.
const RECOMMENDATIONS_STORAGE_KEY = "tuhospedaje.recommendations.v1";

function createRecommendationSeed() {
	try {
		return crypto.randomUUID();
	} catch {
		return `fallback-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-seed`.slice(
			0,
			64,
		);
	}
}

function readStoredRecommendationSession() {
	try {
		const raw = sessionStorage.getItem(RECOMMENDATIONS_STORAGE_KEY);
		if (!raw) return null;
		const parsed = JSON.parse(raw);
		return parsed && typeof parsed.seed === "string" ? parsed : null;
	} catch {
		return null;
	}
}

function writeStoredRecommendationSession(session) {
	try {
		sessionStorage.setItem(RECOMMENDATIONS_STORAGE_KEY, JSON.stringify(session));
	} catch {
		// sessionStorage may be unavailable (e.g. private mode); the
		// recommendation session then simply lives only in memory.
	}
}

export default function Home() {
	const navigate = useNavigate();
	const { user } = useAuth();
	const [lodgings, setLodgings] = useState([]);
	const [categories, setCategories] = useState([]);
	const [selectedCategory, setSelectedCategory] = useState(null);
	const [city, setCity] = useState("");
	const [checkIn, setCheckIn] = useState(null);
	const [checkOut, setCheckOut] = useState(null);
	const [suggestions, setSuggestions] = useState([]);
	const [showSuggestions, setShowSuggestions] = useState(false);
	const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
	const [loadingCities, setLoadingCities] = useState(false);
	const [searchError, setSearchError] = useState("");
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(1);
	const [favoriteIds, setFavoriteIds] = useState(new Set());
	const debounceRef = useRef();

	// Recommendation snapshot: seed is created once (or reused from
	// sessionStorage) and only replaced on an explicit refresh. Revision
	// lives in a ref because updating it from a response must not itself
	// re-trigger a fetch (see design.md §1, item 6-8).
	const [recSeed, setRecSeed] = useState(
		() => readStoredRecommendationSession()?.seed ?? createRecommendationSeed(),
	);
	const revisionRef = useRef(readStoredRecommendationSession()?.revision ?? null);
	const [recStatus, setRecStatus] = useState("idle"); // idle | loading | error
	const requestIdRef = useRef(0);
	const skipResetPageFetchRef = useRef(false);

	useEffect(() => {
		writeStoredRecommendationSession({ seed: recSeed, revision: revisionRef.current });
	}, [recSeed]);

	const fetchRecommendations = useCallback(() => {
		const requestId = ++requestIdRef.current;
		// Matches the SearchResults.jsx convention for setState called
		// synchronously from inside an effect (see runSearch).
		startTransition(() => {
			setRecStatus("loading");
			// Clear stale results before issuing the request so a slow/failed
			// response can never be presented alongside the previous page.
			setLodgings([]);
		});

		getRecommendations({ seed: recSeed, page, revision: revisionRef.current ?? undefined })
			.then((data) => {
				if (requestId !== requestIdRef.current) return; // stale/out-of-order response
				revisionRef.current = data.revision ?? revisionRef.current;
				writeStoredRecommendationSession({ seed: recSeed, revision: revisionRef.current });
				setLodgings(data.lodgings || []);
				setTotalPages(data.totalPages || 1);
				setRecStatus("idle");
				setPage((prev) => {
					const actualPage =
						typeof data.currentPage === "number" ? data.currentPage : prev;
					if (data.reset && prev !== actualPage) skipResetPageFetchRef.current = true;
					return prev === actualPage ? prev : actualPage;
				});
			})
			.catch(() => {
				if (requestId !== requestIdRef.current) return;
				setRecStatus("error");
			});
	}, [recSeed, page]);

	const fetchCategoryLodgings = useCallback(() => {
		get(`/lodgings?category=${selectedCategory}`)
			.then((data) => {
				setLodgings(Array.isArray(data) ? data : []);
				setTotalPages(1);
			})
			.catch(console.error);
	}, [selectedCategory]);

	useEffect(() => {
		if (selectedCategory) {
			fetchCategoryLodgings();
		} else if (skipResetPageFetchRef.current) {
			skipResetPageFetchRef.current = false;
		} else {
			fetchRecommendations();
		}
	}, [selectedCategory, fetchCategoryLodgings, fetchRecommendations]);

	function handleRefreshRecommendations() {
		revisionRef.current = null;
		setPage(0);
		setRecSeed(createRecommendationSeed());
	}

	useEffect(() => {
		get("/categories")
			.then((data) => setCategories(Array.isArray(data) ? data : []))
			.catch(() => {});
	}, []);

	useEffect(() => {
		if (!user) return;
		get("/favorites")
			.then((data) => {
				if (Array.isArray(data)) setFavoriteIds(new Set(data.map((l) => l.id)));
			})
			.catch(() => {});
	}, [user]);

	useEffect(() => {
		if (city.length < 2) return;

		clearTimeout(debounceRef.current);
		debounceRef.current = setTimeout(() => {
			setLoadingCities(true);
			setShowSuggestions(true);
			get(`/lodgings/cities?q=${encodeURIComponent(city)}`)
				.then((data) => {
					setSuggestions(Array.isArray(data) ? data : []);
					setActiveSuggestionIndex(-1);
					setLoadingCities(false);
				})
				.catch(() => {
					setSuggestions([]);
					setActiveSuggestionIndex(-1);
					setLoadingCities(false);
				});
		}, 200);

		return () => clearTimeout(debounceRef.current);
	}, [city]);

	function formatDate(date) {
		return date ? date.toISOString().split("T")[0] : "";
	}

	function handleSearch(e) {
		e.preventDefault();
		setSearchError("");

		if (checkIn && checkOut && checkIn >= checkOut) {
			setSearchError("La fecha de check-out debe ser posterior al check-in");
			return;
		}

		const params = new URLSearchParams();
		if (city) params.set("city", city);
		if (checkIn) params.set("checkIn", formatDate(checkIn));
		if (checkOut) params.set("checkOut", formatDate(checkOut));

		navigate(`/search?${params.toString()}`);
	}

	function handleFavoriteToggle(id, add) {
		setFavoriteIds((prev) => {
			const next = new Set(prev);
			if (add) next.add(id);
			else next.delete(id);
			return next;
		});
	}

	function handleCityChange(value) {
		setCity(value);
		setActiveSuggestionIndex(-1);
		if (value.length < 2) {
			setSuggestions([]);
			setShowSuggestions(false);
			setLoadingCities(false);
		}
	}

	function selectCity(value) {
		setCity(value);
		setShowSuggestions(false);
		setActiveSuggestionIndex(-1);
	}

	function handleCityKeyDown(event) {
		if (event.key === "Escape") {
			setShowSuggestions(false);
			setActiveSuggestionIndex(-1);
			return;
		}

		if (suggestions.length === 0) return;

		if (event.key === "ArrowDown" || event.key === "ArrowUp") {
			event.preventDefault();
			setShowSuggestions(true);
			setActiveSuggestionIndex((current) => {
				if (event.key === "ArrowDown") return (current + 1) % suggestions.length;
				return current <= 0 ? suggestions.length - 1 : current - 1;
			});
			return;
		}

		if (
			event.key === "Enter" &&
			showSuggestions &&
			activeSuggestionIndex >= 0
		) {
			event.preventDefault();
			selectCity(suggestions[activeSuggestionIndex]);
		}
	}

	return (
		<main className="home page-container">
			<section className="search">
				<div className="search-card">
					<h2>Buscar hospedaje</h2>
					<form onSubmit={handleSearch}>
						<div style={{ position: "relative", width: "100%" }}>
							<input
								type="text"
								placeholder="Ciudad"
									value={city}
									onChange={(e) => handleCityChange(e.target.value)}
									onFocus={() => setShowSuggestions(city.length >= 2)}
									onBlur={() => setTimeout(() => setShowSuggestions(false), 300)}
									onKeyDown={handleCityKeyDown}
									role="combobox"
									aria-autocomplete="list"
									aria-expanded={showSuggestions}
									aria-controls="city-suggestions-listbox"
									aria-activedescendant={
										activeSuggestionIndex >= 0
											? `city-suggestion-${activeSuggestionIndex}`
											: undefined
									}
								/>
								{showSuggestions && (
									<ul
										id="city-suggestions-listbox"
										className="city-suggestions"
										role="listbox"
										aria-label="Sugerencias de ciudades"
									>
										{loadingCities ? (
											<li className="city-suggestions-loading" role="status">
												Buscando...
											</li>
										) : suggestions.length === 0 ? (
											<li className="city-suggestions-empty" role="status">
												Sin resultados
											</li>
										) : (
											suggestions.map((c, index) => (
												<li
													key={c}
													id={`city-suggestion-${index}`}
													role="option"
													aria-selected={activeSuggestionIndex === index}
													className={
														activeSuggestionIndex === index ? "is-active" : undefined
													}
													onMouseEnter={() => setActiveSuggestionIndex(index)}
													onMouseDown={() => selectCity(c)}
											>
												{c}
											</li>
										))
									)}
								</ul>
							)}
						</div>
						<div>
							<DatePicker
								selected={checkIn}
								onChange={(date) => setCheckIn(date)}
								selectsStart
									startDate={checkIn}
									endDate={checkOut}
									minDate={new Date()}
								placeholderText="Check-in"
								dateFormat="dd/MM/yyyy"
								locale="es"
								popperClassName="home-datepicker-popper"
							/>
						</div>
						<div>
							<DatePicker
								selected={checkOut}
								onChange={(date) => setCheckOut(date)}
								selectsEnd
									startDate={checkIn}
									endDate={checkOut}
									minDate={checkIn || new Date()}
								placeholderText="Check-out"
								dateFormat="dd/MM/yyyy"
								locale="es"
								popperClassName="home-datepicker-popper"
							/>
						</div>
						<button type="submit" className="btn-search">
							Buscar
						</button>
					</form>
					{searchError && <p className="search-error">{searchError}</p>}
				</div>
			</section>
			<section className="categories">
				<h2>Categorías</h2>
				{categories.length === 0 ? (
					<p className="empty-state">No hay categorías disponibles.</p>
				) : (
					<div className="category-list">
						{categories.map((c) => (
							<CategoryCard
								key={c.id}
								category={c}
								isActive={selectedCategory === c.id}
								onClick={() => {
									setSelectedCategory(selectedCategory === c.id ? null : c.id);
									setPage(0);
								}}
							/>
						))}
					</div>
				)}
			</section>
			<section className="recommendations">
				<div className="section-header">
					<h2>
						{selectedCategory
							? categories.find((c) => c.id === selectedCategory)?.name
							: "Recomendaciones"}
					</h2>
					{selectedCategory && (
						<button
							className="btn-clear-filter"
							onClick={() => setSelectedCategory(null)}
						>
							Mostrar todos
						</button>
					)}
					{!selectedCategory && (
						<button
							type="button"
							className="btn-refresh-recommendations"
							onClick={handleRefreshRecommendations}
						>
							Actualizar recomendaciones
						</button>
					)}
				</div>
				{!selectedCategory && recStatus === "loading" && (
					<p className="recommendations-status" role="status">
						Cargando recomendaciones...
					</p>
				)}
				{!selectedCategory && recStatus === "error" && (
					<div className="recommendations-alert" role="alert">
						<p>No pudimos cargar las recomendaciones.</p>
						<button type="button" onClick={fetchRecommendations}>
							Reintentar
						</button>
					</div>
				)}
				{(selectedCategory || recStatus === "idle") &&
					(lodgings.length === 0 ? (
						<p className="empty-state">
							No hay alojamientos cargados todavía. Volvé más tarde.
						</p>
					) : (
						<div className="hotel-list">
							{lodgings.map((lodging) => (
								<ProductCard
									key={lodging.id}
									lodging={lodging}
									defaultFavorite={user ? favoriteIds.has(lodging.id) : false}
									onFavoriteToggle={handleFavoriteToggle}
								/>
							))}
						</div>
					))}
				{!selectedCategory && totalPages > 1 && (
					<div className="home-pagination">
						<button disabled={page === 0} onClick={() => setPage(0)}>
							Inicio
						</button>
						<button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
							Anterior
						</button>
						<span>
							Página {page + 1} de {totalPages}
						</span>
						<button
							disabled={page >= totalPages - 1}
							onClick={() => setPage((p) => p + 1)}
						>
							Siguiente
						</button>
						<button
							disabled={page >= totalPages - 1}
							onClick={() => setPage(totalPages - 1)}
						>
							Última
						</button>
					</div>
				)}
			</section>
		</main>
	);
}
