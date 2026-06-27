import { Routes, Route } from "react-router-dom";
import { customRender, screen } from "../../test/test-utils";
import BookingConfirmationPage from "./BookingConfirmation";

const reservationFixture = {
  id: 99,
  checkIn: "2026-07-01",
  checkOut: "2026-07-04",
  guestName: "Juan Perez",
  guestEmail: "juan@test.com",
  totalPrice: 300,
};

const lodgingFixture = {
  id: 1,
  name: "Cabaña del Lago",
  city: "Bariloche",
  country: "Argentina",
};

function HomeSentinel() {
  return <div data-testid="home-sentinel">home</div>;
}

function renderConfirmation({ state } = {}) {
  return customRender(
    <Routes>
      <Route path="/booking/confirmation" element={<BookingConfirmationPage />} />
      <Route path="/" element={<HomeSentinel />} />
    </Routes>,
    {
      initialEntries: [{ pathname: "/booking/confirmation", state }],
    }
  );
}

describe("BookingConfirmationPage - with valid state", () => {
  it("shows the lodging name, formatted dates, guest name, and total price", () => {
    renderConfirmation({ state: { reservation: reservationFixture, lodging: lodgingFixture } });

    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
    // fmtDate reverses ISO: "2026-07-01" → "01/07/2026"
    expect(screen.getByText(/01\/07\/2026/)).toBeInTheDocument();
    expect(screen.getByText("Juan Perez")).toBeInTheDocument();
    expect(screen.getByText(/300/)).toBeInTheDocument();
  });

  it("renders navigation links to /my-reservations and /", () => {
    renderConfirmation({ state: { reservation: reservationFixture, lodging: lodgingFixture } });

    expect(screen.getByRole("link", { name: "Ver mis reservas" })).toHaveAttribute(
      "href",
      "/my-reservations"
    );
    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute("href", "/");
  });

  it("shows the email confirmation notice", () => {
    renderConfirmation({ state: { reservation: reservationFixture, lodging: lodgingFixture } });

    expect(screen.getByText(/email de confirmación/i)).toBeInTheDocument();
  });
});

describe("BookingConfirmationPage - without state", () => {
  it("redirects to / when accessed without reservation state", async () => {
    renderConfirmation();

    expect(await screen.findByTestId("home-sentinel")).toBeInTheDocument();
  });
});
