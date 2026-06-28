import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AdminDashboard from "./AdminDashboard";
import { get } from "../../services/api";

vi.mock("../../services/api");

function mockGetDefaults({
  lodgingsCount = [],
  recentLodgings = [],
  reservations = [],
} = {}) {
  get.mockImplementation((endpoint) => {
    if (endpoint === "/lodgings") return Promise.resolve(lodgingsCount);
    if (endpoint === "/lodgings?page=0&size=4")
      return Promise.resolve({ lodgings: recentLodgings });
    if (endpoint === "/categories") return Promise.resolve([]);
    if (endpoint === "/features") return Promise.resolve([]);
    if (endpoint === "/users") return Promise.resolve([]);
    if (endpoint === "/reservations") return Promise.resolve(reservations);
    return Promise.resolve([]);
  });
}

describe("AdminDashboard - stat count", () => {
  it("shows … initially then the count once the fetch resolves", async () => {
    mockGetDefaults({ lodgingsCount: [{ id: 1 }, { id: 2 }] });
    render(<AdminDashboard onTabChange={vi.fn()} />);

    expect(screen.getAllByText("…").length).toBeGreaterThan(0);
    expect(await screen.findByText("2")).toBeInTheDocument();
  });

  it("shows — for a stat when its endpoint rejects", async () => {
    get.mockImplementation((endpoint) => {
      if (endpoint === "/lodgings") return Promise.reject(new Error("fail"));
      if (endpoint === "/lodgings?page=0&size=4")
        return Promise.resolve({ lodgings: [] });
      return Promise.resolve([]);
    });
    render(<AdminDashboard onTabChange={vi.fn()} />);

    expect(await screen.findByText("—")).toBeInTheDocument();
  });
});

describe("AdminDashboard - tab navigation", () => {
  it("calls onTabChange with the correct tab key when a stat card is clicked", async () => {
    mockGetDefaults();
    const onTabChange = vi.fn();
    const user = userEvent.setup();
    render(<AdminDashboard onTabChange={onTabChange} />);

    await user.click(
      screen.getByText("Alojamientos").closest('[role="button"]')
    );
    expect(onTabChange).toHaveBeenCalledWith("lodgings");
  });

  it("calls onTabChange with reservations tab when that card is clicked", async () => {
    mockGetDefaults();
    const onTabChange = vi.fn();
    const user = userEvent.setup();
    render(<AdminDashboard onTabChange={onTabChange} />);

    await user.click(
      screen.getByText("Reservas").closest('[role="button"]')
    );
    expect(onTabChange).toHaveBeenCalledWith("reservations");
  });
});

describe("AdminDashboard - recent lodgings table", () => {
  it("renders the recent lodgings table when there are recent lodgings", async () => {
    mockGetDefaults({ recentLodgings: [{ id: 1, name: "Hotel Sol" }] });
    render(<AdminDashboard onTabChange={vi.fn()} />);

    expect(await screen.findByText("Últimos alojamientos")).toBeInTheDocument();
    expect(screen.getByText("Hotel Sol")).toBeInTheDocument();
  });

  it("does not render the recent lodgings table when the list is empty", async () => {
    mockGetDefaults();
    render(<AdminDashboard onTabChange={vi.fn()} />);

    await waitFor(() => expect(get).toHaveBeenCalled());
    expect(screen.queryByText("Últimos alojamientos")).not.toBeInTheDocument();
  });
});

describe("AdminDashboard - recent reservations table", () => {
  it("does not render the recent reservations table when the list is empty", async () => {
    mockGetDefaults();
    render(<AdminDashboard onTabChange={vi.fn()} />);

    await waitFor(() => expect(get).toHaveBeenCalled());
    expect(screen.queryByText("Últimas reservas")).not.toBeInTheDocument();
  });

  it("renders status badges — Confirmada for CONFIRMED and Cancelada otherwise", async () => {
    const reservations = [
      {
        id: 1,
        lodgingName: "Cabaña",
        guestName: "Ana García",
        checkIn: "2026-07-01",
        checkOut: "2026-07-04",
        totalPrice: 300,
        status: "CONFIRMED",
      },
      {
        id: 2,
        lodgingName: "Hostal",
        guestName: "Luis Pérez",
        checkIn: "2026-08-01",
        checkOut: "2026-08-03",
        totalPrice: 200,
        status: "CANCELLED",
      },
    ];
    mockGetDefaults({ reservations });
    render(<AdminDashboard onTabChange={vi.fn()} />);

    expect(await screen.findByText("Últimas reservas")).toBeInTheDocument();
    expect(screen.getByText("Confirmada")).toBeInTheDocument();
    expect(screen.getByText("Cancelada")).toBeInTheDocument();
  });
});
