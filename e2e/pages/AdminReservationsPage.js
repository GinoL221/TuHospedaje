// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminReservationsPage — page object for the Admin › Reservations section.
 *
 * Entity key : "reservations"
 * Operations : read-only listing
 * No create, no edit, no delete, no role toggle.
 *
 * The component shows "Cargando reservas..." while loading, then either
 * the `reservations-table` or an empty-state paragraph.
 */
class AdminReservationsPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'reservations';
  }

  /**
   * Navigate to the Reservations section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Wait for the loading state to disappear.
   * Resolves when "Cargando reservas..." is no longer visible, or after
   * a short timeout — whichever comes first. Never rejects.
   */
  async waitForLoad() {
    try {
      await this.page
        .getByText('Cargando reservas...')
        .waitFor({ state: 'hidden', timeout: 5000 });
    } catch {
      // Graceful timeout — the text may have already disappeared.
    }
  }

  /**
   * Return the reservations table locator.
   * Only present when at least one reservation exists in the database.
   */
  getTable() {
    return this.page.locator('[data-testid="reservations-table"]');
  }
}

module.exports = AdminReservationsPage;
