import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CategoryCard from "./CategoryCard";

vi.mock("../../utils/iconMap", () => ({
  ICON_MAP: { wifi: "wifi-icon" },
}));

vi.mock("../../components/Icons/Icon", () => ({
  default: ({ name }) => <span data-testid={`icon-${name}`} />,
}));

const baseCategory = { id: 1, name: "Cabaña", icon: null, description: null };

describe("CategoryCard - render", () => {
  it("renders the category name", () => {
    render(<CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />);
    expect(screen.getByText("Cabaña")).toBeInTheDocument();
  });

  it("adds the active class when isActive is true", () => {
    const { container } = render(
      <CategoryCard category={baseCategory} isActive={true} onClick={vi.fn()} />
    );
    expect(container.firstChild).toHaveClass("active");
  });

  it("does not add the active class when isActive is false", () => {
    const { container } = render(
      <CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />
    );
    expect(container.firstChild).not.toHaveClass("active");
  });
});

describe("CategoryCard - description", () => {
  it("renders the description when present", () => {
    render(
      <CategoryCard
        category={{ ...baseCategory, description: "Alojamiento rústico" }}
        isActive={false}
        onClick={vi.fn()}
      />
    );
    expect(screen.getByText("Alojamiento rústico")).toBeInTheDocument();
  });

  it("does not render the description element when absent", () => {
    const { container } = render(
      <CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />
    );
    expect(container.querySelector(".category-description")).not.toBeInTheDocument();
  });
});

describe("CategoryCard - interactions", () => {
  it("calls onClick when clicked", async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(<CategoryCard category={baseCategory} isActive={false} onClick={onClick} />);
    await user.click(screen.getByRole("button", { name: /Cabaña/ }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("calls onClick when Enter is pressed while focused", async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(<CategoryCard category={baseCategory} isActive={false} onClick={onClick} />);
    screen.getByRole("button", { name: /Cabaña/ }).focus();
    await user.keyboard("{Enter}");
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});

describe("CategoryCard - icon", () => {
  it("renders the mapped Icon component when category.icon is a known ICON_MAP key", () => {
    render(
      <CategoryCard
        category={{ ...baseCategory, icon: "wifi" }}
        isActive={false}
        onClick={vi.fn()}
      />
    );
    expect(screen.getByTestId("icon-wifi")).toBeInTheDocument();
  });

  it("falls back gracefully when category.icon is not in ICON_MAP", () => {
    render(
      <CategoryCard
        category={{ ...baseCategory, icon: "unknown-icon" }}
        isActive={false}
        onClick={vi.fn()}
      />
    );
    expect(screen.queryByTestId("icon-unknown-icon")).not.toBeInTheDocument();
    expect(screen.getByText("Cabaña")).toBeInTheDocument();
  });
});
