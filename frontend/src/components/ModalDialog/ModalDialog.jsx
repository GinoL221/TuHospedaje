import { useEffect, useLayoutEffect, useRef } from "react";
import "./ModalDialog.css";

function isOutsideDialog(event, dialog) {
	if (event.target !== dialog) return false;
	const rect = dialog.getBoundingClientRect();
	if (rect.width === 0 && rect.height === 0) {
		return true;
	}
	return (
		event.clientX < rect.left ||
		event.clientX > rect.right ||
		event.clientY < rect.top ||
		event.clientY > rect.bottom
	);
}

export default function ModalDialog({
	open = true,
	ariaLabel,
	ariaLabelledBy,
	ariaDescribedBy,
	className = "",
	closedBy = "any",
	initialFocusRef,
	onClose,
	onCancel,
	children,
	...rest
}) {
	const dialogRef = useRef(null);
	const openRef = useRef(open);
	const onCloseRef = useRef(onClose);
	const onCancelRef = useRef(onCancel);
	const pointerDownOutsideRef = useRef(false);
	const closingProgrammaticallyRef = useRef(false);

	useEffect(() => {
		onCloseRef.current = onClose;
		onCancelRef.current = onCancel;
	}, [onClose, onCancel]);

	useLayoutEffect(() => {
		openRef.current = open;
		const dialog = dialogRef.current;
		if (!dialog) return undefined;

		if (open && !dialog.open) {
			dialog.showModal();
			initialFocusRef?.current?.focus();
		} else if (!open && dialog.open) {
			closingProgrammaticallyRef.current = true;
			dialog.close();
			closingProgrammaticallyRef.current = false;
		}

		return () => {
			if (dialog.open) {
				closingProgrammaticallyRef.current = true;
				dialog.close();
				closingProgrammaticallyRef.current = false;
			}
		};
	}, [open, initialFocusRef]);

	useEffect(() => {
		const dialog = dialogRef.current;
		if (!dialog) return undefined;

		const triggerCancel = (event) => {
			if (onCancelRef.current) {
				onCancelRef.current(event);
				if (event.defaultPrevented) {
					return;
				}
			}
			if (event.cancelable) {
				event.preventDefault();
				onCloseRef.current?.();
			}
		};

		const handleClose = () => {
			dialog.returnValue = "";
			if (!closingProgrammaticallyRef.current) {
				onCloseRef.current?.();
				queueMicrotask(() => {
					if (dialog.isConnected && !dialog.open && openRef.current) {
						dialog.showModal();
					}
				});
			}
		};

		dialog.addEventListener("cancel", triggerCancel);
		dialog.addEventListener("close", handleClose);

		const needsLightDismissFallback =
			closedBy === "any" &&
			typeof HTMLDialogElement !== "undefined" &&
			!("closedBy" in HTMLDialogElement.prototype);

		const handlePointerDown = (event) => {
			pointerDownOutsideRef.current = isOutsideDialog(event, dialog);
		};

		const handlePointerUp = (event) => {
			const startedOutside = pointerDownOutsideRef.current;
			pointerDownOutsideRef.current = false;
			if (!startedOutside || !isOutsideDialog(event, dialog)) {
				return;
			}
			const cancelEvent = new Event("cancel", { cancelable: true });
			triggerCancel(cancelEvent);
		};

		if (needsLightDismissFallback) {
			dialog.addEventListener("pointerdown", handlePointerDown);
			dialog.addEventListener("pointerup", handlePointerUp);
		}

		return () => {
			dialog.removeEventListener("cancel", triggerCancel);
			dialog.removeEventListener("close", handleClose);
			if (needsLightDismissFallback) {
				dialog.removeEventListener("pointerdown", handlePointerDown);
				dialog.removeEventListener("pointerup", handlePointerUp);
			}
		};
	}, [closedBy]);

	const classes = className ? `modal-dialog ${className}` : "modal-dialog";

	return (
		<dialog
			ref={dialogRef}
			className={classes}
			aria-label={ariaLabel}
			aria-labelledby={ariaLabelledBy}
			aria-describedby={ariaDescribedBy}
			closedby={closedBy}
			{...rest}
		>
			{children}
		</dialog>
	);
}
