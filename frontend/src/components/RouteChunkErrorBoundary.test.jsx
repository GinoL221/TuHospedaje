import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RouteChunkErrorBoundary from "./RouteChunkErrorBoundary";

function BrokenRoute() {
  throw new Error("chunk failed");
}

describe("RouteChunkErrorBoundary", () => {
  let reload;

  beforeEach(() => {
    reload = vi.fn();
    vi.stubGlobal("location", { reload });
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("shows manual recovery without reloading automatically", () => {
    render(
      <RouteChunkErrorBoundary resetKey="/search">
        <BrokenRoute />
      </RouteChunkErrorBoundary>
    );

    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
  });

  it("requests exactly one reload per recovery activation", async () => {
    const user = userEvent.setup();
    render(
      <RouteChunkErrorBoundary resetKey="/search">
        <BrokenRoute />
      </RouteChunkErrorBoundary>
    );

    await user.click(screen.getByRole("button", { name: "Recargar página" }));
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it("clears the failure when the route reset key changes", () => {
    const { rerender } = render(
      <RouteChunkErrorBoundary resetKey="/search">
        <BrokenRoute />
      </RouteChunkErrorBoundary>
    );
    expect(screen.getByRole("button", { name: "Recargar página" })).toBeInTheDocument();

    rerender(
      <RouteChunkErrorBoundary resetKey="/">
        <p>Route recovered</p>
      </RouteChunkErrorBoundary>
    );
    expect(screen.getByText("Route recovered")).toBeInTheDocument();
  });
});
