import { useCallback, useEffect, useRef, useState } from "react";
import { searchLodgings } from "../services/lodgingService";

export default function useHomeSearchResults(search) {
	const [searchState, setSearchState] = useState(null);
	const activeRequest = useRef(0);

	const fetchSearch = useCallback((query) => {
		const requestId = ++activeRequest.current;

		searchLodgings(query)
			.then((data) => {
				if (activeRequest.current === requestId) {
					setSearchState({ query, data, error: null });
				}
			})
			.catch((error) => {
				if (activeRequest.current === requestId) {
					setSearchState({ query, data: null, error });
				}
			});
	}, []);

	useEffect(() => {
		if (!search) {
			activeRequest.current += 1;
			return undefined;
		}

		fetchSearch(search);

		return () => {
			activeRequest.current += 1;
		};
	}, [fetchSearch, search]);

	const retrySearch = useCallback(() => {
		if (!search) return;

		setSearchState({ query: search, data: null, error: null });
		fetchSearch(search);
	}, [fetchSearch, search]);

	const isCurrentQuery = searchState?.query === search;
	return {
		searchResults: isCurrentQuery ? searchState.data : null,
		searchError: isCurrentQuery ? searchState.error : null,
		retrySearch,
	};
}
