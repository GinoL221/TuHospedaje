import "@testing-library/jest-dom/vitest";

// jsdom does not implement HTMLDialogElement.prototype.showModal / close.
// Provide a minimal spec-faithful stub so native <dialog> components work in
// unit tests without custom Escape listeners or focus-return hacks.
if (
	typeof HTMLDialogElement !== "undefined" &&
	typeof HTMLDialogElement.prototype.showModal !== "function"
) {
	const previousFocusMap = new WeakMap();
	const openModalDialogs = [];
	let lastExternalFocus = null;

	document.addEventListener("focusin", (event) => {
		if (
			event.target instanceof HTMLElement &&
			!event.target.closest("dialog:not([open])")
		) {
			lastExternalFocus = event.target;
		}
	});

	HTMLDialogElement.prototype.showModal = function showModal() {
		if (this.open) return;
		const invoker =
			document.activeElement && this.contains(document.activeElement)
				? lastExternalFocus
				: document.activeElement;
		previousFocusMap.set(this, invoker);
		this.open = true;
		this.setAttribute("open", "");
		openModalDialogs.push(this);

		const autoFocusTarget = this.querySelector("[autofocus]");
		if (autoFocusTarget && typeof autoFocusTarget.focus === "function") {
			autoFocusTarget.focus();
		}
	};

	HTMLDialogElement.prototype.close = function close(returnValue) {
		if (!this.open) return;
		if (returnValue !== undefined) {
			this.returnValue = String(returnValue);
		}
		this.open = false;
		this.removeAttribute("open");
		const idx = openModalDialogs.lastIndexOf(this);
		if (idx !== -1) {
			openModalDialogs.splice(idx, 1);
		}
		this.dispatchEvent(new Event("close"));

		const previousFocus = previousFocusMap.get(this);
		previousFocusMap.delete(this);
		if (
			previousFocus &&
			previousFocus.isConnected &&
			typeof previousFocus.focus === "function"
		) {
			previousFocus.focus();
		}
	};

	document.addEventListener("keydown", (event) => {
		if (event.key !== "Escape" || event.defaultPrevented) return;
		while (openModalDialogs.length > 0) {
			const topDialog = openModalDialogs.at(-1);
			if (!topDialog.isConnected || !topDialog.open) {
				openModalDialogs.pop();
				continue;
			}
			const cancelEvent = new Event("cancel", { cancelable: true });
			const notPrevented = topDialog.dispatchEvent(cancelEvent);
			if (notPrevented) {
				topDialog.close();
			}
			break;
		}
	});
}
