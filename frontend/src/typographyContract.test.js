import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const read = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

describe("semantic typography contract", () => {
  const appCss = read("src/App.css");

  it("defines the approved semantic scale and explicit body baseline", () => {
    expect(rule(appCss, ":root")).toMatch(/--type-h1-size:\s*32px/);
    expect(rule(appCss, ":root")).toMatch(/--type-h1-weight:\s*700/);
    expect(rule(appCss, ":root")).toMatch(/--type-h1-line-height:\s*1\.2/);
    expect(rule(appCss, ":root")).toMatch(/--type-h2-size:\s*24px/);
    expect(rule(appCss, ":root")).toMatch(/--type-h2-weight:\s*600/);
    expect(rule(appCss, ":root")).toMatch(/--type-h2-line-height:\s*1\.25/);
    expect(rule(appCss, ":root")).toMatch(/--type-body-size:\s*16px/);
    expect(rule(appCss, ":root")).toMatch(/--type-body-weight:\s*400/);
    expect(rule(appCss, ":root")).toMatch(/--type-body-line-height:\s*1\.5/);
    expect(rule(appCss, ":root")).toMatch(/--type-label-size:\s*14px/);
    expect(rule(appCss, ":root")).toMatch(/--type-label-weight:\s*500/);
    expect(rule(appCss, ":root")).toMatch(/--type-label-line-height:\s*1\.4/);
    expect(rule(appCss, ":root")).toMatch(/--type-caption-size:\s*12px/);
    expect(rule(appCss, ":root")).toMatch(/--type-caption-weight:\s*400/);
    expect(rule(appCss, ":root")).toMatch(/--type-caption-line-height:\s*1\.4/);

    const body = rule(appCss, "body");
    expect(body).toMatch(/font-size:\s*var\(--type-body-size\)/);
    expect(body).toMatch(/font-weight:\s*var\(--type-body-weight\)/);
    expect(body).toMatch(/line-height:\s*var\(--type-body-line-height\)/);
  });

  it.each([
    ["src/pages/ProductDetail/ProductDetail.css", ".detail-title-group h1"],
    ["src/pages/Booking/BookingConfirmation.css", ".confirmation-header h1"],
    ["src/pages/MyReservations/MyReservationsPage.css", ".reservations-header h1"],
    ["src/pages/Favorites/FavoritesPage.css", ".favorites-title"],
  ])("applies the H1 tokens to %s", (path, selector) => {
    const title = rule(read(path), selector);
    expect(title).toMatch(/font-size:\s*var\(--type-h1-size\)/);
    expect(title).toMatch(/font-weight:\s*var\(--type-h1-weight\)/);
    expect(title).toMatch(/line-height:\s*var\(--type-h1-line-height\)/);
  });

  it("keeps auth titles on the H2 tokens at every breakpoint", () => {
    const authCss = read("src/assets/css/auth.css");
    const title = rule(authCss, ".login-box h2,\n.register-box h2");

    expect(title).toMatch(/font-size:\s*var\(--type-h2-size\)/);
    expect(title).toMatch(/font-weight:\s*var\(--type-h2-weight\)/);
    expect(title).toMatch(/line-height:\s*var\(--type-h2-line-height\)/);
    expect(authCss.match(/\.login-box h2,\s*\.register-box h2\s*\{/g)).toHaveLength(1);
  });

  it("uses an H1 for the Favorites page title", () => {
    const source = read("src/pages/Favorites/FavoritesPage.jsx");
    expect(source).toContain('<h1 className="favorites-title">Mis favoritos</h1>');
    expect(source).not.toContain('<h2 className="favorites-title">');
  });
});
