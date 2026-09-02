import { startTransition, useCallback, useEffect, useRef, useState } from "react";
import { getRecommendations } from "../services/lodgingService";

const RECOMMENDATIONS_STORAGE_ID = "tuhospedaje.recommendations.v1";

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
		const raw = sessionStorage.getItem(RECOMMENDATIONS_STORAGE_ID);
		if (!raw) return null;
		const parsed = JSON.parse(raw);
		return parsed && typeof parsed.seed === "string" ? parsed : null;
	} catch {
		return null;
	}
}

function writeStoredRecommendationSession(session) {
	try {
		sessionStorage.setItem(RECOMMENDATIONS_STORAGE_ID, JSON.stringify(session));
	} catch {
		// sessionStorage may be unavailable (e.g. private mode); the
		// recommendation session then simply lives only in memory.
	}
}

export default function useHomeRecommendations() {
	const [lodgings, setLodgings] = useState([]);
	const [page, setPage] = useState(0);
	const [confirmedPage, setConfirmedPage] = useState(0);
	const [totalPages, setTotalPages] = useState(1);
	const [recSeed, setRecSeed] = useState(
		() => readStoredRecommendationSession()?.seed ?? createRecommendationSeed(),
	);
	const revisionRef = useRef(readStoredRecommendationSession()?.revision ?? null);
	const [status, setStatus] = useState("idle");
	const [listBusy, setListBusy] = useState(false);
	const [listGeneration, setListGeneration] = useState(0);
	const requestIdRef = useRef(0);
	const skipResetPageFetchRef = useRef(false);

	useEffect(() => {
		writeStoredRecommendationSession({ seed: recSeed, revision: revisionRef.current });
	}, [recSeed]);

	const fetchRecommendations = useCallback(() => {
		const requestId = ++requestIdRef.current;
		startTransition(() => {
			setStatus("loading");
			setListBusy(true);
		});

		getRecommendations({ seed: recSeed, page, revision: revisionRef.current ?? undefined })
			.then((data) => {
				if (requestId !== requestIdRef.current) return;
				revisionRef.current = data.revision ?? revisionRef.current;
				writeStoredRecommendationSession({ seed: recSeed, revision: revisionRef.current });
				setLodgings(data.lodgings || []);
				setTotalPages(data.totalPages || 1);
				setListGeneration((generation) => generation + 1);
				setListBusy(false);
				setStatus("idle");
				setConfirmedPage((previousPage) => {
					const actualPage =
						typeof data.currentPage === "number" ? data.currentPage : page;
					return previousPage === actualPage ? previousPage : actualPage;
				});
				setPage((previousPage) => {
					const actualPage =
						typeof data.currentPage === "number" ? data.currentPage : previousPage;
					if (data.reset && previousPage !== actualPage) {
						skipResetPageFetchRef.current = true;
					}
					return previousPage === actualPage ? previousPage : actualPage;
				});
			})
			.catch(() => {
				if (requestId !== requestIdRef.current) return;
				setListBusy(false);
				setStatus("error");
			});
	}, [recSeed, page]);

	useEffect(() => {
		if (skipResetPageFetchRef.current) {
			skipResetPageFetchRef.current = false;
		} else {
			fetchRecommendations();
		}
	}, [fetchRecommendations]);

	function refresh() {
		revisionRef.current = null;
		setPage(0);
		setRecSeed(createRecommendationSeed());
	}

	return {
		lodgings,
		status,
		listBusy,
		listGeneration,
		page: confirmedPage,
		totalPages,
		setPage,
		refresh,
		retry: fetchRecommendations,
	};
}
