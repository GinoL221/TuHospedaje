export default function Pagination({ page, totalPages, onPageChange, className = "pagination" }) {
  if (totalPages <= 1) return null;

  return (
    <div className={className}>
      <button className="btn-extremo" disabled={page === 0} onClick={() => onPageChange(0)}>
        Primera
      </button>
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        Anterior
      </button>
      <span>
        Página {page + 1} de {totalPages}
      </span>
      <button
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Siguiente
      </button>
      <button
        className="btn-extremo"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(totalPages - 1)}
      >
        Última
      </button>
    </div>
  );
}
