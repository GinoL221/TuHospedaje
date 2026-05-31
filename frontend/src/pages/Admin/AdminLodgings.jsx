import { useState, useEffect } from "react";
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
  const size = 10;

  const fetchLodgings = () => {
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
  };

  const fetchData = (url, setter) => {
    get(url)
      .then((data) => setter(Array.isArray(data) ? data : []))
      .catch(() => {});
  };

  useEffect(() => {
    fetchLodgings();
  }, [page, size]);
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

  return (
    <>
      <div className="admin-toolbar">
        <button className="btn-add" onClick={() => setShowModal(true)}>
          + Agregar alojamiento
        </button>
      </div>
      <LodgingsTable
        lodgings={lodgings}
        page={page}
        totalPages={totalPages}
        onDelete={handleDelete}
        onPageChange={setPage}
      />
      {showModal && (
        <LodgingFormModal
          categories={categories}
          features={features}
          policies={policies}
          onSaved={() => {
            setPage(0);
            fetchLodgings();
          }}
          onClose={() => setShowModal(false)}
        />
      )}
    </>
  );
}
