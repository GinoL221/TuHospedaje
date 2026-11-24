import { useEffect, useId, useRef, useState } from "react";
import {
	createLodging,
	updateLodging,
} from "../../services/adminCatalogService";
import useConfirmCancel from "../../hooks/useConfirmCancel";
import ConfirmDialog from "../../components/ConfirmDialog";
import ImageUpload from "../../components/ImageUpload/ImageUpload";
import Icon from "../Icons/Icon";

const FOCUSABLE_SELECTOR = [
	"button:not([disabled])",
	"[href]",
	'input:not([disabled]):not([type="hidden"]):not([tabindex="-1"])',
	"select:not([disabled])",
	"textarea:not([disabled])",
	'[tabindex]:not([tabindex="-1"])',
].join(",");

function haveSameIds(current, initial) {
	if (current.length !== initial.length) return false;
	const initialIds = new Set(initial.map(String));
	return current.every((id) => initialIds.has(String(id)));
}

export default function LodgingFormModal({
	lodging,
	categories,
	features,
	policies,
	onSaved,
	onClose,
}) {
	const isEdit = Boolean(lodging?.id);
	const dialogRef = useRef(null);
	const initialFocusRef = useRef(null);
	const previousFocusRef = useRef(document.activeElement);
	const submittingRef = useRef(false);
	const uploadingRef = useRef(false);
	const fieldRefs = useRef({});
	const imageUploadRef = useRef(null);
	const titleId = useId();
	const descriptionId = useId();
	const formId = useId();

	const [form, setForm] = useState({
		name: lodging?.name ?? "",
		description: lodging?.description ?? "",
		address: lodging?.address ?? "",
		city: lodging?.city ?? "",
		country: lodging?.country ?? "",
		phoneNumber: lodging?.phoneNumber ?? "",
		email: lodging?.email ?? "",
		pricePerNight: lodging?.pricePerNight ?? "",
		maxGuests: lodging?.maxGuests ?? "",
		categoryId: lodging?.categoryId ?? "",
		featureIds: lodging?.features?.map((f) => f.id) ?? [],
		policyIds: lodging?.policies?.map((p) => p.id) ?? [],
		imageUrls: lodging?.imageUrls ?? [],
	});
	const [error, setError] = useState("");
	const [fieldErrors, setFieldErrors] = useState({});
	const [submitting, setSubmitting] = useState(false);
	const [uploading, setUploading] = useState(false);

	useEffect(() => {
		const previousFocus = previousFocusRef.current;
		initialFocusRef.current?.focus();

		return () => {
			if (
				previousFocus?.isConnected &&
				previousFocus.matches(FOCUSABLE_SELECTOR)
			) {
				previousFocus.focus();
			}
		};
	}, []);

	const hasChanges = isEdit
		? form.name !== (lodging.name ?? "") ||
			form.description !== (lodging.description ?? "") ||
			form.address !== (lodging.address ?? "") ||
			form.city !== (lodging.city ?? "") ||
			form.country !== (lodging.country ?? "") ||
			form.phoneNumber !== (lodging.phoneNumber ?? "") ||
			form.email !== (lodging.email ?? "") ||
			String(form.pricePerNight) !== String(lodging.pricePerNight ?? "") ||
			String(form.maxGuests) !== String(lodging.maxGuests ?? "") ||
			String(form.categoryId) !== String(lodging.categoryId ?? "") ||
			!haveSameIds(form.featureIds, lodging.features?.map((f) => f.id) ?? []) ||
			!haveSameIds(form.policyIds, lodging.policies?.map((p) => p.id) ?? []) ||
			form.imageUrls.length !== (lodging.imageUrls ?? []).length ||
			form.imageUrls.some((url, index) => url !== lodging.imageUrls[index])
		: Boolean(
				form.name ||
				form.description ||
				form.address ||
				form.city ||
				form.country ||
				form.phoneNumber ||
				form.email ||
				form.pricePerNight ||
				form.maxGuests ||
				form.categoryId ||
				form.featureIds.length > 0 ||
				form.policyIds.length > 0 ||
				form.imageUrls.length > 0,
			);

	const cancel = useConfirmCancel(hasChanges, () => {
		setFieldErrors({});
		onClose();
	});

	const isPending = submitting || uploading;

	const requestClose = () => {
		if (submittingRef.current || uploadingRef.current) return;
		cancel.handleCancel();
	};

	const handleKeyDown = (event) => {
		if (cancel.showConfirm) return;

		if (event.key === "Escape") {
			event.preventDefault();
			requestClose();
			return;
		}

		if (event.key !== "Tab") return;

		const focusableElements = Array.from(
			dialogRef.current?.querySelectorAll(FOCUSABLE_SELECTOR) ?? [],
		);
		if (focusableElements.length === 0) {
			event.preventDefault();
			return;
		}

		const firstElement = focusableElements[0];
		const lastElement = focusableElements.at(-1);
		if (event.shiftKey && document.activeElement === firstElement) {
			event.preventDefault();
			lastElement.focus();
		} else if (!event.shiftKey && document.activeElement === lastElement) {
			event.preventDefault();
			firstElement.focus();
		}
	};

	const validate = () => {
		const errs = {};
		if (!form.name.trim()) errs.name = "El nombre es obligatorio";
		if (!form.description.trim())
			errs.description = "La descripción es obligatoria";
		if (!form.address?.trim()) errs.address = "La dirección es obligatoria";
		if (!form.city?.trim()) errs.city = "La ciudad es obligatoria";
		if (!form.country?.trim()) errs.country = "El país es obligatorio";
		if (!form.phoneNumber?.trim())
			errs.phoneNumber = "El teléfono es obligatorio";
		if (!form.email?.trim()) errs.email = "El email es obligatorio";
		else if (!/\S+@\S+\.\S+/.test(form.email))
			errs.email = "El email no es válido";
		if (form.pricePerNight === "")
			errs.pricePerNight = "El precio por noche es obligatorio";
		else if (Number(form.pricePerNight) <= 0)
			errs.pricePerNight = "El precio por noche debe ser mayor a cero";
		if (form.maxGuests === "")
			errs.maxGuests = "La capacidad máxima es obligatoria";
		else if (!Number.isInteger(Number(form.maxGuests)))
			errs.maxGuests = "La capacidad máxima debe ser un número entero";
		else if (Number(form.maxGuests) <= 0)
			errs.maxGuests = "La capacidad máxima debe ser mayor a cero";
		if (!form.imageUrls || form.imageUrls.length === 0)
			errs.imageUrls = "Debés agregar al menos una imagen";
		return errs;
	};

	const handleSubmit = async (e) => {
		e.preventDefault();
		if (submittingRef.current || uploadingRef.current) return;
		setError("");
		const errs = validate();
		setFieldErrors(errs);
		if (Object.keys(errs).length > 0) {
			const fieldOrder = [
				"name",
				"email",
				"description",
				"address",
				"city",
				"country",
				"phoneNumber",
				"pricePerNight",
				"maxGuests",
				"imageUrls",
			];
			const firstInvalid = fieldOrder.find((key) => errs[key]);
			if (firstInvalid === "imageUrls") {
				imageUploadRef.current?.querySelector(".btn-upload")?.focus();
			} else if (firstInvalid) {
				fieldRefs.current[firstInvalid]?.focus();
			}
			return;
		}
		const payload = {
			name: form.name,
			description: form.description,
			address: form.address,
			city: form.city,
			country: form.country,
			phoneNumber: form.phoneNumber,
			email: form.email,
			pricePerNight: Number(form.pricePerNight),
			maxGuests: Number(form.maxGuests),
			categoryId: form.categoryId || null,
			featureIds: form.featureIds,
			imageUrls: form.imageUrls || [],
			policyIds: form.policyIds || [],
		};

		submittingRef.current = true;
		setSubmitting(true);
		try {
			await (isEdit
				? updateLodging(lodging.id, payload)
				: createLodging(payload));
			onSaved();
			onClose();
		} catch (err) {
			setError(err.message);
		} finally {
			submittingRef.current = false;
			setSubmitting(false);
		}
	};

	function inputField(name, label, type, field, inputProps = {}) {
		const value = form[name];
		const error = fieldErrors[name];
		const fieldId = `${formId}-${name}`;
		return (
			<label htmlFor={fieldId} className={label ? "required-dot" : ""}>
				{field || name.charAt(0).toUpperCase() + name.slice(1)}
				<input
					id={fieldId}
					name={name}
					type={type || "text"}
					value={value}
					required={Boolean(label)}
					data-testid={`field-${name}`}
					className={error ? "input-error" : ""}
					aria-invalid={Boolean(error)}
					aria-describedby={
						error ? `${descriptionId}-${name}-error` : undefined
					}
					disabled={isPending}
					ref={(el) => {
						fieldRefs.current[name] = el;
						if (name === "name") initialFocusRef.current = el;
					}}
					{...inputProps}
					onChange={(e) => {
						setForm({ ...form, [name]: e.target.value });
						if (error) setFieldErrors({ ...fieldErrors, [name]: "" });
					}}
				/>
				{error && (
					<span
						id={`${descriptionId}-${name}-error`}
						className="field-error"
						role="alert"
						data-testid={`error-${name}`}
					>
						{error}
					</span>
				)}
			</label>
		);
	}

	return (
		<>
			<div
				className="modal-overlay"
				onClick={(event) => {
					if (event.target === event.currentTarget) requestClose();
				}}
				onKeyDown={handleKeyDown}
				aria-hidden={cancel.showConfirm ? "true" : undefined}
				inert={cancel.showConfirm ? true : undefined}
			>
				<div
					ref={dialogRef}
					className="modal modal-lg"
					data-testid="admin-modal"
					role="dialog"
					aria-modal="true"
					aria-labelledby={titleId}
					aria-describedby={descriptionId}
					onClick={(e) => e.stopPropagation()}
				>
					<h2 id={titleId}>
						{isEdit ? "Editar alojamiento" : "Nuevo alojamiento"}
					</h2>
					<form onSubmit={handleSubmit} noValidate>
						<div className="modal-form-grid">
							{inputField("name", true, "text", "Nombre del alojamiento", {
								enterKeyHint: "next",
							})}
							{inputField("email", true, "email", "Correo electrónico", {
								autoComplete: "email",
								spellCheck: false,
								enterKeyHint: "next",
							})}
							<label
								htmlFor={`${formId}-description`}
								className="full-width required-dot"
							>
								Descripción
								<textarea
									id={`${formId}-description`}
									name="description"
									ref={(el) => {
										fieldRefs.current.description = el;
									}}
									required
									data-testid="field-description"
									value={form.description}
									className={fieldErrors.description ? "input-error" : ""}
									aria-invalid={Boolean(fieldErrors.description)}
									aria-describedby={
										fieldErrors.description
											? `${descriptionId}-description-error`
											: undefined
									}
									disabled={isPending}
									onChange={(e) => {
										setForm({ ...form, description: e.target.value });
										if (fieldErrors.description)
											setFieldErrors({ ...fieldErrors, description: "" });
									}}
								/>
								{fieldErrors.description && (
									<span
										id={`${descriptionId}-description-error`}
										className="field-error"
										role="alert"
										data-testid="error-description"
									>
										{fieldErrors.description}
									</span>
								)}
							</label>
							{inputField("address", true, "text", "Dirección", {
								autoComplete: "street-address",
								enterKeyHint: "next",
							})}
							{inputField("city", true, "text", "Ciudad", {
								autoComplete: "address-level2",
								enterKeyHint: "next",
							})}
							{inputField("country", true, "text", "País", {
								autoComplete: "country-name",
								enterKeyHint: "next",
							})}
							{inputField("phoneNumber", true, "tel", "Teléfono", {
								autoComplete: "tel",
								enterKeyHint: "next",
							})}
							{inputField(
								"pricePerNight",
								true,
								"number",
								"Precio por noche (ARS)",
								{
									min: "0.01",
									step: "0.01",
									enterKeyHint: "next",
								},
							)}
							{inputField(
								"maxGuests",
								true,
								"number",
								"Capacidad máxima de huéspedes",
								{
									min: "1",
									step: "1",
									enterKeyHint: "next",
								},
							)}
							<label htmlFor={`${formId}-categoryId`}>
								Categoría
								<select
									id={`${formId}-categoryId`}
									name="categoryId"
									value={form.categoryId || ""}
									disabled={isPending}
									onChange={(e) =>
										setForm({ ...form, categoryId: e.target.value })
									}
								>
									<option value="">Sin categoría</option>
									{categories.map((c) => (
										<option key={c.id} value={c.id}>
											{c.name}
										</option>
									))}
								</select>
							</label>
							<div className="full-width field-group">
								<span className="field-label">Características</span>
								<div className="feature-checkboxes">
									{features.map((f) => (
										<label key={f.id} className="feature-checkbox">
											<input
												type="checkbox"
												name="featureIds"
												value={f.id}
												checked={form.featureIds.includes(f.id)}
												disabled={isPending}
												onChange={(e) => {
													const id = Number(e.target.value);
													setForm({
														...form,
														featureIds: e.target.checked
															? [...form.featureIds, id]
															: form.featureIds.filter((fid) => fid !== id),
													});
												}}
											/>
											<Icon name={f.icon} /> {f.name}
										</label>
									))}
								</div>
							</div>
							<div className="full-width field-group">
								<span className="field-label">Políticas</span>
								<div className="feature-checkboxes">
									{policies.map((p) => (
										<label key={p.id} className="feature-checkbox">
											<input
												type="checkbox"
												name="policyIds"
												value={p.id}
												checked={form.policyIds?.includes(p.id)}
												disabled={isPending}
												onChange={(e) => {
													const id = Number(e.target.value);
													setForm({
														...form,
														policyIds: e.target.checked
															? [...(form.policyIds || []), id]
															: (form.policyIds || []).filter(
																	(pid) => pid !== id,
																),
													});
												}}
											/>
											<Icon name={p.icon} /> {p.name}
										</label>
									))}
								</div>
							</div>
							<div
								ref={imageUploadRef}
								className="full-width"
								role="group"
								aria-label="Imágenes"
								aria-invalid={Boolean(fieldErrors.imageUrls)}
								aria-describedby={
									fieldErrors.imageUrls
										? `${descriptionId}-imageUrls-error`
										: undefined
								}
							>
								<ImageUpload
									urls={form.imageUrls}
									onUrlsChange={(urls) => {
										setForm({ ...form, imageUrls: urls });
										if (fieldErrors.imageUrls && urls.length > 0) {
											setFieldErrors({ ...fieldErrors, imageUrls: "" });
										}
									}}
									disabled={submitting}
									onUploadingChange={(nextUploading) => {
										uploadingRef.current = nextUploading;
										setUploading(nextUploading);
									}}
								/>
							</div>
							{fieldErrors.imageUrls && (
								<p
									id={`${descriptionId}-imageUrls-error`}
									className="field-error full-width"
									role="alert"
									data-testid="error-imageUrls"
								>
									{fieldErrors.imageUrls}
								</p>
							)}
							{error && <p className="form-error full-width">{error}</p>}
							<p id={descriptionId} className="required-note full-width">
								* Campos obligatorios
							</p>
							<div className="modal-actions full-width">
								<button
									type="submit"
									className="btn-save"
									data-testid="admin-save-btn"
									disabled={isPending}
								>
									{isEdit ? "Guardar cambios" : "Guardar"}
								</button>
								<button
									type="button"
									className="btn-cancel"
									data-testid="admin-cancel-btn"
									onClick={requestClose}
									disabled={isPending}
								>
									Cancelar
								</button>
							</div>
						</div>
					</form>
				</div>
			</div>

			<ConfirmDialog
				show={cancel.showConfirm}
				message="Hay cambios sin guardar. ¿Cancelar de todas formas?"
				onConfirm={() => {
					cancel.confirmCancel();
				}}
				onCancel={cancel.dismissConfirm}
				testId="confirm-cancel"
			/>
		</>
	);
}
