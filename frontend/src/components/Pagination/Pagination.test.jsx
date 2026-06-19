import { render, screen, userEvent } from "../../test/test-utils";
import Pagination from "./Pagination";

describe("Pagination - single page renders nothing", () => {
  it("renders null when totalPages is 1", () => {
    const { container } = render(
      <Pagination page={0} totalPages={1} onPageChange={vi.fn()} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("renders null when totalPages is 0", () => {
    const { container } = render(
      <Pagination page={0} totalPages={0} onPageChange={vi.fn()} />
    );

    expect(container).toBeEmptyDOMElement();
  });
});

describe("Pagination - page indicator", () => {
  it("renders the current page and total pages 1-indexed", () => {
    render(<Pagination page={2} totalPages={5} onPageChange={vi.fn()} />);

    expect(screen.getByText("Página 3 de 5")).toBeInTheDocument();
  });
});

describe("Pagination - first page disables backward controls", () => {
  it('disables "Primera" and "Anterior" on the first page', () => {
    render(<Pagination page={0} totalPages={5} onPageChange={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Primera" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Siguiente" })).not.toBeDisabled();
    expect(screen.getByRole("button", { name: "Última" })).not.toBeDisabled();
  });
});

describe("Pagination - last page disables forward controls", () => {
  it('disables "Siguiente" and "Última" on the last page', () => {
    render(<Pagination page={4} totalPages={5} onPageChange={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Última" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Primera" })).not.toBeDisabled();
    expect(screen.getByRole("button", { name: "Anterior" })).not.toBeDisabled();
  });
});

describe("Pagination - control clicks call onPageChange with the target page", () => {
  it('calls onPageChange(1) when "Anterior" is clicked from page 2 (index)', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={2} totalPages={5} onPageChange={onPageChange} />);

    await user.click(screen.getByRole("button", { name: "Anterior" }));

    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('calls onPageChange(3) when "Siguiente" is clicked from page 2 (index)', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={2} totalPages={5} onPageChange={onPageChange} />);

    await user.click(screen.getByRole("button", { name: "Siguiente" }));

    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  it('calls onPageChange(0) when "Primera" is clicked', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={3} totalPages={5} onPageChange={onPageChange} />);

    await user.click(screen.getByRole("button", { name: "Primera" }));

    expect(onPageChange).toHaveBeenCalledWith(0);
  });

  it('calls onPageChange(totalPages - 1) when "Última" is clicked', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={1} totalPages={5} onPageChange={onPageChange} />);

    await user.click(screen.getByRole("button", { name: "Última" }));

    expect(onPageChange).toHaveBeenCalledWith(4);
  });
});

describe("Pagination - custom className", () => {
  it("applies the default className when none is provided", () => {
    const { container } = render(
      <Pagination page={0} totalPages={5} onPageChange={vi.fn()} />
    );

    expect(container.querySelector(".pagination")).toBeInTheDocument();
  });

  it("applies a custom className when provided", () => {
    const { container } = render(
      <Pagination page={0} totalPages={5} onPageChange={vi.fn()} className="search-pagination" />
    );

    expect(container.querySelector(".search-pagination")).toBeInTheDocument();
  });
});
