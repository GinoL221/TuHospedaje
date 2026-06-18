import "@testing-library/jest-dom/vitest";

// jsdom gaps — add ONLY if a mounted component needs them (e.g. matchMedia,
// IntersectionObserver). Start empty; add lazily when a test fails on a
// missing API.
// window.matchMedia ??= () => ({ matches: false, addEventListener() {}, removeEventListener() {} })
