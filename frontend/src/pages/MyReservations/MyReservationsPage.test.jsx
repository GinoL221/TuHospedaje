import userEvent from "@testing-library/user-event";
import { customRender, screen } from "../../test/test-utils";
import MyReservationsPage from "./MyReservationsPage";
import { get } from "../../services/api";
import { cancelReservation } from "../../services/reservationService";

vi.mock("../../services/api");
vi.mock("../../services/reservationService");

const reservationFixture = {
  id: 1,
  lodgingId: 10,
  lodgingName: "Cabaña del Lago",
  city: "Bariloche",
  status: "CONFIRMED",
  checkIn: "2026-07-01",
  checkOut: "2026-07-04",
  guestName: "Test User",
  guestEmail: "test@example.com",
  guestPhone: "123456",
  totalPrice: 300,
};

describe("MyReservationsPage - reservation list", () => {
  beforeEach(() => {
    vi.setSystemTime(new Date("2026-06-30T15:00:00-03:00"));
  });

  afterEach(() => vi.useRealTimers());

  it("renders the user's reservations with nights computed from checkIn/checkOut", async () => {
    get.mockResolvedValue([reservationFixture]);
    customRender(<MyReservationsPage />);

    expect(screen.getByText("Cargando reservas...")).toBeInTheDocument();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/reservations/my");
    expect(screen.getByText("3 noches")).toBeInTheDocument();
    expect(screen.getByText("Total:")).toBeInTheDocument();
    expect(screen.getByText("$300")).toBeInTheDocument();
  });

  it("shows a singular count label and singular night label for one reservation", async () => {
    get.mockResolvedValue([
      { ...reservationFixture, checkIn: "2026-07-01", checkOut: "2026-07-02" },
    ]);
    customRender(<MyReservationsPage />);

    expect(await screen.findByText("1 reserva")).toBeInTheDocument();
    expect(screen.getByText("1 noche")).toBeInTheDocument();
  });

  it("keeps long reservation content visible and addressable", async () => {
    const longReservation = {
      ...reservationFixture,
      lodgingName: "Alojamiento con un nombre excepcionalmente largo para pantallas pequeñas",
      guestEmail: "persona.con.un.correo.muy.largo@subdominio.example.com",
      guestPhone: "+54 9 11 5555 1234 9876",
      checkIn: "2026-07-01",
      checkOut: "2026-07-31",
      totalPrice: 1234567,
    };
    get.mockResolvedValue([longReservation]);
    customRender(<MyReservationsPage />);

    expect(await screen.findByText(longReservation.lodgingName)).toBeInTheDocument();
    expect(screen.getByText(longReservation.guestEmail)).toBeInTheDocument();
    expect(screen.getByText(longReservation.guestPhone)).toBeInTheDocument();
    expect(screen.getByText("01/07/2026 → 31/07/2026")).toBeInTheDocument();
    expect(screen.getByText("30 noches")).toBeInTheDocument();
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    expect(
      screen.getByText(/^\$\d{1,3}(?:[,.]\d{3})+$/, {
        selector: ".reservation-total strong",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Ver alojamiento/ })).toHaveAttribute(
      "href",
      "/lodgings/10",
    );
    expect(screen.getByRole("button", { name: "Cancelar reserva" })).toBeInTheDocument();
  });

  it("offers cancellation only for confirmed reservations before check-in", async () => {
    get.mockResolvedValue([
      reservationFixture,
      { ...reservationFixture, id: 2, lodgingName: "Cancelled", status: "CANCELLED" },
      { ...reservationFixture, id: 3, lodgingName: "Today", checkIn: "2026-06-30" },
    ]);
    customRender(<MyReservationsPage />);

    expect(await screen.findByRole("button", { name: "Cancelar reserva" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Cancelar reserva" })).toHaveLength(1);
  });

  it("requires confirmation, prevents duplicate requests, and replaces the cancelled row", async () => {
    const user = userEvent.setup();
    let resolveCancellation;
    cancelReservation.mockReturnValue(new Promise((resolve) => { resolveCancellation = resolve; }));
    get.mockResolvedValue([reservationFixture]);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    customRender(<MyReservationsPage />);

    const button = await screen.findByRole("button", { name: "Cancelar reserva" });
    await user.click(button);

    expect(window.confirm).toHaveBeenCalled();
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent("Cancelando...");
    await user.click(button);
    expect(cancelReservation).toHaveBeenCalledTimes(1);

    resolveCancellation({ ...reservationFixture, status: "CANCELLED" });
    expect(await screen.findByText("CANCELLED")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancelar reserva" })).not.toBeInTheDocument();
  });

  it("does not cancel when confirmation is declined", async () => {
    const user = userEvent.setup();
    get.mockResolvedValue([reservationFixture]);
    vi.spyOn(window, "confirm").mockReturnValue(false);
    customRender(<MyReservationsPage />);

    await user.click(await screen.findByRole("button", { name: "Cancelar reserva" }));

    expect(cancelReservation).not.toHaveBeenCalled();
  });

  it("keeps the row usable and shows an inline error when cancellation fails", async () => {
    const user = userEvent.setup();
    get.mockResolvedValue([reservationFixture]);
    cancelReservation.mockRejectedValue(new Error("Intentá nuevamente."));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    customRender(<MyReservationsPage />);

    await user.click(await screen.findByRole("button", { name: "Cancelar reserva" }));

    expect(await screen.findByText("Intentá nuevamente.")).toBeInTheDocument();
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancelar reserva" })).toBeEnabled();
  });
});

describe("MyReservationsPage - empty state", () => {
  it("shows a CTA to explore lodgings when there are no reservations", async () => {
    get.mockResolvedValue([]);
    customRender(<MyReservationsPage />);

    expect(await screen.findByText("No tenés reservas todavía.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Explorar alojamientos" })).toHaveAttribute(
      "href",
      "/"
    );
  });
});

describe("MyReservationsPage - fetch failure", () => {
  it("renders the error message instead of the list when the fetch rejects", async () => {
    get.mockRejectedValue(new Error("No se pudieron cargar las reservas."));
    customRender(<MyReservationsPage />);

    expect(
      await screen.findByText("No se pudieron cargar las reservas.")
    ).toBeInTheDocument();
    // The current implementation only suppresses the "no reservations" empty
    // state when there's an error (`!error && reservations.length === 0`),
    // but it does not render the reservations list either since the array
    // stays empty after a rejected fetch — no crash, error renders alone.
    expect(screen.queryByText("No tenés reservas todavía.")).not.toBeInTheDocument();
  });
});
