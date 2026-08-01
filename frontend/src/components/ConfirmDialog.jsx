import { useEffect, useId, useRef, useState } from "react";

const FOCUSABLE_SELECTOR = [
  "button:not([disabled])",
  "[href]",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  '[tabindex]:not([tabindex="-1"])',
].join(",");

export default function ConfirmDialog({ show, message, onConfirm, onCancel, testId = "confirm-delete" }) {
  const [pending, setPending] = useState(false);
  const dialogRef = useRef(null);
  const cancelButtonRef = useRef(null);
  const pendingRef = useRef(false);
  const previousFocusRef = useRef(null);
  const messageId = useId();

  useEffect(() => {
    if (!show) return undefined;

    previousFocusRef.current = document.activeElement;
    cancelButtonRef.current?.focus();

    return () => {
      const previousFocus = previousFocusRef.current;
      if (
        previousFocus?.isConnected &&
        previousFocus.matches(FOCUSABLE_SELECTOR)
      ) {
        previousFocus.focus();
      }
    };
  }, [show]);

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

  const handleKeyDown = (event) => {
    if (event.key === "Escape") {
      event.preventDefault();
      handleCancel();
      return;
    }

    if (event.key !== "Tab") return;

    const focusableElements = Array.from(
      dialogRef.current?.querySelectorAll(FOCUSABLE_SELECTOR) ?? []
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

  return (
    <div className="modal-overlay" data-testid={testId} onClick={handleCancel} onKeyDown={handleKeyDown}>
      <div
        ref={dialogRef}
        className="modal confirm-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={messageId}
        onClick={(event) => event.stopPropagation()}
      >
        <p id={messageId} className="confirm-message">{message}</p>
        <div className="modal-actions">
          <button className="btn-save" data-testid={`${testId}-yes`} onClick={handleConfirm} disabled={pending}>Confirmar</button>
          <button ref={cancelButtonRef} className="btn-cancel" data-testid={`${testId}-no`} onClick={handleCancel} disabled={pending}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
