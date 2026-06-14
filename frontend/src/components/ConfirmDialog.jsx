export default function ConfirmDialog({ show, message, onConfirm, onCancel, testId = "confirm-delete" }) {
  if (!show) return null;

  return (
    <div className="modal-overlay" data-testid={testId} onClick={onCancel}>
      <div className="modal confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <p className="confirm-message">{message}</p>
        <div className="modal-actions">
          <button className="btn-save" data-testid={`${testId}-yes`} onClick={onConfirm}>Confirmar</button>
          <button className="btn-cancel" data-testid={`${testId}-no`} onClick={onCancel}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
