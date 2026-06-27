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
  it("shows the in-app confirm dialog, calls PUT /users/:id/role on accept, and updates the row", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    put.mockResolvedValue(userFixture({ id: 1, role: "ADMIN" }));
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    expect(toggleBtn).toHaveTextContent("Hacer admin");

    await user.click(toggleBtn);

    // Uses the in-app ConfirmDialog (testId confirm-role-toggle), NOT
    // window.confirm — same mechanism as AdminLodgings/AdminCategories.
    expect(screen.getByTestId("confirm-role-toggle")).toHaveTextContent(
      '¿dar permisos de admin a "Ana Gomez"?'
    );
    expect(put).not.toHaveBeenCalled();

    await user.click(screen.getByTestId("confirm-role-toggle-yes"));

    expect(put).toHaveBeenCalledWith("/users/1/role", { role: "ADMIN" });

    await waitFor(() => {
      expect(screen.getByTestId("row-role-btn")).toHaveTextContent("Quitar admin");
    });
    expect(screen.queryByTestId("confirm-role-toggle")).not.toBeInTheDocument();
  });

  it("makes no PUT request when the confirmation is dismissed", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    await user.click(toggleBtn);

    expect(screen.getByTestId("confirm-role-toggle")).toBeInTheDocument();
    await user.click(screen.getByTestId("confirm-role-toggle-no"));

    expect(screen.queryByTestId("confirm-role-toggle")).not.toBeInTheDocument();
    expect(put).not.toHaveBeenCalled();
    expect(toggleBtn).toHaveTextContent("Hacer admin");
  });

  it("keeps the dialog message scoped to the most recently clicked user when reopened for a different row", async () => {
    get.mockResolvedValue([
      userFixture({ id: 1, firstName: "Ana", lastName: "Gomez", role: "USER" }),
      userFixture({ id: 2, firstName: "Beto", lastName: "Diaz", email: "beto@example.com", role: "ADMIN" }),
    ]);
    const user = userEvent.setup();
    renderAdminUsers();

    await screen.findByText("ana@example.com");

    await user.click(screen.getByTestId("row-1").querySelector("[data-testid='row-role-btn']"));
    expect(screen.getByTestId("confirm-role-toggle")).toHaveTextContent(
      '¿dar permisos de admin a "Ana Gomez"?'
    );
    await user.click(screen.getByTestId("confirm-role-toggle-no"));

    await user.click(screen.getByTestId("row-2").querySelector("[data-testid='row-role-btn']"));
    expect(screen.getByTestId("confirm-role-toggle")).toHaveTextContent(
      '¿quitar permisos de admin a "Beto Diaz"?'
    );
    expect(put).not.toHaveBeenCalled();
  });

  it("surfaces the request error via alert and keeps the row unchanged when PUT rejects", async () => {
    get.mockResolvedValue([userFixture({ id: 1, role: "USER" })]);
    put.mockRejectedValue(new Error("No autorizado"));
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    renderAdminUsers();

    const toggleBtn = await screen.findByTestId("row-role-btn");
    await user.click(toggleBtn);
    await user.click(screen.getByTestId("confirm-role-toggle-yes"));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith("No autorizado");
    });
    // SUSPICIOUS: AdminUsers surfaces this failure via window.alert, while
    // AdminLodgings (same PR) shows an inline error string instead — the two
    // Admin CRUD screens use different, inconsistent error UX. Asserted as-is
    // per spec Risks; not unified here (no production change in this change).
    expect(toggleBtn).toHaveTextContent("Hacer admin");
    expect(screen.queryByTestId("confirm-role-toggle")).not.toBeInTheDocument();
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
