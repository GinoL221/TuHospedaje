import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { get } from "../../services/api";
import "./ProductDetail.css";

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lodging, setLodging] = useState(null);
  const [showAllImages, setShowAllImages] = useState(false);
  useEffect(() => {
    get(`/lodgings/${id}`).then(setLodging).catch(console.error);
  }, [id]);
  if (!lodging)
    return (
      <main className="page-container">
        <p>Cargando...</p>
      </main>
    );
  const images = lodging.imageUrls || [];
  return (
    <main className="page-container product-detail">
      <div className="detail-header">
        <h1>{lodging.name}</h1>
        <button className="back-arrow" onClick={() => navigate(-1)}>
          ←
        </button>
      </div>
      <p className="location">
        {lodging.city}, {lodging.country}
      </p>
      {images.length > 0 && (
        <div className="gallery-wrapper">
          <div className="gallery">
            <div className="gallery-main">
              <img src={images[0]} alt={`${lodging.name} - 1`} />
            </div>
            {images.length > 1 && (
              <div className="gallery-grid">
                {images.slice(1, 5).map((url, i) => (
                  <img key={i} src={url} alt={`${lodging.name} - ${i + 2}`} />
                ))}
              </div>
            )}
          </div>
          {images.length > 5 && !showAllImages && (
            <button
              className="btn-show-more"
              onClick={() => setShowAllImages(true)}
            >
              Ver más
            </button>
          )}
          {showAllImages && (
            <>
              <div className="gallery-all">
                {images.slice(5).map((url, i) => (
                  <img
                    key={i + 5}
                    src={url}
                    alt={`${lodging.name} - ${i + 6}`}
                  />
                ))}
              </div>
              <button
                className="btn-show-more"
                onClick={() => setShowAllImages(false)}
              >
                Ver menos
              </button>
            </>
          )}
        </div>
      )}
      <section className="description">
        <h2>Descripción</h2>
        <p>{lodging.description}</p>
      </section>
    </main>
  );
}
