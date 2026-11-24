'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const {
  collectRecoveredTests,
  classifyAndFormat,
  main,
} = require('./summarize-e2e-retries.cjs');

function withTempDir(fn) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'e2e-retry-summary-'));
  try {
    return fn(dir);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

test('collectRecoveredTests walks 3-level nested suites and detects flaky and retried passes', () => {
  const report = {
    suites: [
      {
        title: 'tests/booking.spec.js',
        suites: [
          {
            title: 'Booking flow',
            suites: [
              {
                title: 'Guest checkout',
                specs: [
                  {
                    title: 'completes booking after transient network hiccup',
                    tests: [
                      {
                        status: 'flaky',
                        results: [
                          { retry: 0, status: 'failed' },
                          { retry: 1, status: 'passed' },
                        ],
                      },
                    ],
                  },
                  {
                    title: 'recovers via retry result even if top-level status is expected',
                    tests: [
                      {
                        status: 'expected',
                        results: [
                          { retry: 0, status: 'timedOut' },
                          { retry: 1, status: 'passed' },
                        ],
                      },
                    ],
                  },
                  {
                    title: 'passes on first attempt',
                    tests: [
                      {
                        status: 'expected',
                        results: [{ retry: 0, status: 'passed' }],
                      },
                    ],
                  },
                  {
                    title: 'fails on all attempts',
                    tests: [
                      {
                        status: 'unexpected',
                        results: [
                          { retry: 0, status: 'failed' },
                          { retry: 1, status: 'failed' },
                        ],
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  };

  assert.deepEqual(collectRecoveredTests(report), [
    'tests/booking.spec.js > Booking flow > Guest checkout > completes booking after transient network hiccup',
    'tests/booking.spec.js > Booking flow > Guest checkout > recovers via retry result even if top-level status is expected',
  ]);
});

test('classifyAndFormat formats all four E2E outcomes accurately', () => {
  const firstAttempt = classifyAndFormat({
    browser: 'chromium',
    outcome: 'success',
    recovered: [],
    missingResults: false,
    parseError: '',
  });
  assert.equal(firstAttempt, '### E2E (chromium): E2E_PASS_FIRST_ATTEMPT\n');

  const recoveredPass = classifyAndFormat({
    browser: 'firefox',
    outcome: 'success',
    recovered: ['smoke.spec.js > loads homepage', 'auth.spec.js > logs in'],
    missingResults: false,
    parseError: '',
  });
  assert.equal(
    recoveredPass,
    [
      '### E2E (firefox): RECOVERED_PASS (2)',
      '- smoke.spec.js > loads homepage',
      '- auth.spec.js > logs in',
      '',
    ].join('\n'),
  );

  const unavailableMissing = classifyAndFormat({
    browser: 'mobile-webkit',
    outcome: 'success',
    recovered: [],
    missingResults: true,
    parseError: '',
  });
  assert.equal(
    unavailableMissing,
    [
      '### E2E (mobile-webkit): E2E_PASS_RESULTS_UNAVAILABLE',
      '- `results.json` was not generated.',
      '',
    ].join('\n'),
  );

  const unavailableCorrupt = classifyAndFormat({
    browser: 'mobile-chromium',
    outcome: 'success',
    recovered: [],
    missingResults: false,
    parseError: 'Unexpected end of JSON input',
  });
  assert.equal(
    unavailableCorrupt,
    [
      '### E2E (mobile-chromium): E2E_PASS_RESULTS_UNAVAILABLE',
      '- results.json parse error: Unexpected end of JSON input',
      '',
    ].join('\n'),
  );

  const finalFailureWithRecovered = classifyAndFormat({
    browser: 'chromium',
    outcome: 'failure',
    recovered: ['reviews.spec.js > submits review'],
    missingResults: false,
    parseError: '',
  });
  assert.equal(
    finalFailureWithRecovered,
    [
      '### E2E (chromium): FINAL_E2E_FAILURE',
      '- Recovered on retry before failure (1):',
      '  - reviews.spec.js > submits review',
      '',
    ].join('\n'),
  );

  const finalFailureMissing = classifyAndFormat({
    browser: 'firefox',
    outcome: 'failure',
    recovered: [],
    missingResults: true,
    parseError: '',
  });
  assert.equal(
    finalFailureMissing,
    [
      '### E2E (firefox): FINAL_E2E_FAILURE',
      '- `results.json` was not generated.',
      '',
    ].join('\n'),
  );

  const finalFailureCorrupt = classifyAndFormat({
    browser: 'firefox',
    outcome: 'failure',
    recovered: [],
    missingResults: false,
    parseError: 'Unexpected token',
  });
  assert.equal(
    finalFailureCorrupt,
    [
      '### E2E (firefox): FINAL_E2E_FAILURE',
      '- results.json parse error: Unexpected token',
      '',
    ].join('\n'),
  );
});

test('main handles missing results.json on success, exits 0, and appends to GITHUB_STEP_SUMMARY', () => {
  withTempDir((dir) => {
    const missingResultsPath = path.join(dir, 'missing-results.json');
    const summaryPath = path.join(dir, 'step-summary.md');
    fs.writeFileSync(summaryPath, '## Existing summary\n', 'utf8');

    let capturedStdout = '';
    const code = main({
      resultsPath: missingResultsPath,
      env: {
        E2E_BROWSER: 'mobile-webkit',
        E2E_OUTCOME: 'success',
        GITHUB_STEP_SUMMARY: summaryPath,
      },
      stdout: {
        write: (chunk) => {
          capturedStdout += chunk;
        },
      },
    });

    assert.equal(code, 0);
    assert.match(capturedStdout, /E2E_PASS_RESULTS_UNAVAILABLE/);
    assert.match(capturedStdout, /`results\.json` was not generated\./);

    const summaryContent = fs.readFileSync(summaryPath, 'utf8');
    assert.ok(summaryContent.startsWith('## Existing summary\n'));
    assert.ok(summaryContent.endsWith(capturedStdout));
  });
});

test('main handles corrupt/truncated results.json gracefully and exits 0', () => {
  withTempDir((dir) => {
    const corruptPath = path.join(dir, 'results.json');
    fs.writeFileSync(corruptPath, '{"suites": [', 'utf8');

    let capturedSuccess = '';
    const successCode = main({
      resultsPath: corruptPath,
      env: {
        E2E_BROWSER: 'chromium',
        E2E_OUTCOME: 'success',
      },
      stdout: {
        write: (chunk) => {
          capturedSuccess += chunk;
        },
      },
    });

    assert.equal(successCode, 0);
    assert.match(capturedSuccess, /### E2E \(chromium\): E2E_PASS_RESULTS_UNAVAILABLE/);
    assert.match(capturedSuccess, /results\.json parse error:/);

    let capturedFailure = '';
    const failureCode = main({
      resultsPath: corruptPath,
      env: {
        E2E_BROWSER: 'chromium',
        E2E_OUTCOME: 'failure',
      },
      stdout: {
        write: (chunk) => {
          capturedFailure += chunk;
        },
      },
    });

    assert.equal(failureCode, 0);
    assert.match(capturedFailure, /### E2E \(chromium\): FINAL_E2E_FAILURE/);
    assert.match(capturedFailure, /results\.json parse error:/);
  });
});

test('main parses valid results.json and appends RECOVERED_PASS and E2E_PASS_FIRST_ATTEMPT to GITHUB_STEP_SUMMARY', () => {
  withTempDir((dir) => {
    const resultsPath = path.join(dir, 'results.json');
    const summaryPath = path.join(dir, 'summary.md');

    fs.writeFileSync(
      resultsPath,
      JSON.stringify({
        suites: [
          {
            title: 'tests/smoke.spec.js',
            specs: [
              {
                title: 'renders landing page',
                tests: [
                  {
                    status: 'flaky',
                    results: [
                      { retry: 0, status: 'failed' },
                      { retry: 1, status: 'passed' },
                    ],
                  },
                ],
              },
            ],
          },
        ],
      }),
      'utf8',
    );

    let outRecovered = '';
    const code1 = main({
      resultsPath,
      env: {
        E2E_BROWSER: 'firefox',
        E2E_OUTCOME: 'success',
        GITHUB_STEP_SUMMARY: summaryPath,
      },
      stdout: { write: (chunk) => { outRecovered += chunk; } },
    });
    assert.equal(code1, 0);
    assert.equal(
      outRecovered,
      '### E2E (firefox): RECOVERED_PASS (1)\n- tests/smoke.spec.js > renders landing page\n',
    );

    fs.writeFileSync(
      resultsPath,
      JSON.stringify({
        suites: [
          {
            title: 'tests/smoke.spec.js',
            specs: [
              {
                title: 'renders landing page',
                tests: [{ status: 'expected', results: [{ retry: 0, status: 'passed' }] }],
              },
            ],
          },
        ],
      }),
      'utf8',
    );

    let outFirst = '';
    const code2 = main({
      resultsPath,
      env: {
        E2E_BROWSER: 'chromium',
        E2E_OUTCOME: 'success',
        GITHUB_STEP_SUMMARY: summaryPath,
      },
      stdout: { write: (chunk) => { outFirst += chunk; } },
    });
    assert.equal(code2, 0);
    assert.equal(outFirst, '### E2E (chromium): E2E_PASS_FIRST_ATTEMPT\n');

    const summary = fs.readFileSync(summaryPath, 'utf8');
    assert.equal(summary, outRecovered + outFirst);
  });
});
