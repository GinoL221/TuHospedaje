import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const read = (path) => readFileSync(resolve(process.cwd(), path), "utf8");

function rule(css, selector) {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))?.[1];
}

describe("public shell contract", () => {
  const appCss = read("src/App.css");
  const layoutCss = read("src/layout.css");
  const whatsappCss = read("src/components/WhatsAppButton/WhatsAppButton.css");
  const app = read("src/App.jsx");
  const entry = read("src/main.jsx");
  const footer = read("src/components/Footer/Footer.jsx");

  it("owns semantic visual and layer tokens in App.css", () => {
    const root = rule(appCss, ":root");

    expect(root).toMatch(/--surface-page:\s*var\(--bg\)/);
    expect(root).toMatch(/--space-4:\s*16px/);
    expect(root).toMatch(/--layer-header:\s*100/);
    expect(root).toMatch(/--layer-floating:\s*200/);
  });

  it("defines the global scroll and responsive clearance contract in layout.css", () => {
    expect(rule(layoutCss, ":root")).toMatch(/--site-scroll-clearance:\s*calc\(var\(--site-header-height\) \+ var\(--site-scroll-gap\)\)/);
    expect(rule(layoutCss, "html")).toMatch(/scroll-padding-top:\s*var\(--site-scroll-clearance\)/);
    expect(rule(layoutCss, "main")).toMatch(/padding-top:\s*var\(--site-scroll-clearance\)/);
    expect(layoutCss).toMatch(/z-index:\s*var\(--layer-header\)/);
    expect(whatsappCss).toMatch(/z-index:\s*var\(--layer-floating\)/);
  });

  it("loads shell styles in order and keeps admin on the separate guarded route", () => {
    expect(entry.indexOf('import "./App.css"')).toBeLessThan(entry.indexOf('import "./layout.css"'));
    expect(app).toMatch(/<div className="public-shell">\s*<Header \/>/);
    expect(app).toContain('const ADMIN_PATH_PREFIXES = ["/administración", "/admin"]');
    expect(app).toMatch(/<Route element={<RequireAdmin \/>}>[\s\S]*path="\/administración"/);
  });

  it("keeps the public footer content while exposing its social links as navigation", () => {
    expect(footer).toContain('<footer className="site-footer">');
    expect(footer).toContain('<nav className="footer-right" aria-label="Redes sociales">');
    expect(footer).toContain("<WhatsAppButton />");
  });
});
