import { Routes, Route, useLocation } from "react-router-dom";
import { customRender, screen, userEvent, makeAuthValue } from "../../test/test-utils";
import ProductDetail from "./ProductDetail";
import { get } from "../../services/api";

vi.mock("../../services/api");

vi.mock("../../components/ShareModal/ShareModal", () => ({
  default: ({ onClose }) => (
    <div data-testid="share-modal">
      <button onClick={onClose}>Close</button>
    </div>
  ),
}));

vi.mock("../../components/GalleryModal/GalleryModal", () => ({
  default: () => <div data-testid="gallery-modal" />,
}));

function BookingSentinel() {
  return <div data-testid="booking-sentinel">booking page</div>;
}

const lodgingFixture = {
  id: 1,
  name: "Cabaña del Lago",
  city: "Bariloche",
  country: "Argentina",
  description: "Una cabaña con vista al lago.",
  pricePerNight: 100,
  imageUrls: ["https://example.com/img.jpg"],
};

function mockGetDefaults({ lodging = lodgingFixture, availability = {}, ratings = {} } = {}) {
  get.mockImplementation((endpoint) => {
    if (endpoint.startsWith(`/lodgings/${lodging?.id ?? 1}/availability`)) {
      return Promise.resolve(availability);
    }
    if (endpoint === `/lodgings/${lodging?.id ?? 1}`) {
      return Promise.resolve(lodging);
    }
    if (endpoint.startsWith("/ratings/lodging/")) {
      return Promise.resolve({ average: 0, count: 0, ratings: [], ...ratings });
    }
    return Promise.resolve(null);
  });
}

function LoginSentinel() {
  const location = useLocation();
  return (
    <div data-testid="login-sentinel">
      login page, from: {location.state?.from?.pathname ?? "none"}
    </div>
  );
}

function renderProductDetail({ authValue, initialEntries = ["/lodgings/1"] } = {}) {
  return customRender(
    <Routes>
      <Route path="/lodgings/:id" element={<ProductDetail />} />
      <Route path="/booking/:id" element={<BookingSentinel />} />
      <Route path="/login" element={<LoginSentinel />} />
    </Routes>,
    { authValue, initialEntries }
  );
}

describe("ProductDetail - rendering lodging detail", () => {
  it("shows a loading state before the lodging resolves, then renders the summary", async () => {
    mockGetDefaults();
    renderProductDetail();

    expect(screen.getByText("Cargando...")).toBeInTheDocument();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/lodgings/1");
    expect(screen.getByText("Bariloche, Argentina")).toBeInTheDocument();
    expect(screen.getByText("Una cabaña con vista al lago.")).toBeInTheDocument();
  });
});

describe("ProductDetail - useAuth integration for the reserve CTA", () => {
  it("shows a login prompt instead of the reserve button for anonymous users", async () => {
    mockGetDefaults();
    renderProductDetail({ authValue: null });

    await screen.findByText("Cabaña del Lago");

    expect(screen.getByRole("link", { name: "Iniciá sesión" })).toHaveAttribute(
      "href",
      "/login"
    );
    expect(screen.queryByRole("button", { name: "Reservar" })).not.toBeInTheDocument();
  });

  it("preserves the current location as state.from on the login link, so login can redirect back here", async () => {
    mockGetDefaults();
    const user = userEvent.setup();
    renderProductDetail({ authValue: null, initialEntries: ["/lodgings/1"] });

    await screen.findByText("Cabaña del Lago");

    const loginLink = screen.getByRole("link", { name: "Iniciá sesión" });
    await user.click(loginLink);

    // Same pattern RequireAuth already uses: the login link must carry
    // state.from = current location, so LoginPage's
    // `location.state?.from?.pathname` resolves back here instead of
    // falling back to "/".
    expect(await screen.findByTestId("login-sentinel")).toHaveTextContent(
      "from: /lodgings/1"
    );
  });

  it("disables the reserve button for a logged-in user until both dates are selected", async () => {
    mockGetDefaults();
    const authValue = makeAuthValue();
    renderProductDetail({ authValue });

    await screen.findByText("Cabaña del Lago");

    expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
  });
});

describe("ProductDetail - navigation to booking", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date("2026-07-15"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("navigates to /booking/:id with checkIn/checkOut state once dates are selected", async () => {
    mockGetDefaults();
    const authValue = makeAuthValue();
    const user = userEvent.setup();
    const { container } = renderProductDetail({ authValue });

    await screen.findByText("Cabaña del Lago");

    // react-datepicker renders a calendar popup on click; typing a formatted
    // date string into the input is unreliable under jsdom (same quirk
    // already documented for BookingPage in PR #4), so we open each
    // calendar and click an enabled day instead.
    const [checkInInput, checkOutInput] = container.querySelectorAll(
      ".react-datepicker-wrapper input"
    );

    // react-datepicker marks enabled/disabled state via aria-disabled on
    // each gridcell (a stable accessibility contract), instead of relying
    // on its internal "--disabled" CSS class which can change across
    // library versions.
    await user.click(checkInInput);
    const checkInDay = document.querySelector(
      '[role="gridcell"][aria-disabled="false"]'
    );
    await user.click(checkInDay);

    await user.click(checkOutInput);
    const checkOutDay = Array.from(
      document.querySelectorAll('[role="gridcell"][aria-disabled="false"]')
    ).at(-1);
    await user.click(checkOutDay);

    const reserveButton = screen.getByRole("button", { name: "Reservar" });
    expect(reserveButton).not.toBeDisabled();
    await user.click(reserveButton);

    expect(await screen.findByTestId("booking-sentinel")).toBeInTheDocument();
  });

  // NOTE: minCheckoutDate's date-arithmetic contract (day-after-checkIn,
  // month/year rollover, null/undefined fallback) is unit-tested
  // exhaustively in src/utils/dateRange.test.js. This test only checks the
  // thin page-specific wiring: that the check-out DatePicker's minDate prop
  // is actually connected to minCheckoutDate(checkIn), not duplicating the
  // date-math assertions already owned by dateRange.test.js.
  it("disables the same day as check-in in the check-out calendar, requiring at least one night", async () => {
    mockGetDefaults();
    const authValue = makeAuthValue();
    const user = userEvent.setup();
    const { container } = renderProductDetail({ authValue });

    await screen.findByText("Cabaña del Lago");

    const [checkInInput, checkOutInput] = container.querySelectorAll(
      ".react-datepicker-wrapper input"
    );

    // Select today's earliest enabled day as check-in. react-datepicker
    // marks enabled/disabled state via aria-disabled on each gridcell (a
    // stable accessibility contract), instead of relying on its internal
    // "--disabled" CSS class which can change across library versions.
    await user.click(checkInInput);
    const checkInDay = document.querySelector(
      '[role="gridcell"][aria-disabled="false"]'
    );
    const checkInDayNumber = Number(checkInDay.textContent);
    await user.click(checkInDay);

    // Open the check-out calendar: the same day must now be disabled,
    // proving minDate={minCheckoutDate(checkIn)} is actually wired up
    // (a booking requires at least one night, so checkOut > checkIn).
    // "outside-month" has no aria/role equivalent in react-datepicker, so
    // that one filter still relies on the internal CSS class.
    await user.click(checkOutInput);
    const sameDayInCheckoutCalendar = Array.from(
      document.querySelectorAll('[role="gridcell"]')
    ).find(
      (day) =>
        Number(day.textContent) === checkInDayNumber &&
        !day.classList.contains("react-datepicker__day--outside-month")
    );

    expect(sameDayInCheckoutCalendar).toHaveAttribute("aria-disabled", "true");
  });
});

describe("ProductDetail - ShareModal", () => {
  it("opens ShareModal when the Compartir button is clicked", async () => {
    mockGetDefaults();
    const user = userEvent.setup();
    renderProductDetail();

    await screen.findByText("Cabaña del Lago");

    expect(screen.queryByTestId("share-modal")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Compartir" }));

    expect(screen.getByTestId("share-modal")).toBeInTheDocument();
  });

  it("closes ShareModal when onClose is called", async () => {
    mockGetDefaults();
    const user = userEvent.setup();
    renderProductDetail();

    await screen.findByText("Cabaña del Lago");

    await user.click(screen.getByRole("button", { name: "Compartir" }));
    expect(screen.getByTestId("share-modal")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Close" }));
    expect(screen.queryByTestId("share-modal")).not.toBeInTheDocument();
  });
});

describe("ProductDetail - GalleryModal", () => {
  it("opens GalleryModal when the main gallery image trigger is clicked", async () => {
    mockGetDefaults();
    const user = userEvent.setup();
    renderProductDetail();

    await screen.findByText("Cabaña del Lago");

    expect(screen.queryByTestId("gallery-modal")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Abrir galería" }));

    expect(screen.getByTestId("gallery-modal")).toBeInTheDocument();
  });
});

describe("ProductDetail - ReviewsSection integration", () => {
  it("renders the nested ReviewsSection with data fetched for this lodging", async () => {
    mockGetDefaults({ ratings: { average: 4.2, count: 5, ratings: [] } });
    renderProductDetail();

    await screen.findByText("Cabaña del Lago");

    expect(get).toHaveBeenCalledWith("/ratings/lodging/1");
    expect(await screen.findByText("4.2")).toBeInTheDocument();
    expect(screen.getByText("(5 reseñas)")).toBeInTheDocument();
  });
});
