// Regenerates the deliverable PDFs in docs/entregables from the Markdown sources
// listed in docs/entregables/SOURCES.sha256, then refreshes that manifest.
//
// Usage (from tools/docs-pdf): npm ci && npm run build
// Optional: PUPPETEER_EXECUTABLE_PATH=/path/to/chrome to choose the browser.

import { createHash } from "node:crypto";
import { existsSync, globSync, readFileSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import matter from "gray-matter";
import { mdToPdf } from "md-to-pdf";

const toolDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(toolDir, "..", "..");
const deliverables = join(root, "docs", "entregables");
const manifestPath = join(deliverables, "SOURCES.sha256");

const sha256 = (buffer) => createHash("sha256").update(buffer).digest("hex");
const dataUri = (file, mime) =>
  `data:${mime};base64,${readFileSync(file).toString("base64")}`;
const escapeHtml = (text) =>
  String(text).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

function findBrowser() {
  if (process.env.PUPPETEER_EXECUTABLE_PATH) return process.env.PUPPETEER_EXECUTABLE_PATH;
  const cache = join(homedir(), ".cache", "ms-playwright");
  const [playwrightChrome] = globSync("chromium-*/chrome-linux*/chrome", { cwd: cache })
    .sort()
    .reverse();
  return playwrightChrome ? join(cache, playwrightChrome) : undefined;
}

function readSources() {
  return readFileSync(manifestPath, "utf8")
    .split("\n")
    .map((line) => line.match(/^[0-9a-f]{64} {2}(\S+)$/)?.[1])
    .filter(Boolean);
}

const fontFile = join(
  toolDir,
  "node_modules/@fontsource-variable/inter/files/inter-latin-wght-normal.woff2",
);
const logoFile = join(root, "frontend/src/assets/images/TuHospedaje_Imagotipo.png");
const fontFace = `@font-face { font-family: "Inter Variable"; font-weight: 100 900; font-style: normal; src: url(${dataUri(fontFile, "font/woff2")}) format("woff2"); }`;
const css = `${fontFace}\n${readFileSync(join(toolDir, "brand.css"), "utf8")}`;
const logo = dataUri(logoFile, "image/png");

function withCover(source) {
  const { data } = matter(source);
  const cover = [
    '<section class="cover">',
    `<img src="${logo}" alt="TuHospedaje">`,
    '<div class="cover-rule"></div>',
    `<h1 class="cover-title">${escapeHtml(data.title)}</h1>`,
    `<p class="cover-subtitle">${escapeHtml(data.subtitle ?? "")}</p>`,
    `<p class="cover-meta">${escapeHtml(data.author ?? "")} · ${escapeHtml(data.date ?? "")}</p>`,
    "</section>",
    "",
  ].join("\n");
  const frontMatterEnd = source.indexOf("\n---", 3) + 4;
  return `${source.slice(0, frontMatterEnd)}\n\n${cover}${source.slice(frontMatterEnd)}`;
}

const executablePath = findBrowser();
const hashes = [];

for (const relative of readSources()) {
  const sourceFile = join(root, relative);
  const buffer = readFileSync(sourceFile);
  const destination = join(deliverables, `${basename(relative, ".md")}.pdf`);
  const result = await mdToPdf(
    { content: withCover(buffer.toString("utf8")) },
    {
      css,
      basedir: dirname(sourceFile),
      dest: destination,
      launch_options: { executablePath, args: ["--no-sandbox"] },
    },
  );
  if (!result?.filename || !existsSync(destination)) {
    throw new Error(`PDF generation failed for ${relative}`);
  }
  hashes.push(`${sha256(buffer)}  ${relative}`);
  console.log(`ok  ${relative} -> docs/entregables/${basename(destination)}`);
}

writeFileSync(manifestPath, `${hashes.join("\n")}\n`);
console.log(`manifest updated: ${manifestPath}`);
