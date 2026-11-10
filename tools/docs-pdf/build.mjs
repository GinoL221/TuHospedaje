// Regenerates the deliverable PDFs in docs/entregables from the Markdown sources
// declared in documents.json, then refreshes docs/entregables/SOURCES.sha256.
//
// Each document kind (definition, report, test-plan, logbook) has its own accent,
// tint, cover layout and header. The manifest hashes the sources and every build
// input, so a test can detect a PDF that is out of date.
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

import { prepareContent } from "./transform.mjs";

const toolDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(toolDir, "..", "..");
const deliverables = join(root, "docs", "entregables");
const manifestPath = join(deliverables, "SOURCES.sha256");

// Files, besides the Markdown sources, whose content changes the PDFs.
const buildInputs = [
  "tools/docs-pdf/brand.css",
  "tools/docs-pdf/build.mjs",
  "tools/docs-pdf/transform.mjs",
  "tools/docs-pdf/documents.json",
  "tools/docs-pdf/package-lock.json",
  "frontend/src/assets/images/TuHospedaje_Imagotipo.png",
];

function loadConfig() {
  const file = join(toolDir, "documents.json");
  try {
    return JSON.parse(readFileSync(file, "utf8"));
  } catch (error) {
    console.error(`Cannot read ${file}: ${error.message}`);
    process.exit(1);
  }
}

const config = loadConfig();

const sha256 = (buffer) => createHash("sha256").update(buffer).digest("hex");
const dataUri = (file, mime) =>
  `data:${mime};base64,${readFileSync(file).toString("base64")}`;
const escapeHtml = (text) =>
  String(text).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

function findBrowser() {
  if (process.env.PUPPETEER_EXECUTABLE_PATH) return process.env.PUPPETEER_EXECUTABLE_PATH;
  const cache = join(homedir(), ".cache", "ms-playwright");
  if (!existsSync(cache)) return undefined;
  const playwrightChrome = globSync("chromium-*/chrome-linux*/chrome", { cwd: cache })
    .sort()
    .at(-1);
  return playwrightChrome ? join(cache, playwrightChrome) : undefined;
}

const fontFile = join(
  toolDir,
  "node_modules/@fontsource-variable/inter/files/inter-latin-wght-normal.woff2",
);
const fontFace = `@font-face { font-family: "Inter Variable"; font-weight: 100 900; font-style: normal; src: url(${dataUri(fontFile, "font/woff2")}) format("woff2"); }`;
const brandCss = readFileSync(join(toolDir, "brand.css"), "utf8");
const logo = dataUri(join(root, buildInputs[5]), "image/png");

function coverHtml(data, kind, document) {
  const facts = document.facts
    .map(
      (fact) =>
        `<div class="fact"><span class="fact-label">${escapeHtml(fact.label)}</span><span class="fact-value">${escapeHtml(fact.value)}</span></div>`,
    )
    .join("");
  return [
    `<section class="cover cover-${kind.layout} cover-${document.kind}">`,
    `<div class="cover-top"><img src="${logo}" alt="TuHospedaje"><span class="cover-badge">${escapeHtml(kind.label)}</span></div>`,
    '<div class="cover-main">',
    `<h1 class="cover-title">${escapeHtml(data.title)}</h1>`,
    `<p class="cover-subtitle">${escapeHtml(data.subtitle ?? "")}</p>`,
    `<div class="cover-facts">${facts}</div>`,
    "</div>",
    `<p class="cover-meta">${escapeHtml(data.author ?? "")} · ${escapeHtml(data.date ?? "")}</p>`,
    "</section>",
    "",
  ].join("\n");
}

function headerTemplate(kind, document) {
  return `<div style="font-size:7.5pt;font-family:Arial,sans-serif;width:100%;margin:0 20mm;padding-bottom:4px;border-bottom:1px solid #DCE1E5;color:#5B6770;display:flex;justify-content:space-between;-webkit-print-color-adjust:exact"><span><b style="color:#264653">TuHospedaje</b></span><span><span style="display:inline-block;width:7px;height:7px;margin-right:6px;background:${kind.accent};border-radius:2px"></span>${escapeHtml(document.header)}</span></div>`;
}

function footerTemplate(data) {
  return `<div style="font-size:7.5pt;font-family:Arial,sans-serif;width:100%;margin:0 20mm;color:#5B6770;display:flex;justify-content:space-between"><span>${escapeHtml(data.date ?? "")}</span><span>Página <span class="pageNumber"></span> de <span class="totalPages"></span></span></div>`;
}

const executablePath = findBrowser();
const hashes = [];

for (const [source, document] of Object.entries(config.documents)) {
  const kind = config.kinds[document.kind];
  const sourceFile = join(root, source);
  const buffer = readFileSync(sourceFile);
  const { data, content } = matter(buffer.toString("utf8"));
  const destination = join(deliverables, `${basename(source, ".md")}.pdf`);
  const css = `${fontFace}\n:root { --kind: ${kind.accent}; --tint: ${kind.tint}; }\n${brandCss}`;
  const result = await mdToPdf(
    { content: `${coverHtml(data, kind, document)}${prepareContent(content)}` },
    {
      css,
      basedir: dirname(sourceFile),
      dest: destination,
      pdf_options: {
        format: "a4",
        printBackground: true,
        displayHeaderFooter: true,
        margin: { top: "25mm", bottom: "25mm", left: "20mm", right: "20mm" },
        headerTemplate: headerTemplate(kind, document),
        footerTemplate: footerTemplate(data),
      },
      launch_options: { executablePath, args: ["--no-sandbox"] },
    },
  );
  if (!result?.content?.length || !existsSync(destination)) {
    throw new Error(`PDF generation failed for ${source}`);
  }
  hashes.push([source, buffer]);
  console.log(`ok  ${source} -> docs/entregables/${basename(destination)}`);
}

for (const input of buildInputs) hashes.push([input, readFileSync(join(root, input))]);

writeFileSync(
  manifestPath,
  `${hashes.map(([path, buffer]) => `${sha256(buffer)}  ${path}`).join("\n")}\n`,
);
console.log(`manifest updated: ${manifestPath}`);
