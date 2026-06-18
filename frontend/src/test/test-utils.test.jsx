import { useAuth } from "../hooks/useAuth";
import { customRender, screen, makeAuthValue } from "./test-utils";

function AuthConsumer() {
  const { user, token } = useAuth();
  return (
    <div>
      <span data-testid="user">{user ? user.email : "no-user"}</span>
      <span data-testid="token">{token ?? "no-token"}</span>
    </div>
  );
}

describe("test-utils - customRender AuthContext propagation", () => {
  it("provides the default mocked authValue when none is passed", () => {
    customRender(<AuthConsumer />);

    expect(screen.getByTestId("user")).toHaveTextContent("test@example.com");
    expect(screen.getByTestId("token")).toHaveTextContent("fake-token");
  });

  it("provides a custom authValue explicitly passed via the authValue option", () => {
    const customValue = makeAuthValue({
      token: "custom-token",
      user: { firstName: "Custom", lastName: "User", email: "custom@example.com", role: "ADMIN" },
    });

    customRender(<AuthConsumer />, { authValue: customValue });

    expect(screen.getByTestId("user")).toHaveTextContent("custom@example.com");
    expect(screen.getByTestId("token")).toHaveTextContent("custom-token");
  });
});
