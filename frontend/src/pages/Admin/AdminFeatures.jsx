import { useState, useEffect, useRef } from "react";
import {
	createFeature,
	deleteFeature,
	getFeatures,
	updateFeature,
} from "../../services/adminCatalogService";
import ConfirmDialog from "../../components/ConfirmDialog";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import Icon from "../../components/Icons/Icon";
import IconPicker from "../../components/IconPicker/IconPicker";
import useTableData from "../../hooks/useTableData";
import SortableTh from "../../components/SortableTh/SortableTh";
import Pagination from "../../components/Pagination/Pagination";

export default function AdminFeatures() {
	const [featureList, setFeatureList] = useState([]);
	const {
		pageItems,
		sortKey,
		sortDir,
		requestSort,
		page,
		totalPages,
		setPage,
	} = useTableData(featureList);
	const [showModal, setShowModal] = useState(false);
	const [form, setForm] = useState({ name: "", icon: "" });
	const [editing, setEditing] = useState(null);
	const [error, setError] = useState("");
	const [fieldErrors, setFieldErrors] = useState({});
	const [initialLoadError, setInitialLoadError] = useState(false);
	const [loadAttempt, setLoadAttempt] = useState(0);
	const [deleteConfirm, setDeleteConfirm] = useState(null);
	const nameInputRef = useRef(null);
	const iconContainerRef = useRef(null);

	const resetForm = () => setForm({ name: "", icon: "" });
	const cancel = useConfirmCancel(form.name || form.icon, () => {
		setFieldErrors({});
		resetForm();
		setShowModal(false);
	});

	useEffect(() => {
		let cancelled = false;
		(async () => {
			try {
				const data = await getFeatures();
				if (!cancelled) {
					setFeatureList(Array.isArray(data) ? data : []);
					setInitialLoadError(false);
				}
			} catch (err) {
				if (!cancelled) {
					console.error(err);
					setInitialLoadError(true);
				}
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [loadAttempt]);

	const openModal = (feat = null) => {
		if (feat) {
			setForm({ name: feat.name, icon: feat.icon });
			setEditing(feat);
		} else {
			setForm({ name: "", icon: "" });
			setEditing(null);
		}
		setError("");
		setFieldErrors({});
		setShowModal(true);
	};

	const refresh = () => {
		getFeatures()
			.then((data) => setFeatureList(Array.isArray(data) ? data : []))
			.catch(() => {});
	};

	const validate = () => {
		const errs = {};
		if (!form.name.trim()) errs.name = "El nombre es obligatorio";
		if (!form.icon.trim()) errs.icon = "El ícono es obligatorio";
		return errs;
	};

	const handleSubmit = (e) => {
		e.preventDefault();
		setError("");

		const errs = validate();
		setFieldErrors(errs);
		if (Object.keys(errs).length > 0) {
			if (errs.name) {
				nameInputRef.current?.focus();
			} else if (errs.icon) {
				iconContainerRef.current?.querySelector("button")?.focus();
			}
			return;
		}

		const body = { name: form.name, icon: form.icon };
		const request = editing
			? updateFeature(editing.id, body)
			: createFeature(body);
		request
			.then(() => {
				setShowModal(false);
				refresh();
			})
			.catch((err) => setError(err.message));
	};

	const handleDelete = (id, name) => {
		setDeleteConfirm({ id, name });
	};

	const confirmDelete = async () => {
		if (!deleteConfirm) return;
		try {
			await deleteFeature(deleteConfirm.id);
			refresh();
		} catch (err) {
			alert(err.message);
		} finally {
			setDeleteConfirm(null);
		}
	};

	return (
		<>
			<button
				className="btn-fab"
				data-testid="admin-add-btn"
				onClick={() => openModal(null)}
			>
				+ Agregar característica
			</button>
			{initialLoadError && (
				<div role="alert">
					<p>No pudimos cargar las características.</p>
					<button
						type="button"
						onClick={() => setLoadAttempt((attempt) => attempt + 1)}
					>
						Reintentar
					</button>
				</div>
			)}
			{!initialLoadError && featureList.length === 0 && (
				<p className="empty-state">
					No hay características cargadas todavía. ¡Creá la primera!
				</p>
			)}
			{!initialLoadError && featureList.length > 0 && (
				<>
					<div
						className="admin-table-wrapper"
						role="region"
						tabIndex={0}
						aria-label="Tabla de características"
					>
						<table>
							<thead>
								<tr>
									<SortableTh
										columnKey="id"
										sortKey={sortKey}
										sortDir={sortDir}
										onSort={requestSort}
									>
										ID
									</SortableTh>
									<SortableTh
										columnKey="name"
										sortKey={sortKey}
										sortDir={sortDir}
										onSort={requestSort}
									>
										Nombre
									</SortableTh>
									<SortableTh
										columnKey="icon"
										sortKey={sortKey}
										sortDir={sortDir}
										onSort={requestSort}
									>
										Ícono
									</SortableTh>
									<th>Acciones</th>
								</tr>
							</thead>
							<tbody>
								{pageItems.map((f) => (
									<tr key={f.id} data-testid={`row-${f.id}`}>
										<td>{f.id}</td>
										<td>{f.name}</td>
										<td>
											<Icon name={f.icon} /> <code>{f.icon}</code>
										</td>
										<td>
											<button
												className="btn-edit"
												data-testid="row-edit-btn"
												onClick={() => openModal(f)}
											>
												Editar
											</button>
											<button
												className="btn-delete"
												data-testid="row-delete-btn"
												onClick={() => handleDelete(f.id, f.name)}
											>
												Eliminar
											</button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
					<Pagination
						page={page}
						totalPages={totalPages}
						onPageChange={setPage}
					/>
				</>
			)}

			{showModal && (
				<div className="modal-overlay" onClick={cancel.handleCancel}>
					<div
						className="modal"
						data-testid="admin-modal"
						onClick={(e) => e.stopPropagation()}
					>
						<h2>
							{editing ? "Editar característica" : "Nueva característica"}
						</h2>
						<form onSubmit={handleSubmit} noValidate>
							<label htmlFor="admin-feature-name" className="required-dot">
								Nombre
								<input
									ref={nameInputRef}
									id="admin-feature-name"
									name="name"
									type="text"
									required
									enterKeyHint="next"
									data-testid="field-name"
									value={form.name}
									className={fieldErrors.name ? "input-error" : ""}
									aria-invalid={Boolean(fieldErrors.name)}
									aria-describedby={
										fieldErrors.name ? "error-feature-name" : undefined
									}
									onChange={(e) => {
										setForm({ ...form, name: e.target.value });
										if (fieldErrors.name)
											setFieldErrors({ ...fieldErrors, name: "" });
									}}
								/>
								{fieldErrors.name && (
									<span
										id="error-feature-name"
										className="field-error"
										role="alert"
										data-testid="error-name"
									>
										{fieldErrors.name}
									</span>
								)}
							</label>
							<label className="required-dot">
								Ícono
								<div
									ref={iconContainerRef}
									aria-invalid={Boolean(fieldErrors.icon)}
									aria-describedby={
										fieldErrors.icon ? "error-feature-icon" : undefined
									}
								>
									<IconPicker
										value={form.icon}
										onChange={(val) => {
											setForm({ ...form, icon: val });
											if (fieldErrors.icon)
												setFieldErrors({ ...fieldErrors, icon: "" });
										}}
										placeholder="fa-solid fa-wifi"
									/>
								</div>
								{fieldErrors.icon && (
									<span
										id="error-feature-icon"
										className="field-error"
										role="alert"
										data-testid="error-icon"
									>
										{fieldErrors.icon}
									</span>
								)}
							</label>
							{error && (
								<p className="form-error" data-testid="admin-form-error">
									{error}
								</p>
							)}
							<p className="required-note">* Campos obligatorios</p>
							<div className="modal-actions">
								<button
									type="submit"
									className="btn-save"
									data-testid="admin-save-btn"
								>
									{editing ? "Guardar cambios" : "Crear"}
								</button>
								<button
									type="button"
									className="btn-cancel"
									data-testid="admin-cancel-btn"
									onClick={cancel.handleCancel}
								>
									Cancelar
								</button>
							</div>
						</form>
					</div>
				</div>
			)}

			<ConfirmDialog
				show={cancel.showConfirm}
				message="Hay cambios sin guardar. ¿Cancelar de todas formas?"
				onConfirm={() => {
					cancel.confirmCancel();
				}}
				onCancel={cancel.dismissConfirm}
				testId="confirm-cancel"
			/>

			<ConfirmDialog
				show={deleteConfirm !== null}
				message={
					deleteConfirm
						? `¿Eliminar característica "${deleteConfirm.name}"?`
						: ""
				}
				onConfirm={confirmDelete}
				onCancel={() => setDeleteConfirm(null)}
				testId="confirm-delete"
			/>
		</>
	);
}
