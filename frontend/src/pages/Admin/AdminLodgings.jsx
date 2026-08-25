import { useState, useEffect, useCallback } from "react";
import { get, del } from "../../services/api";
import LodgingFormModal from "../../components/LodgingFormModal/LodgingFormModal";
import LodgingsTable from "../../components/LodgingsTable/LodgingsTable";
import ConfirmDialog from "../../components/ConfirmDialog";

export default function AdminLodgings() {
	const [lodgings, setLodgings] = useState([]);
	const [categories, setCategories] = useState([]);
	const [features, setFeatures] = useState([]);
	const [policies, setPolicies] = useState([]);
	const [showModal, setShowModal] = useState(false);
	const [editingLodging, setEditingLodging] = useState(null);
	const [deleteConfirm, setDeleteConfirm] = useState(null);
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);
	const [sortKey, setSortKey] = useState("id");
	const [sortDir, setSortDir] = useState("asc");
	const [search, setSearch] = useState("");

	const fetchLodgings = useCallback(() => {
		const params = new URLSearchParams({
			page: String(page),
			size: "10",
			sort: sortKey,
			direction: sortDir,
		});
		if (search.trim()) params.set("q", search.trim());

		get(`/lodgings/admin?${params.toString()}`)
			.then((data) => {
				const items = Array.isArray(data?.items) ? data.items : [];
				const nextTotalPages = data?.totalPages ?? 0;

				if (items.length === 0 && page > 0 && nextTotalPages > 0) {
					setTotalPages(nextTotalPages);
					setPage(nextTotalPages - 1);
					return;
				}

				setLodgings(items);
				setTotalPages(nextTotalPages);
			})
			.catch(console.error);
	}, [page, search, sortDir, sortKey]);

	const fetchData = (url, setter) => {
		get(url)
			.then((data) => setter(Array.isArray(data) ? data : []))
			.catch(() => {});
	};

	useEffect(() => {
		fetchLodgings();
	}, [fetchLodgings]);
	useEffect(() => {
		fetchData("/categories", setCategories);
	}, []);
	useEffect(() => {
		fetchData("/features", setFeatures);
	}, []);
	useEffect(() => {
		fetchData("/policies", setPolicies);
	}, []);

	const handleDelete = (id, name) => {
		setDeleteConfirm({ id, name });
	};

	const confirmDelete = async () => {
		if (!deleteConfirm) return;
		try {
			await del(`/lodgings/${deleteConfirm.id}`);
			fetchLodgings();
		} catch (err) {
			console.error(err);
		} finally {
			setDeleteConfirm(null);
		}
	};

	const handleEdit = (lodging) => {
		setEditingLodging(lodging);
		setShowModal(true);
	};

	const handleSort = (key) => {
		setPage(0);
		if (key === sortKey) {
			setSortDir((current) => (current === "asc" ? "desc" : "asc"));
		} else {
			setSortKey(key);
			setSortDir("asc");
		}
	};

	const handleSearchChange = (event) => {
		setSearch(event.target.value);
		setPage(0);
	};

	const handleCloseModal = () => {
		setShowModal(false);
		setEditingLodging(null);
	};

	return (
		<>
			<h2>Lista de productos</h2>
			<button
				className="btn-fab"
				data-testid="admin-add-btn"
				onClick={() => setShowModal(true)}
			>
				+ Agregar alojamiento
			</button>
			<div className="admin-table-controls">
				<label htmlFor="admin-lodgings-search">
					Buscar alojamientos
					<input
						id="admin-lodgings-search"
						aria-label="Buscar alojamientos"
						value={search}
						onChange={handleSearchChange}
						placeholder="Buscar por nombre, ciudad o país"
					/>
				</label>
			</div>
			<LodgingsTable
				lodgings={lodgings}
				onDelete={handleDelete}
				onEdit={handleEdit}
				sortKey={sortKey}
				sortDir={sortDir}
				onSort={handleSort}
				page={page}
				totalPages={totalPages}
				onPageChange={setPage}
			/>
			<ConfirmDialog
				show={deleteConfirm !== null}
				message={
					deleteConfirm
						? `¿Eliminar el alojamiento "${deleteConfirm.name}"? Esta acción no se puede deshacer.`
						: ""
				}
				onConfirm={confirmDelete}
				onCancel={() => setDeleteConfirm(null)}
				testId="confirm-delete"
			/>
			{showModal && (
				<LodgingFormModal
					lodging={editingLodging}
					categories={categories}
					features={features}
					policies={policies}
					onSaved={() => {
						fetchLodgings();
					}}
					onClose={handleCloseModal}
				/>
			)}
		</>
	);
}
