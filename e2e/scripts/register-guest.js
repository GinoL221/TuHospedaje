const { randomUUID } = require('node:crypto');

function createGuest() {
  const id = randomUUID();
  return {
    name: 'Disposable E2E Guest',
    email: `e2e-disposable-${id}@example.invalid`,
    password: randomUUID(),
  };
}

async function registerGuest({ guest, transport = ({ url, ...options }) => global.fetch(url, options) }) {
  try {
    const response = await transport({
      method: 'POST',
      url: 'http://localhost:8080/api/auth/register',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(guest),
    });
    if (response.status !== 201) throw new Error('unexpected status');
    return guest;
  } catch {
    throw new Error('Disposable guest registration failed.');
  }
}

module.exports = { createGuest, registerGuest };
