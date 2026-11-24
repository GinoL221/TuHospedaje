#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const DEFAULT_RESULTS_PATH = path.join('e2e', 'test-results', 'results.json');

function walkSuite(suite, parents, recovered) {
  if (!suite || typeof suite !== 'object') {
    return;
  }
  const next = suite.title ? [...parents, suite.title] : parents;
  for (const spec of suite.specs || []) {
    const title = [...next, spec.title].filter(Boolean).join(' > ');
    for (const testEntry of spec.tests || []) {
      const results = Array.isArray(testEntry.results) ? testEntry.results : [];
      const isRecovered =
        testEntry.status === 'flaky' ||
        results.some((r) => r && r.retry > 0 && r.status === 'passed');
      if (isRecovered) {
        recovered.push(title);
      }
    }
  }
  for (const child of suite.suites || []) {
    walkSuite(child, next, recovered);
  }
}

function collectRecoveredTests(data) {
  const recovered = [];
  if (!data || typeof data !== 'object') {
    return recovered;
  }
  for (const suite of data.suites || []) {
    walkSuite(suite, [], recovered);
  }
  return recovered;
}

function classifyAndFormat({
  browser = 'unknown',
  outcome = 'unknown',
  recovered = [],
  missingResults = false,
  parseError = '',
} = {}) {
  const lines = [];
  if (outcome === 'failure') {
    lines.push(`### E2E (${browser}): FINAL_E2E_FAILURE`);
    if (missingResults) {
      lines.push('- `results.json` was not generated.');
    } else if (parseError) {
      lines.push(`- results.json parse error: ${parseError}`);
    }
    if (recovered.length > 0) {
      lines.push(`- Recovered on retry before failure (${recovered.length}):`);
      for (const title of recovered) {
        lines.push(`  - ${title}`);
      }
    }
  } else if (missingResults || parseError) {
    lines.push(`### E2E (${browser}): E2E_PASS_RESULTS_UNAVAILABLE`);
    if (missingResults) {
      lines.push('- `results.json` was not generated.');
    } else {
      lines.push(`- results.json parse error: ${parseError}`);
    }
  } else if (recovered.length > 0) {
    lines.push(`### E2E (${browser}): RECOVERED_PASS (${recovered.length})`);
    for (const title of recovered) {
      lines.push(`- ${title}`);
    }
  } else {
    lines.push(`### E2E (${browser}): E2E_PASS_FIRST_ATTEMPT`);
  }

  return `${lines.join('\n')}\n`;
}

function main(options = {}) {
  const env = options.env || process.env;
  const fsModule = options.fsModule || fs;
  const stdout = options.stdout || process.stdout;
  const resultsPath = options.resultsPath || DEFAULT_RESULTS_PATH;

  const browser = env.E2E_BROWSER || 'unknown';
  const outcome = env.E2E_OUTCOME || 'unknown';

  let recovered = [];
  let missingResults = false;
  let parseError = '';

  if (fsModule.existsSync(resultsPath)) {
    try {
      const data = JSON.parse(fsModule.readFileSync(resultsPath, 'utf8'));
      recovered = collectRecoveredTests(data);
    } catch (err) {
      parseError = err instanceof Error ? err.message : String(err);
    }
  } else {
    missingResults = true;
  }

  const output = classifyAndFormat({
    browser,
    outcome,
    recovered,
    missingResults,
    parseError,
  });

  stdout.write(output);
  if (env.GITHUB_STEP_SUMMARY) {
    fsModule.appendFileSync(env.GITHUB_STEP_SUMMARY, output, 'utf8');
  }

  return 0;
}

module.exports = {
  collectRecoveredTests,
  classifyAndFormat,
  main,
};

if (require.main === module) {
  process.exitCode = main();
}
