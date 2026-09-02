import { useEffect, useRef, useState } from "react";
import { get } from "../services/api";

const DEBOUNCE_MS = 200;
const BLUR_DELAY_MS = 300;

export default function useCityAutocomplete() {
	const [city, setCity] = useState("");
	const [suggestions, setSuggestions] = useState([]);
	const [showSuggestions, setShowSuggestions] = useState(false);
	const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
	const [loadingCities, setLoadingCities] = useState(false);
	const debounceRef = useRef();

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
		}, DEBOUNCE_MS);

		return () => clearTimeout(debounceRef.current);
	}, [city]);

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

	function handleCityFocus() {
		setShowSuggestions(city.length >= 2);
	}

	function handleCityBlur() {
		setTimeout(() => setShowSuggestions(false), BLUR_DELAY_MS);
	}

	function activateSuggestion(index) {
		setActiveSuggestionIndex(index);
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

		if (event.key === "Enter" && showSuggestions && activeSuggestionIndex >= 0) {
			event.preventDefault();
			selectCity(suggestions[activeSuggestionIndex]);
		}
	}

	return {
		city,
		suggestions,
		showSuggestions,
		activeSuggestionIndex,
		loadingCities,
		handleCityChange,
		handleCityFocus,
		handleCityBlur,
		handleCityKeyDown,
		activateSuggestion,
		selectCity,
	};
}
