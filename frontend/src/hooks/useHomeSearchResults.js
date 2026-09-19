import { useEffect, useState } from "react";
import { searchLodgings } from "../services/lodgingService";

const EMPTY_SEARCH_RESULTS = { lodgings: [], totalItems: 0, catalogItems: 0 };

export default function useHomeSearchResults(search) {
	const [searchResults, setSearchResults] = useState(null);

	useEffect(() => {
		if (!search) return undefined;

		let isCurrentSearch = true;
		searchLodgings(search)
			.then((data) => {
				if (isCurrentSearch) setSearchResults({ query: search, data });
			})
			.catch(() => {
				if (isCurrentSearch) {
					setSearchResults({ query: search, data: EMPTY_SEARCH_RESULTS });
				}
			});

		return () => {
			isCurrentSearch = false;
		};
	}, [search]);

	return {
		searchResults: searchResults?.query === search ? searchResults.data : null,
	};
}
