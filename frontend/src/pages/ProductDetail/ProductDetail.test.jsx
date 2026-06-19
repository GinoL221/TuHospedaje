import { Routes, Route } from "react-router-dom";
import { customRender, screen, userEvent, makeAuthValue } from "../../test/test-utils";
import ProductDetail from "./ProductDetail";
import { get } from "../../services/api";

vi.mock("../../services/api");

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

function renderProductDetail({ authValue, initialEntries = ["/lodgings/1"] } = {}) {
  return customRender(
    <Routes>
      <Route path="/lodgings/:id" element={<ProductDetail />} />
      <Route path="/booking/:id" element={<BookingSentinel />} />
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

    await user.click(checkInInput);
    const checkInDay = document.querySelector(
      ".react-datepicker__day:not(.react-datepicker__day--disabled)"
    );
    await user.click(checkInDay);

    await user.click(checkOutInput);
    const checkOutDay = Array.from(
      document.querySelectorAll(".react-datepicker__day:not(.react-datepicker__day--disabled)")
    ).at(-1);
    await user.click(checkOutDay);

    const reserveButton = screen.getByRole("button", { name: "Reservar" });
    expect(reserveButton).not.toBeDisabled();
    await user.click(reserveButton);

    expect(await screen.findByTestId("booking-sentinel")).toBeInTheDocument();
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
