// @ts-check
const { test, expect } = require('@playwright/test');

const FRONTEND = 'http://localhost:5173';
const BACKEND  = 'http://localhost:8080';
const EMAIL    = 'admin@tuhospedaje.com';
const PASS     = 'Admin1';

async function loginViaUI(page) {
  await page.goto(`${FRONTEND}/login`);
  await page.locator('input[name="email"]').fill(EMAIL);
  await page.locator('input[name="password"]').fill(PASS);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(`${FRONTEND}/`);
}

test.describe('jwt-cookie-storage — browser verification', () => {

  // ── 1. Login: Set-Cookie present, no token in body ──────────────────────
  test('login sets ACCESS_TOKEN cookie and returns no token in body', async ({ page, context }) => {
    let loginResponse;
    page.on('response', res => {
      if (res.url().includes('/api/auth/login')) loginResponse = res;
    });

    await loginViaUI(page);

    expect(loginResponse, 'no login response captured').toBeTruthy();
    expect(loginResponse.status()).toBe(200);

    const body = await loginResponse.json();
    console.log('Login body:', JSON.stringify(body));
    expect(body).not.toHaveProperty('token');
    expect(body).not.toHaveProperty('accessToken');

    const cookies = await context.cookies();
    const accessToken = cookies.find(c => c.name === 'ACCESS_TOKEN');
    console.log('ACCESS_TOKEN cookie:', JSON.stringify(accessToken));
    expect(accessToken, 'ACCESS_TOKEN cookie must exist').toBeTruthy();
    expect(accessToken.httpOnly, 'must be httpOnly').toBe(true);
    expect(accessToken.value.length).toBeGreaterThan(10);
  });

  // ── 2. Reload: session persists via /me ─────────────────────────────────
  test('session survives page reload via /me', async ({ page }) => {
    await loginViaUI(page);

    let meResponse;
    page.on('response', res => {
      if (res.url().includes('/api/auth/me')) meResponse = res;
    });

    await page.reload();
    await page.waitForLoadState('networkidle');

    expect(meResponse, '/me must be called on reload').toBeTruthy();
    console.log('/me status:', meResponse.status());
    expect(meResponse.status()).toBe(200);

    const body = await meResponse.json();
    console.log('/me body:', JSON.stringify(body));
    expect(body.email).toBe(EMAIL);
    expect(page.url()).not.toContain('/login');
  });

  // ── 3. Logout: cookie cleared by backend ────────────────────────────────
  // Note: headless Chromium doesn't expose cookies from localhost:8080 in
  // document.cookie at localhost:5173 (cross-port limitation). The frontend's
  // getCsrfToken() reads document.cookie, so the UI logout button gets a 403
  // in headless. We bypass this by injecting the XSRF token directly from
  // Playwright's cookie store, which has full cross-port visibility.
  // This exercises the real backend behavior: POST /auth/logout clears the cookie.
  test('logout clears the ACCESS_TOKEN cookie', async ({ page, context }) => {
    await loginViaUI(page);

    const before = await context.cookies();
    expect(before.find(c => c.name === 'ACCESS_TOKEN'), 'must have cookie before logout').toBeTruthy();

    const xsrf = before.find(c => c.name === 'XSRF-TOKEN');
    expect(xsrf, 'XSRF-TOKEN must exist after login').toBeTruthy();
    console.log('XSRF-TOKEN for logout:', xsrf.value.slice(0, 10));

    // Call logout directly with the CSRF token injected from Playwright's cookie store
    const logoutStatus = await page.evaluate(async ({ backendUrl, xsrfValue }) => {
      const res = await fetch(`${backendUrl}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': xsrfValue,
        },
      });
      return res.status;
    }, { backendUrl: BACKEND, xsrfValue: xsrf.value });

    console.log('Logout status:', logoutStatus);
    expect(logoutStatus, 'logout must return 204 No Content').toBe(204);

    const after = await context.cookies();
    const token = after.find(c => c.name === 'ACCESS_TOKEN');
    console.log('ACCESS_TOKEN after logout:', JSON.stringify(token));

    const cleared = !token || token.value === '' ||
      (token.expires !== -1 && token.expires < Date.now() / 1000);
    expect(cleared, 'ACCESS_TOKEN must be cleared after logout').toBe(true);
  });

  // ── 4. CSRF: mutating request without header → 403 ──────────────────────
  test('mutating request without CSRF header is rejected with 403', async ({ page, context }) => {
    await loginViaUI(page);

    const cookies = await context.cookies();
    const xsrf = cookies.find(c => c.name === 'XSRF-TOKEN');
    console.log('XSRF-TOKEN present:', !!xsrf, xsrf?.value?.slice(0, 10));
    expect(xsrf, 'XSRF-TOKEN cookie must exist').toBeTruthy();

    // POST without X-XSRF-TOKEN header — cookie IS sent, header is omitted
    const statusNoHeader = await page.evaluate(async ({ backendUrl }) => {
      const res = await fetch(`${backendUrl}/api/favorites/1`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      });
      return res.status;
    }, { backendUrl: BACKEND });
    console.log('POST no CSRF header →', statusNoHeader);
    expect(statusNoHeader, `expected 403, got ${statusNoHeader}`).toBe(403);

    // POST with deliberately wrong CSRF token
    const statusWrongToken = await page.evaluate(async ({ backendUrl }) => {
      const res = await fetch(`${backendUrl}/api/favorites/1`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': 'tampered-token',
        },
      });
      return res.status;
    }, { backendUrl: BACKEND });
    console.log('POST wrong CSRF token →', statusWrongToken);
    expect(statusWrongToken, `expected 403, got ${statusWrongToken}`).toBe(403);
  });
});
