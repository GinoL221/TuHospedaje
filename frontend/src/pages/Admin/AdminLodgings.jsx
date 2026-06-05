import { useState, useEffect, useCallback } from "react";
import { get, del } from "../../services/api";
import LodgingFormModal from "../../components/LodgingFormModal/LodgingFormModal";
import LodgingsTable from "../../components/LodgingsTable/LodgingsTable";

export default function AdminLodgings() {
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [features, setFeatures] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editingLodging, setEditingLodging] = useState(null);
  const size = 10;

  const fetchLodgings = useCallback(() => {
    get(`/lodgings?page=${page}&size=${size}`)
      .then((data) => {
        if (Array.isArray(data)) {
          setLodgings(data);
          setTotalPages(1);
          return;
        }
        setLodgings(data.lodgings || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(console.error);
  }, [page, size]);

  const fetchData = (url, setter) => {
    get(url)
      .then((data) => setter(Array.isArray(data) ? data : []))
      .catch(() => {});
  };

  useEffect(() => {
    fetchLodgings();
  }, [fetchLodgings]);
  useEffect(() => {
    fetchData("/categories", setCategories);
  }, []);
  useEffect(() => {
    fetchData("/features", setFeatures);
  }, []);
  useEffect(() => {
    fetchData("/policies", setPolicies);
  }, []);

  const handleDelete = async (id, name) => {
    if (window.confirm(`¿Eliminar "${name}"?`)) {
      try {
        await del(`/lodgings/${id}`);
        const data = await get(`/lodgings?page=${page}&size=${size}`);
        const items = data.lodgings || data || [];
        if (Array.isArray(data)) {
          setLodgings(data);
          setTotalPages(1);
        } else {
          setLodgings(data.lodgings || []);
          setTotalPages(data.totalPages || 0);
        }
        if (items.length === 0 && page > 0) setPage((p) => p - 1);
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleEdit = (lodging) => {
    setEditingLodging(lodging);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingLodging(null);
  };

  return (
    <>
      <button className="btn-fab" onClick={() => setShowModal(true)}>
        + Agregar alojamiento
      </button>
      <LodgingsTable
        lodgings={lodgings}
        page={page}
        totalPages={totalPages}
        onDelete={handleDelete}
        onEdit={handleEdit}
        onPageChange={setPage}
      />
      {showModal && (
        <LodgingFormModal
          lodging={editingLodging}
          categories={categories}
          features={features}
          policies={policies}
          onSaved={() => {
            setPage(0);
            fetchLodgings();
          }}
          onClose={handleCloseModal}
        />
      )}
    </>
  );
}
