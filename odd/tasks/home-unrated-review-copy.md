# Home Unrated Review Copy

## Goal
Show `Sin reseñas` on unrated Home lodging cards instead of `0.0 (0 opiniones)`, while leaving rated cards and Favorites unchanged.

## Scope and authority
- User selected this Home-only copy change. `ProductCard` is shared by Home (recommendations and search results) and Favorites, so Home explicitly opts in to the new label.
- Preserve the isolated demo database, canonical images, running services, and the unrelated canonical Playwright spec fix. No backend/seed/Compose changes, cleanup or push was authorized.
- Technical checks must distinguish unit tests, build/lint, and browser evidence; do not claim broader academic criteria from this copy fix.

## Allowed edit surfaces
- `frontend/src/components/ProductCard/ProductCard.jsx`
- `frontend/src/components/ProductCard/ProductCard.test.jsx`
- `frontend/src/pages/Home/Home.jsx`
- `frontend/src/pages/Home/Home.test.jsx`
- `odd/tasks/home-unrated-review-copy.md` (task evidence only)

## Work units
- [x] HURC-01 — Added focused tests and implemented Home-only opt-in in ProductCard and both Home usages. Clean RED observed: 4 expected behavior failures / 52 passes; two further inconsistent count/average edge cases failed as intended. GREEN: 58/58 focused tests passed. The label requires both review count and average to be nonpositive; rated/default shared cards keep their numeric summary. Relevant Prettier check and diff check passed.
- [x] HURC-02 — Regression checks passed: 66/66 tests across ProductCard, Home and Favorites; lint, build, scoped Prettier and diff check passed. Chromium verified Home and Bariloche search at 1280×900 and 390×844: 8/8 recommended and 5/5 searched cards show `Sin reseñas` with no rating stars or numeric zero. Impeccable detector returned no findings. With explicit user approval, created a review-only worktree `academic-story-review-home-unrated-7667387` at the same base SHA: its only four changed paths were byte-identical to the Home changes in the demo checkout. Native review `review-87cd7a1dbd669118` approved and acknowledged that four-path slice. The unrelated already-reviewed canonical test and this untracked task record were excluded. Demo containers remained healthy and unchanged.

## Evidence
- Existing `ProductCard` always renders a numeric rating summary; stars already appear only when `averageRating > 0`. `ratingCount` is zero for unrated list items and may be absent in component fixtures.
- Commits: canonical e2e test `873552f` (`test(e2e): fix canonical lodging image coverage`); Home behavior/tests `9f1da30` (`fix(home): show unrated lodging review copy`). The Home commit also contains this feature task record as it stood at commit time; this post-commit evidence update is currently uncommitted. The review-only worktree is retained. No push was authorized. The independent Chromium check found description line-clamp geometry extending past the card's bounding rectangle; whether clipped glyphs paint beyond it was not established and this remains outside the requested rating-copy scope.
