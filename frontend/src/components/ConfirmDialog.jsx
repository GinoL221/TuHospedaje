import { useState } from "react";

export default function ConfirmDialog({ show, message, onConfirm, onCancel, testId = "confirm-delete" }) {
  const [pending, setPending] = useState(false);

  if (!show) return null;

  const handleConfirm = async () => {
    if (pending) return;
    setPending(true);
    try {
      await onConfirm();
    } catch (error) {
      console.error(error);
    } finally {
      setPending(false);
    }
  };

  const handleCancel = () => {
    if (pending) return;
    onCancel();
  };

  return (
    <div className="modal-overlay" data-testid={testId} onClick={handleCancel}>
      <div className="modal confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <p className="confirm-message">{message}</p>
        <div className="modal-actions">
          <button className="btn-save" data-testid={`${testId}-yes`} onClick={handleConfirm} disabled={pending}>Confirmar</button>
          <button className="btn-cancel" data-testid={`${testId}-no`} onClick={handleCancel} disabled={pending}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
