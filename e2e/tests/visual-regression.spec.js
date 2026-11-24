// @ts-check
const { test, expect } = require('@playwright/test');

test.use({
  locale: 'es-AR',
  timezoneId: 'UTC',
  colorScheme: 'light',
  reducedMotion: 'reduce',
  deviceScaleFactor: 1,
});

const SNAPSHOT_OPTIONS = {
  fullPage: true,
  threshold: 0.3,
  maxDiffPixels: 1000,
};

function svgDataUri(label, bgColor, accentColor) {
  const svg = [
    '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">',
    `<rect width="800" height="600" fill="${bgColor}"/>`,
    `<rect x="48" y="48" width="704" height="504" rx="24" fill="${accentColor}" opacity="0.22"/>`,
    `<circle cx="620" cy="170" r="56" fill="${accentColor}" opacity="0.45"/>`,
    `<path d="M80 490 L270 280 L430 430 L560 320 L720 490 Z" fill="${accentColor}" opacity="0.65"/>`,
    `<text x="400" y="540" text-anchor="middle" font-family="sans-serif" font-size="28" fill="#1F2937">${label}</text>`,
    '</svg>',
  ].join('');
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

const CATEGORIES = [
  {
    id: 1,
    name: 'Hoteles',
    description: 'Estadías confortables en centros urbanos y turísticos',
    icon: 'hotel',
  },
  {
    id: 2,
    name: 'Cabañas',
    description: 'Refugios rodeados de naturaleza y tranquilidad',
    icon: 'tree-pine',
  },
  {
    id: 3,
    name: 'Departamentos',
    description: 'Espacios equipados para viajes cortos o largos',
    icon: 'building-2',
  },
  {
    id: 4,
    name: 'Glamping',
    description: 'Naturaleza al aire libre con comodidades de hotel',
    icon: 'tent',
  },
];

const LODGINGS = [
  {
    id: 1,
    name: 'Refugio del Lago Nahuel',
    description:
      'Cabaña luminosa con ventanales al lago, hogar a leña y deck privado para disfrutar todo el año.',
    city: 'San Carlos de Bariloche',
    country: 'Argentina',
    address: 'Av. Bustillo 4200',
    pricePerNight: 145000,
    averageRating: 4.8,
    ratingCount: 2,
    category: CATEGORIES[1],
    imageUrls: [
      svgDataUri('Refugio del Lago 1', '#E6F4F1', '#1D7A6E'),
      svgDataUri('Refugio del Lago 2', '#EEF2FF', '#4338CA'),
      svgDataUri('Refugio del Lago 3', '#FEF3C7', '#D97706'),
      svgDataUri('Refugio del Lago 4', '#FCE7F3', '#BE185D'),
      svgDataUri('Refugio del Lago 5', '#ECFDF5', '#047857'),
    ],
    features: [
      { id: 1, name: 'Wi-Fi de alta velocidad', icon: 'wifi' },
      { id: 2, name: 'Estacionamiento privado', icon: 'car' },
      { id: 3, name: 'Aire acondicionado', icon: 'thermometer-snowflake' },
      { id: 4, name: 'Cocina equipada', icon: 'kitchen-set' },
      { id: 5, name: 'Smart TV', icon: 'tv' },
      { id: 6, name: 'Apto mascotas', icon: 'paw-print' },
    ],
    policies: [
      {
        id: 1,
        name: 'Check-in y Check-out',
        description: 'Ingreso desde las 15:00 y salida hasta las 11:00.',
        icon: 'clock',
      },
      {
        id: 2,
        name: 'No fumar',
        description: 'Espacio 100% libre de humo en ambientes cerrados.',
        icon: 'smoking-ban',
      },
      {
        id: 3,
        name: 'Cancelación flexible',
        description: 'Sin cargo hasta 48 horas antes del ingreso.',
        icon: 'ban',
      },
    ],
  },
  {
    id: 2,
    name: 'Hotel Boutique Recoleta',
    description:
      'Habitaciones de diseño con desayuno artesanal a pasos de parques, museos y cafés históricos.',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Av. Alvear 1520',
    pricePerNight: 185000,
    averageRating: 4.6,
    ratingCount: 18,
    category: CATEGORIES[0],
    imageUrls: [svgDataUri('Hotel Recoleta', '#F3E8FF', '#7E22CE')],
  },
  {
    id: 3,
    name: 'Loft Viñedos del Sur',
    description:
      'Departamento moderno con terraza panorámica hacia la cordillera y bodegas cercanas.',
    city: 'Mendoza',
    country: 'Argentina',
    address: 'Ruta Provincial 15 Km 22',
    pricePerNight: 120000,
    averageRating: 4.9,
    ratingCount: 11,
    category: CATEGORIES[2],
    imageUrls: [svgDataUri('Loft Mendoza', '#FFF7ED', '#C2410C')],
  },
  {
    id: 4,
    name: 'Domos Valle Calchaquí',
    description:
      'Experiencia de glamping bajo cielos despejados con desayuno regional y senderos guiados.',
    city: 'Cafayate',
    country: 'Argentina',
    address: 'Camino a las Bodegas 850',
    pricePerNight: 132000,
    averageRating: 4.7,
    ratingCount: 9,
    category: CATEGORIES[3],
    imageUrls: [svgDataUri('Domos Cafayate', '#FEF2F2', '#B91C1C')],
  },
];

const AVAILABILITY = {
  lodgingId: 1,
  occupiedRanges: [
    {
      checkIn: '2026-12-10',
      checkOut: '2026-12-13',
    },
  ],
};

const RATINGS = {
  average: 4.8,
  count: 2,
  ratings: [
    {
      id: 1,
      userName: 'Lucía Fernández',
      score: 5,
      comment: 'Excelente vista al lago y atención impecable durante toda la estadía.',
      createdAt: '2026-11-15T14:30:00Z',
    },
    {
      id: 2,
      userName: 'Martín Gómez',
      score: 4,
      comment: 'Muy cómodo, limpio y tranquilo para descansar en familia.',
      createdAt: '2026-11-20T18:00:00Z',
    },
  ],
};

async function stubApi(page) {
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Unauthorized' }),
    }),
  );

  await page.route('**/api/auth/csrf', (route) =>
    route.fulfill({
      status: 204,
      body: '',
    }),
  );

  await page.route('**/api/categories', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(CATEGORIES),
    }),
  );

  await page.route('**/api/lodgings/recommendations**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        lodgings: LODGINGS,
        currentPage: 0,
        totalPages: 1,
        totalElements: LODGINGS.length,
        revision: 'visual-snapshot-rev-1',
        reset: false,
      }),
    }),
  );

  await page.route('**/api/lodgings/random**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(LODGINGS),
    }),
  );

  await page.route('**/api/lodgings/1/availability**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(AVAILABILITY),
    }),
  );

  await page.route('**/api/lodgings/1', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(LODGINGS[0]),
    }),
  );

  await page.route('**/api/ratings/lodging/1', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(RATINGS),
    }),
  );
}

async function waitForStableVisualState(page) {
  await page.evaluate(async () => {
    await document.fonts.ready;
    const images = Array.from(document.querySelectorAll('img'));
    await Promise.all(
      images.map((img) => {
        if (img.complete) return Promise.resolve();
        return new Promise((resolve) => {
          img.addEventListener('load', resolve, { once: true });
          img.addEventListener('error', resolve, { once: true });
        });
      }),
    );
  });
}

test.describe('Visual regression snapshots', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-12-01T12:00:00Z'));
    await stubApi(page);
  });

  test('home page matches baseline', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.category-tag')).toHaveCount(4);
    await expect(page.locator('.hotel-card')).toHaveCount(4);
    await waitForStableVisualState(page);

    await expect(page).toHaveScreenshot('home.png', SNAPSHOT_OPTIONS);
  });

  test('lodging detail page matches baseline', async ({ page }) => {
    await page.goto('/lodgings/1');
    await expect(page.locator('#product-detail-title')).toHaveText('Refugio del Lago Nahuel');
    await expect(page.locator('.reviews-list .review-item')).toHaveCount(2);
    await expect(page.locator('.booking-section')).toBeVisible();
    await expect(page.getByText('Comprobando disponibilidad...')).toBeHidden();
    await waitForStableVisualState(page);

    await expect(page).toHaveScreenshot('lodging-detail.png', SNAPSHOT_OPTIONS);
  });

  test('login page matches baseline', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('main.login-container')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    await waitForStableVisualState(page);

    await expect(page).toHaveScreenshot('login.png', SNAPSHOT_OPTIONS);
  });

  test('register page matches baseline', async ({ page }) => {
    await page.goto('/register');
    await expect(page.locator('main')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    await waitForStableVisualState(page);

    await expect(page).toHaveScreenshot('register.png', SNAPSHOT_OPTIONS);
  });
});
