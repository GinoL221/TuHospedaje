import { act, render, screen } from "@testing-library/react";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import RouteLoadingFallback from "./RouteLoadingFallback";
import "../App.css";

describe("RouteLoadingFallback", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("waits 150 ms before showing accessible feedback", () => {
    render(<RouteLoadingFallback />);

    act(() => vi.advanceTimersByTime(149));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByRole("status")).toHaveTextContent("Cargando página…");
    expect(screen.getByTestId("route-loading-spinner")).toHaveClass("route-loading__spinner");
  });

  it("cleans up its delay when a fast route unmounts", () => {
    const { unmount } = render(<RouteLoadingFallback />);
    unmount();

    act(() => vi.runAllTimers());
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("disappears immediately when pending content resolves", () => {
    const { unmount } = render(<RouteLoadingFallback />);
    act(() => vi.advanceTimersByTime(150));
    expect(screen.getByRole("status")).toBeInTheDocument();

    unmount();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("centers the primary indicator and disables motion when requested", () => {
    const css = readFileSync(resolve(process.cwd(), "src/App.css"), "utf8");

    expect(css).toMatch(/\.route-loading\s*{[^}]*color:\s*var\(--primary\)/s);
    expect(css).toMatch(/\.route-loading,[\s\S]*justify-content:\s*center/);
    expect(css).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.route-loading__spinner\s*{[^}]*animation:\s*none/s
    );
  });
});
