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
  const [ratingsStatus, setRatingsStatus] = useState("loading");
  const [ratingsRequest, setRatingsRequest] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { status: eligibilityStatus, load: loadEligibility } =
    useRatingEligibility(lodgingId);
  const sectionId = `reviews-${lodgingId}`;
  const retryRatings = () => { setRatingsStatus("loading"); setRatingsRequest((request) => request + 1); };

  useEffect(() => {
    let active = true;
    get(`/ratings/lodging/${lodgingId}`)
      .then((data) => {
        if (!active) return;
        setAvgScore(data.average || 0);
        setTotalReviews(data.count || 0);
        setRatings(Array.isArray(data.ratings) ? data.ratings : []);
        setRatingsStatus("ready");
      })
      .catch(() => active && setRatingsStatus("error"));
    return () => { active = false; };
  }, [lodgingId, ratingsRequest]);

  useEffect(() => {
    // Anonymous visitors never call eligibility; the review form is hidden
    // entirely by the `{user && (...)}` guard below regardless of this
    // status, so there is nothing to fetch.
    if (!user) return;
    loadEligibility();
  }, [user, loadEligibility]);

  async function submitRating(event) {
    event.preventDefault();
    if (isSubmitting || userScore === 0) return;
    setIsSubmitting(true);
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
      setRatingsStatus("ready");
      setUserScore(0);
      setUserComment("");
    } catch (err) {
      setSubmitError(err?.message || "No pudimos enviar tu reseña.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="ratings-section" aria-labelledby={`${sectionId}-title`} aria-busy={ratingsStatus === "loading"}>
      <h2 id={`${sectionId}-title`}>Reseñas</h2>
      <div className="ratings-header" role="img" aria-label={`Puntaje promedio ${avgScore.toFixed(1)} de 5 estrellas; ${totalReviews} reseñas`}>
        <span className="avg-score" aria-hidden="true">{avgScore.toFixed(1)}</span>
        <span className="stars-display" aria-hidden="true">
          {STAR_VALUES.map((s) => (
            <span key={s} className={s <= Math.round(avgScore) ? "star-filled" : "star-empty"}>
              ★
            </span>
          ))}
        </span>
        <span className="reviews-count" aria-hidden="true">({totalReviews} reseñas)</span>
      </div>

      {user && (
        <form className="review-form" onSubmit={submitRating} aria-labelledby={`${sectionId}-form-title`}>
          <h3 id={`${sectionId}-form-title`}>Dejá tu reseña</h3>

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
            <p className="eligibility-status" role="status">
              Todavía no tenés una estadía confirmada y finalizada en este
              alojamiento, así que no podés dejar una reseña.
            </p>
          )}

          {eligibilityStatus === "eligible" && (
            <>
              <fieldset className="star-selector" disabled={isSubmitting}>
                <legend>Puntaje</legend>
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
              </fieldset>
              <label htmlFor={`${sectionId}-comment`}>Comentario</label>
              <textarea
                id={`${sectionId}-comment`}
                value={userComment}
                onChange={(e) => setUserComment(e.target.value)}
                placeholder="Contá tu experiencia..."
                rows={3}
                disabled={isSubmitting}
                aria-invalid={submitError ? "true" : undefined}
                aria-describedby={submitError ? `${sectionId}-submit-error` : undefined}
              />
              {submitError && (
                <p id={`${sectionId}-submit-error`} className="submit-error" role="alert" aria-live="assertive">
                  {submitError}
                </p>
              )}
              <button
                type="submit"
                className="btn-submit-review"
                disabled={userScore === 0 || isSubmitting}
                aria-busy={isSubmitting}
              >
                {isSubmitting ? "Enviando..." : "Enviar reseña"}
              </button>
            </>
          )}
        </form>
      )}

      <h3 className="reviews-list-title">Opiniones de huéspedes</h3>
      {ratingsStatus === "loading" && <p className="ratings-loading" role="status" aria-live="polite">Cargando reseñas...</p>}
      {ratingsStatus === "error" && (
        <div className="ratings-alert" role="alert" aria-live="assertive">
          <p>No pudimos cargar las reseñas.</p>
          <button type="button" onClick={retryRatings}>Reintentar</button>
        </div>
      )}
      {ratingsStatus === "ready" && ratings.length === 0 && <p className="reviews-empty-state">Todavía no hay reseñas para este alojamiento.</p>}
      {ratingsStatus === "ready" && ratings.length > 0 && (
        <div className="reviews-list" role="list" aria-label="Opiniones de huéspedes">
          {ratings.map((r) => (
            <div key={r.id} className="review-item" role="listitem">
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
      )}
    </section>
  );
}
