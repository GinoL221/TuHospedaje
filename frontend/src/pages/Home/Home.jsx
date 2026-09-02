import { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import DatePicker, { registerLocale } from "react-datepicker";
import { es } from "date-fns/locale/es";
import { get } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import useHomeRecommendations from "../../hooks/useHomeRecommendations";
import useHomeSearchResults from "../../hooks/useHomeSearchResults";
import ProductCard from "../../components/ProductCard/ProductCard";
import CategoryCard from "./CategoryCard";
import "../../App.css";
import "./Home.css";

registerLocale("es", es);

export default function Home() {
	const navigate = useNavigate();
	const { search } = useLocation();
	const { user } = useAuth();
	const [categories, setCategories] = useState([]);
	const [city, setCity] = useState("");
	const [checkIn, setCheckIn] = useState(null);
	const [checkOut, setCheckOut] = useState(null);
	const [suggestions, setSuggestions] = useState([]);
	const [showSuggestions, setShowSuggestions] = useState(false);
	const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
	const [loadingCities, setLoadingCities] = useState(false);
	const [searchError, setSearchError] = useState("");
	const [favoriteIds, setFavoriteIds] = useState(new Set());
	const debounceRef = useRef();
	const {
		lodgings,
		status: recStatus,
		listBusy,
		listGeneration,
		page,
		totalPages,
		setPage,
		refresh: handleRefreshRecommendations,
		retry: fetchRecommendations,
	} = useHomeRecommendations();
	const { searchResults: visibleSearchResults } = useHomeSearchResults(search);

	const searchParams = new URLSearchParams(search);
	const selectedCategories = searchParams.getAll("categories");
	function updateCategories(categoryId) {
		const next = new URLSearchParams(search);
		const category = String(categoryId);
		const selected = next.getAll("categories").filter((id) => id !== category);
		next.delete("categories");
		(selected.length === selectedCategories.length ? [...selected, category] : selected).forEach((id) => next.append("categories", id));
		navigate(`/?${next.toString()}`);
	}
	function clearCategories() {
		const next = new URLSearchParams(search);
		next.delete("categories");
		navigate(next.toString() ? `/?${next.toString()}` : "/");
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

		navigate(`/?${params.toString()}`);
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
									className={
										"city-suggestions" +
										(loadingCities ? " is-pending" : "")
									}
									role="listbox"
									aria-label="Sugerencias de ciudades"
									aria-busy={loadingCities}
								>
										{loadingCities && (
											<li className="city-suggestions-loading" role="status">
												Buscando...
											</li>
										)}
										{!loadingCities && suggestions.length === 0 && (
											<li className="city-suggestions-empty" role="status">
												Sin resultados
											</li>
										)}
										{suggestions.map((c, index) => (
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
										))}
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
					{searchError && <p className="search-error" role="alert">{searchError}</p>}
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
								isActive={selectedCategories.includes(String(c.id))}
								onClick={() => updateCategories(c.id)}
							/>
						))}
					</div>
				)}
			</section>
			{visibleSearchResults && (
				<section className="search-results" aria-live="polite">
					<div className="section-header"><h2>Resultados de búsqueda</h2>{selectedCategories.length > 0 && <button className="btn-clear-filter" onClick={clearCategories}>Limpiar filtros</button>}</div>
					<p>{visibleSearchResults.totalItems ?? 0} resultados de {visibleSearchResults.catalogItems ?? 0} alojamientos</p>
					{visibleSearchResults.lodgings?.length > 0 && <div className="hotel-list">{visibleSearchResults.lodgings.map((lodging) => <ProductCard key={lodging.id} lodging={lodging} />)}</div>}
				</section>
			)}
			<section className="recommendations">
				<div className="section-header">
					<h2>
						Recomendaciones
					</h2>
						<button
							type="button"
							className="btn-refresh-recommendations"
							onClick={handleRefreshRecommendations}
						>
							Actualizar recomendaciones
						</button>
				</div>
				{recStatus === "loading" && (
					<p className="recommendations-status" role="status">
						Cargando recomendaciones...
					</p>
				)}
				{recStatus === "error" && (
					<div className="recommendations-alert" role="alert">
						<p>No pudimos cargar las recomendaciones.</p>
						<button type="button" onClick={fetchRecommendations}>
							Reintentar
						</button>
					</div>
				)}
				{lodgings.length > 0 && (
					<div
						key={listGeneration}
						className={"hotel-list" + (listBusy ? " is-pending" : "")}
						role="list"
						aria-label="Recomendaciones"
						aria-busy={listBusy}
					>
						{lodgings.map((lodging) => (
							<ProductCard
								key={lodging.id}
								lodging={lodging}
								defaultFavorite={user ? favoriteIds.has(lodging.id) : false}
								onFavoriteToggle={handleFavoriteToggle}
							/>
						))}
					</div>
				)}
				{!listBusy && recStatus !== "error" && lodgings.length === 0 && (
					<p className="empty-state">
						No hay alojamientos cargados todavía. Volvé más tarde.
					</p>
				)}
				{totalPages > 1 && (
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
