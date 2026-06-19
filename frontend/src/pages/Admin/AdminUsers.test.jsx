import { customRender, screen, userEvent, makeAuthValue, waitFor } from "../../test/test-utils";
import AdminUsers from "./AdminUsers";
import { get, put } from "../../services/api";

vi.mock("../../services/api");

const userFixture = (overrides = {}) => ({
  id: 1,
  firstName: "Ana",
  lastName: "Gomez",
  email: "ana@example.com",
  role: "USER",
  ...overrides,
});

function renderAdminUsers({ authValue } = {}) {
  return customRender(<AdminUsers />, { authValue });
}

describe("AdminUsers - listing", () => {
  it("renders the empty state when there are no users", async () => {
    get.mockResolvedValue([]);
    renderAdminUsers();

    expect(
      await screen.findByText("No hay usuarios registrados.")
    ).toBeInTheDocument();
  });

  it("renders a row per user with name, email and role badge", async () => {
    get.mockResolvedValue([
      userFixture({ id: 1, firstName: "Ana", lastName: "Gomez", role: "USER" }),
      userFixture({ id: 2, firstName: "Beto", lastName: "Diaz", email: "beto@example.com", role: "ADMIN" }),
    ]);
    renderAdminUsers();

    expect(await screen.findByText("ana@example.com")).toBeInTheDocument();
    expect(screen.getByText("Ana Gomez")).toBeInTheDocument();
    expect(screen.getByText("beto@example.com")).toBeInTheDocument();

    const row1 = screen.getByTestId("row-1");
    const row2 = screen.getByTestId("row-2");
    expect(row1.querySelector(".role-badge")).toHaveTextContent("USER");
    expect(row2.querySelector(".role-badge")).toHaveClass("role-admin");
  });

  it("fetches from GET /users on mount", async () => {
    get.mockResolvedValue([userFixture()]);
    renderAdminUsers();

    await screen.findByText("ana@example.com");
    expect(get).toHaveBeenCalledWith("/users");
  });
});

describe("AdminUsers - role toggle", () => {
  it("asks for confirmation, calls PUT /users/:id/role, and updates the row on accept", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    put.mockResolvedValue(userFixture({ id: 1, role: "ADMIN" }));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    expect(toggleBtn).toHaveTextContent("Hacer admin");

    await user.click(toggleBtn);

    expect(window.confirm).toHaveBeenCalledWith(
      '¿dar permisos de admin a "Ana Gomez"?'
    );
    expect(put).toHaveBeenCalledWith("/users/1/role", { role: "ADMIN" });

    await waitFor(() => {
      expect(screen.getByTestId("row-role-btn")).toHaveTextContent("Quitar admin");
    });
  });

  it("makes no PUT request when the confirmation is rejected", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    await user.click(toggleBtn);

    expect(window.confirm).toHaveBeenCalled();
    expect(put).not.toHaveBeenCalled();
    expect(toggleBtn).toHaveTextContent("Hacer admin");
  });

  it("surfaces the request error via alert and keeps the row unchanged when PUT rejects", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    put.mockRejectedValue(new Error("No autorizado"));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    await user.click(toggleBtn);

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith("No autorizado");
    });
    // SUSPICIOUS: AdminUsers surfaces this failure via window.alert, while
    // AdminLodgings (same PR) shows an inline error string instead — the two
    // Admin CRUD screens use different, inconsistent error UX. Asserted as-is
    // per spec Risks; not unified here (no production change in this change).
    expect(toggleBtn).toHaveTextContent("Hacer admin");
  });

  it("disables the role button for the row matching the current authenticated user", async () => {
    get.mockResolvedValue([
      userFixture({ id: 1, email: "admin@example.com", role: "ADMIN" }),
      userFixture({ id: 2, email: "ana@example.com", role: "USER" }),
    ]);
    renderAdminUsers({ authValue: makeAuthValue({ user: { ...makeAuthValue().user, email: "admin@example.com" } }) });

    await screen.findByText("admin@example.com");

    const ownRow = screen.getByTestId("row-1");
    const otherRow = screen.getByTestId("row-2");
    expect(ownRow.querySelector("[data-testid='row-role-btn']")).toBeDisabled();
    expect(otherRow.querySelector("[data-testid='row-role-btn']")).not.toBeDisabled();
  });
});
