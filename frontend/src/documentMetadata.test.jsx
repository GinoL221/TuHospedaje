import { readFileSync } from "node:fs";
import { resolve } from "node:path";

describe("document metadata", () => {
  const html = readFileSync(resolve(process.cwd(), "index.html"), "utf8");
  const packageJson = JSON.parse(readFileSync(resolve(process.cwd(), "package.json"), "utf8"));

  it("declares Spanish and the fixed product title", () => {
    expect(html).toMatch(/<html lang="es">/);
    expect(html).toMatch(/<title>TuHospedaje<\/title>/);
  });

  it("does not add route-title machinery or metadata dependencies", () => {
    expect(html).not.toMatch(/route[- ]title|react-helmet/i);
    expect(packageJson.dependencies).not.toHaveProperty("react-helmet");
    expect(packageJson.dependencies).not.toHaveProperty("react-helmet-async");
  });
});
