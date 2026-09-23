import { useEffect, useRef, useState } from "react";
import { getCities } from "../services/lodgingService";

const DEBOUNCE_MS = 200;
const BLUR_DELAY_MS = 300;

export default function useCityAutocomplete() {
	const [city, setCity] = useState("");
	const [suggestions, setSuggestions] = useState([]);
	const [showSuggestions, setShowSuggestions] = useState(false);
	const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
	const [loadingCities, setLoadingCities] = useState(false);
	const debounceRef = useRef();
	const skipSearchForCityRef = useRef(null);
	const activeRequestRef = useRef(null);

	function invalidateActiveRequest() {
		const activeRequest = activeRequestRef.current;
		activeRequestRef.current = null;
		activeRequest?.controller.abort();
	}

	useEffect(() => {
		clearTimeout(debounceRef.current);

		if (skipSearchForCityRef.current === city) {
			skipSearchForCityRef.current = null;
			return;
		}

		skipSearchForCityRef.current = null;
		if (city.length < 2) return;

		debounceRef.current = setTimeout(() => {
			const request = { controller: new AbortController() };
			activeRequestRef.current = request;
			setLoadingCities(true);
			setShowSuggestions(true);
			getCities(city, { signal: request.controller.signal })
				.then((data) => {
					if (activeRequestRef.current !== request) return;
					activeRequestRef.current = null;
					setSuggestions(Array.isArray(data) ? data : []);
					setActiveSuggestionIndex(-1);
					setLoadingCities(false);
				})
				.catch((error) => {
					if (activeRequestRef.current !== request) return;
					activeRequestRef.current = null;
					if (
						request.controller.signal.aborted ||
						error?.name === "AbortError"
					) {
						setLoadingCities(false);
						return;
					}
					setSuggestions([]);
					setActiveSuggestionIndex(-1);
					setLoadingCities(false);
				});
		}, DEBOUNCE_MS);

		return () => {
			clearTimeout(debounceRef.current);
			invalidateActiveRequest();
		};
	}, [city]);

	function handleCityChange(value) {
		invalidateActiveRequest();
		skipSearchForCityRef.current = null;
		setCity(value);
		setActiveSuggestionIndex(-1);
		setLoadingCities(false);
		if (value.length < 2) {
			setSuggestions([]);
			setShowSuggestions(false);
		}
	}

	function selectCity(value) {
		clearTimeout(debounceRef.current);
		invalidateActiveRequest();
		skipSearchForCityRef.current = value;
		setCity(value);
		setShowSuggestions(false);
		setActiveSuggestionIndex(-1);
		setLoadingCities(false);
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
				if (event.key === "ArrowDown")
					return (current + 1) % suggestions.length;
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
