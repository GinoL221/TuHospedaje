import { useState, useEffect, useCallback } from "react";
import { get, del } from "../../services/api";
import LodgingFormModal from "../../components/LodgingFormModal/LodgingFormModal";
import LodgingsTable from "../../components/LodgingsTable/LodgingsTable";
import ConfirmDialog from "../../components/ConfirmDialog";

export default function AdminLodgings() {
  const [lodgings, setLodgings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [features, setFeatures] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editingLodging, setEditingLodging] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const fetchLodgings = useCallback(() => {
    get("/lodgings")
      .then((data) => {
        setLodgings(Array.isArray(data) ? data : (data.lodgings || []));
      })
      .catch(console.error);
  }, []);

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

  const handleDelete = (id, name) => {
    setDeleteConfirm({ id, name });
  };

  const confirmDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await del(`/lodgings/${deleteConfirm.id}`);
      fetchLodgings();
    } catch (err) {
      console.error(err);
    } finally {
      setDeleteConfirm(null);
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
      <button className="btn-fab" data-testid="admin-add-btn" onClick={() => setShowModal(true)}>
        + Agregar alojamiento
      </button>
      <LodgingsTable
        lodgings={lodgings}
        onDelete={handleDelete}
        onEdit={handleEdit}
      />
      <ConfirmDialog
        show={deleteConfirm !== null}
        message={deleteConfirm ? `¿Eliminar el alojamiento "${deleteConfirm.name}"? Esta acción no se puede deshacer.` : ""}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteConfirm(null)}
        testId="confirm-delete"
      />
      {showModal && (
        <LodgingFormModal
          lodging={editingLodging}
          categories={categories}
          features={features}
          policies={policies}
          onSaved={() => {
            fetchLodgings();
          }}
          onClose={handleCloseModal}
        />
      )}
    </>
  );
}
