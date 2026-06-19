import { customRender, screen } from "../../test/test-utils";
import MyReservationsPage from "./MyReservationsPage";
import { get } from "../../services/api";

vi.mock("../../services/api");

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
