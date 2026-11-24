import { useId, useRef, useState } from "react";
import ModalDialog from "./ModalDialog/ModalDialog";
import "./ConfirmDialog.css";

export default function ConfirmDialog({
	show,
	message,
	onConfirm,
	onCancel,
	testId = "confirm-delete",
}) {
	const [pending, setPending] = useState(false);
	const pendingRef = useRef(false);
	const cancelButtonRef = useRef(null);
	const messageId = useId();

	if (!show) return null;

	const handleConfirm = async () => {
		if (pendingRef.current) return;
		pendingRef.current = true;
		setPending(true);
		try {
			await onConfirm();
		} catch (error) {
			console.error(error);
		} finally {
			pendingRef.current = false;
			setPending(false);
		}
	};

	const handleCancel = () => {
		if (pendingRef.current) return;
		onCancel();
	};

	const handleDialogCancel = (event) => {
		if (pendingRef.current) {
			event.preventDefault();
		}
	};

	return (
		<ModalDialog
			open={show}
			className="modal confirm-dialog"
			ariaLabelledBy={messageId}
			closedBy={pending ? "none" : "any"}
			initialFocusRef={cancelButtonRef}
			onCancel={handleDialogCancel}
			onClose={handleCancel}
			data-testid={testId}
		>
			<p id={messageId} className="confirm-message">
				{message}
			</p>
			<div className="modal-actions">
				<button
					type="button"
					className="btn-save btn-confirm"
					data-testid={`${testId}-yes`}
					onClick={handleConfirm}
					disabled={pending}
				>
					Confirmar
				</button>
				<button
					ref={cancelButtonRef}
					type="button"
					className="btn-cancel"
					data-testid={`${testId}-no`}
					onClick={handleCancel}
					disabled={pending}
					autoFocus
				>
					Cancelar
				</button>
			</div>
		</ModalDialog>
	);
}
