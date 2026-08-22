import { useState, useEffect } from "react";
import { get, post } from "../../services/api";
import useRatingEligibility from "../../hooks/useRatingEligibility";
import "./ReviewsSection.css";

const STAR_VALUES = [1, 2, 3, 4, 5];

function starLabel(score) {
  return `${score} estrella${score === 1 ? "" : "s"}`;
}

export default function ReviewsSection({ lodgingId, user }) {
  const [ratings, setRatings] = useState([]);
  const [avgScore, setAvgScore] = useState(0);
  const [totalReviews, setTotalReviews] = useState(0);
  const [userScore, setUserScore] = useState(0);
  const [userComment, setUserComment] = useState("");
  const [hoverScore, setHoverScore] = useState(0);
  const [submitError, setSubmitError] = useState(null);
  const { status: eligibilityStatus, load: loadEligibility } =
    useRatingEligibility(lodgingId);

  useEffect(() => {
    get(`/ratings/lodging/${lodgingId}`)
      .then((data) => {
        setAvgScore(data.average || 0);
        setTotalReviews(data.count || 0);
        setRatings(Array.isArray(data.ratings) ? data.ratings : []);
      })
      .catch(() => {});
  }, [lodgingId]);

  useEffect(() => {
    // Anonymous visitors never call eligibility; the review form is hidden
    // entirely by the `{user && (...)}` guard below regardless of this
    // status, so there is nothing to fetch.
    if (!user) return;
    loadEligibility();
  }, [user, loadEligibility]);

  async function submitRating() {
    if (userScore === 0) return;
    setSubmitError(null);
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
      setSubmitError(err.message);
    }
  }

  return (
    <section className="ratings-section">
      <h2>Reseñas</h2>
      <div className="ratings-header">
        <span className="avg-score">{avgScore.toFixed(1)}</span>
        <span className="stars-display">
          {STAR_VALUES.map((s) => (
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

          {eligibilityStatus === "loading" && (
            <p className="eligibility-status" role="status">
              Comprobando si podés dejar una reseña...
            </p>
          )}

          {eligibilityStatus === "error" && (
            <div className="eligibility-alert" role="alert">
              <p>No pudimos comprobar si podés dejar una reseña.</p>
              <button type="button" onClick={loadEligibility}>
                Reintentar
              </button>
            </div>
          )}

          {eligibilityStatus === "ineligible" && (
            <p className="eligibility-status">
              Todavía no tenés una estadía confirmada y finalizada en este
              alojamiento, así que no podés dejar una reseña.
            </p>
          )}

          {eligibilityStatus === "eligible" && (
            <>
              <div className="star-selector" role="group" aria-label="Puntaje">
                {STAR_VALUES.map((s) => (
                  <button
                    type="button"
                    key={s}
                    className={s <= (hoverScore || userScore) ? "star-filled" : "star-empty"}
                    aria-pressed={s === userScore}
                    aria-label={starLabel(s)}
                    onClick={() => setUserScore(s)}
                    onMouseEnter={() => setHoverScore(s)}
                    onMouseLeave={() => setHoverScore(0)}
                  >
                    ★
                  </button>
                ))}
              </div>
              <textarea
                value={userComment}
                onChange={(e) => setUserComment(e.target.value)}
                placeholder="Contá tu experiencia..."
                rows={3}
              />
              {submitError && (
                <p className="submit-error" role="alert">
                  {submitError}
                </p>
              )}
              <button
                className="btn-submit-review"
                onClick={submitRating}
                disabled={userScore === 0}
              >
                Enviar reseña
              </button>
            </>
          )}
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
              {STAR_VALUES.map((s) => (
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
