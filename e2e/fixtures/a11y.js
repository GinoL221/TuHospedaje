// @ts-check
const { expect } = require('@playwright/test');
const AxeBuilder = require('@axe-core/playwright').default;

const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/**
 * Format axe-core violations into a clear, actionable report.
 *
 * @param {string} label
 * @param {import('axe-core').Result[]} violations
 * @returns {string}
 */
function formatViolations(label, violations) {
  if (!violations.length) {
    return `[a11y] ${label}: no violations`;
  }

  const lines = [
    `[a11y] ${label}: ${violations.length} WCAG violation rule(s) found`,
  ];

  for (const violation of violations) {
    lines.push(
      '',
      `- Rule: ${violation.id} (impact: ${violation.impact || 'unknown'}, nodes: ${violation.nodes.length})`,
      `  Help: ${violation.help}`,
      `  Help URL: ${violation.helpUrl}`,
    );

    for (const [index, node] of violation.nodes.entries()) {
      const target = Array.isArray(node.target) ? node.target.join(', ') : String(node.target);
      const summary = (node.failureSummary || '').replace(/\s+/g, ' ').trim();
      const snippet = (node.html || '').replace(/\s+/g, ' ').trim();
      lines.push(
        `  [${index + 1}] Target: ${target}`,
        `      Summary: ${summary}`,
        `      HTML: ${snippet}`,
      );
    }
  }

  return lines.join('\n');
}

/**
 * Normalize a selector or array of selectors into an array.
 *
 * @param {string | string[] | undefined} value
 * @returns {string[]}
 */
function toSelectorList(value) {
  if (!value) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

/**
 * Wait for finite animations to settle and assert zero WCAG 2.0/2.1 A/AA violations.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} label
 * @param {{ include?: string | string[], exclude?: string | string[] }} [options]
 */
async function expectNoA11yViolations(page, label, options = {}) {
  await page.evaluate(() =>
    Promise.all(
      document
        .getAnimations()
        .filter((a) => a.effect?.getComputedTiming?.().iterations !== Infinity)
        .map((a) => a.finished.catch(() => {})),
    ),
  );

  let builder = new AxeBuilder({ page }).withTags(WCAG_TAGS);

  for (const selector of toSelectorList(options.include)) {
    builder = builder.include(selector);
  }
  for (const selector of toSelectorList(options.exclude)) {
    builder = builder.exclude(selector);
  }

  const results = await builder.analyze();
  const formatted = formatViolations(label, results.violations);

  expect(results.violations, formatted).toEqual([]);
}

module.exports = {
  WCAG_TAGS,
  expectNoA11yViolations,
  formatViolations,
};
