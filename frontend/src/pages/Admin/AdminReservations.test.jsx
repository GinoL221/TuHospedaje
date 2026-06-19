import { customRender, screen, waitFor } from "../../test/test-utils";
import AdminReservations from "./AdminReservations";
import { get } from "../../services/api";

vi.mock("../../services/api");

const reservationFixture = (overrides = {}) => ({
  id: 1,
  lodgingName: "Cabaña del Lago",
  guestName: "Ana Gomez",
  checkIn: "2026-07-01",
  checkOut: "2026-07-05",
  totalPrice: 400,
  status: "CONFIRMED",
  ...overrides,
});

function renderAdminReservations(options) {
  return customRender(<AdminReservations />, options);
}

describe("AdminReservations - listing", () => {
  it("shows a loading state before the fetch resolves", async () => {
    let resolveFetch;
    get.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveFetch = resolve;
        })
    );
    renderAdminReservations();

    expect(screen.getByText("Cargando reservas...")).toBeInTheDocument();

    resolveFetch([]);
    await waitFor(() => {
      expect(screen.queryByText("Cargando reservas...")).not.toBeInTheDocument();
    });
  });

  it("renders the empty state when there are no reservations", async () => {
    get.mockResolvedValue([]);
    renderAdminReservations();

    expect(
      await screen.findByText("No hay reservas registradas.")
    ).toBeInTheDocument();
  });

  it("renders a row per reservation with lodging, guest, dates, total and status", async () => {
    get.mockResolvedValue([
      reservationFixture({ id: 1, lodgingName: "Cabaña del Lago", status: "CONFIRMED" }),
      reservationFixture({ id: 2, lodgingName: "Hotel Centro", status: "CANCELLED" }),
    ]);
    renderAdminReservations();

    expect(await screen.findByText("Cabaña del Lago")).toBeInTheDocument();
    expect(screen.getByText("Hotel Centro")).toBeInTheDocument();
    expect(screen.getAllByText("Ana Gomez")).toHaveLength(2);

    const row1 = screen.getByTestId("row-1");
    const row2 = screen.getByTestId("row-2");
    expect(row1).toHaveTextContent("Confirmada");
    expect(row2).toHaveTextContent("Cancelada");
    expect(row1.querySelector(".status-badge")).toHaveClass("status-confirmed");
    expect(row2.querySelector(".status-badge")).toHaveClass("status-cancelled");
  });

  it("fetches from GET /reservations on mount", async () => {
    get.mockResolvedValue([reservationFixture()]);
    renderAdminReservations();

    await screen.findByText("Cabaña del Lago");
    expect(get).toHaveBeenCalledWith("/reservations");
  });

  it("clears the loading state and logs the error when the fetch rejects", async () => {
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    get.mockRejectedValue(new Error("fail"));
    renderAdminReservations();

    await waitFor(() => {
      expect(screen.queryByText("Cargando reservas...")).not.toBeInTheDocument();
    });
    // SUSPICIOUS: on fetch failure, AdminReservations only logs the error via
    // console.error and falls through to the empty-state message — there is
    // no distinct error UI, unlike the read+write Admin modules which surface
    // failures via alert/inline text on their write operations. Read-only
    // failure here is silently treated as "no data". Asserted as-is per spec
    // Risks; not unified or fixed in this change.
    expect(screen.getByText("No hay reservas registradas.")).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });
});

describe("AdminReservations - read-only scope", () => {
  it("renders no create/edit/delete controls anywhere in the document", async () => {
    get.mockResolvedValue([reservationFixture()]);
    renderAdminReservations();

    await screen.findByText("Cabaña del Lago");

    // AdminReservations is explicitly read-only per spec: list + sort +
    // paginate only. Confirmed in the real component — no add button, no
    // modal, no edit/delete buttons, no ConfirmDialog usage at all.
    expect(screen.queryByTestId("admin-add-btn")).not.toBeInTheDocument();
    expect(screen.queryByTestId("admin-modal")).not.toBeInTheDocument();
    expect(screen.queryByTestId("row-edit-btn")).not.toBeInTheDocument();
    expect(screen.queryByTestId("row-delete-btn")).not.toBeInTheDocument();
    expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /eliminar|editar|agregar/i })).not.toBeInTheDocument();
  });
});
