// Pure Markdown transformations applied only while rendering the PDFs.
// The Markdown sources under docs/markdown are never modified.

const STATUS_KINDS = { "✔": "ok", "⚠": "warn", "✘": "fail", "▢": "pending" };

const escapeHtml = (text) =>
  text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

const chip = (kind, text) => `<span class="chip chip-${kind}">${escapeHtml(text)}</span>`;

// Each source carries its own <style> block (table and heading rules) that would
// override brand.css, so it is removed before rendering.
export function stripEmbeddedStyles(markdown) {
  return markdown.replace(/<style[\s\S]*?<\/style>\s*/gi, "");
}

// The cover already shows the title, so a leading level-1 heading is dropped.
export function stripLeadingHeading(markdown) {
  const lines = markdown.split("\n");
  const first = lines.findIndex((line) => line.trim() !== "");
  if (first === -1 || !/^# \S/.test(lines[first])) return markdown;
  lines.splice(first, 1);
  return lines.join("\n");
}

// Turns status cells such as "✔ Pasa" or "No comprobado" into colored chips.
// Only table rows are touched, so prose that mentions a status stays as written.
export function statusChips(markdown) {
  const cell = /\|(\s*)(?:(✔|⚠|✘|▢)\s+(?:\*\*)?([^|]{1,60}?)(?:\*\*)?|(No comprobado))(\s*)(?=\|)/g;
  return markdown
    .split("\n")
    .map((line) => {
      if (!line.trimStart().startsWith("|")) return line;
      return line.replace(cell, (_match, lead, symbol, text, plain, trail) =>
        plain
          ? `|${lead}${chip("warn", plain)}${trail}`
          : `|${lead}${chip(STATUS_KINDS[symbol], `${symbol} ${text.trim()}`)}${trail}`,
      );
    })
    .join("\n");
}

export function prepareContent(markdown) {
  return statusChips(stripLeadingHeading(stripEmbeddedStyles(markdown)));
}
