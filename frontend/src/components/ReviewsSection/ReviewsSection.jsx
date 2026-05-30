import { useState, useEffect } from "react";
import { get, post } from "../../services/api";
import "./ReviewsSection.css";

export default function ReviewsSection({ lodgingId, user }) {
  const [ratings, setRatings] = useState([]);
  const [avgScore, setAvgScore] = useState(0);
  const [totalReviews, setTotalReviews] = useState(0);
  const [userScore, setUserScore] = useState(0);
  const [userComment, setUserComment] = useState("");
  const [hoverScore, setHoverScore] = useState(0);

  useEffect(() => {
    get(`/ratings/lodging/${lodgingId}`)
      .then((data) => {
        setAvgScore(data.average || 0);
        setTotalReviews(data.count || 0);
        setRatings(Array.isArray(data.ratings) ? data.ratings : []);
      })
      .catch(() => {});
  }, [lodgingId]);

  async function submitRating() {
    if (userScore === 0) return;
    try {
      await post("/ratings", {
        lodgingId,
        score: userScore,
        comment: userComment,
      });
      const data = await get(`/ratings/lodging/${lodgingId}`);
      setAvgScore(data.average || 0);
      setTotalReviews(data.count || 0);
      setRatings(Array.isArray(data.ratings) ? data.ratings : []);
      setUserScore(0);
      setUserComment("");
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <section className="ratings-section">
      <h2>Reseñas</h2>
      <div className="ratings-header">
        <span className="avg-score">{avgScore.toFixed(1)}</span>
        <span className="stars-display">
          {[1, 2, 3, 4, 5].map((s) => (
            <span key={s} className={s <= Math.round(avgScore) ? "star-filled" : "star-empty"}>
              ★
            </span>
          ))}
        </span>
        <span className="reviews-count">({totalReviews} reseñas)</span>
      </div>

      {user && (
        <div className="review-form">
          <h3>Dejá tu reseña</h3>
          <div className="star-selector">
            {[1, 2, 3, 4, 5].map((s) => (
              <span
                key={s}
                className={s <= (hoverScore || userScore) ? "star-filled" : "star-empty"}
                onClick={() => setUserScore(s)}
                onMouseEnter={() => setHoverScore(s)}
                onMouseLeave={() => setHoverScore(0)}
              >
                ★
              </span>
            ))}
          </div>
          <textarea
            value={userComment}
            onChange={(e) => setUserComment(e.target.value)}
            placeholder="Contá tu experiencia..."
            rows={3}
          />
          <button
            className="btn-submit-review"
            onClick={submitRating}
            disabled={userScore === 0}
          >
            Enviar reseña
          </button>
        </div>
      )}

      <div className="reviews-list">
        {ratings.map((r) => (
          <div key={r.id} className="review-item">
            <div className="review-header">
              <strong>{r.userName}</strong>
              <span className="review-date">
                {new Date(r.createdAt).toLocaleDateString()}
              </span>
            </div>
            <div className="review-stars">
              {[1, 2, 3, 4, 5].map((s) => (
                <span key={s} className={s <= r.score ? "star-filled" : "star-empty"}>
                  ★
                </span>
              ))}
            </div>
            {r.comment && <p className="review-comment">{r.comment}</p>}
          </div>
        ))}
      </div>
    </section>
  );
}
