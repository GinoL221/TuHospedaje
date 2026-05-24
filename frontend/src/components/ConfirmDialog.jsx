export default function ConfirmDialog({ show, message, onConfirm, onCancel }) {
  if (!show) return null;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <p className="confirm-message">{message}</p>
        <div className="modal-actions">
          <button className="btn-save" onClick={onConfirm}>Confirmar</button>
          <button className="btn-cancel" onClick={onCancel}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
