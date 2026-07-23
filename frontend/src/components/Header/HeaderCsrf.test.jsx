import { render, screen, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Header from "./Header";
import { useAuth } from "../../hooks/useAuth";

vi.mock("../../hooks/useAuth");

describe("Header logout failure", () => {
  it("keeps the authenticated user visible and shows the logout error", async () => {
    useAuth.mockReturnValue({
      user: { firstName: "Test", role: "USER", imageUrl: null },
      logout: vi.fn().mockRejectedValue(new Error("Logout rejected")),
      logoutError: "Logout rejected",
    });

    render(<MemoryRouter><Header /></MemoryRouter>);
    await act(async () => screen.getByRole("button", { name: "Cerrar sesión" }).click());

    expect(screen.getByText("Test")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Logout rejected");
  });
});
