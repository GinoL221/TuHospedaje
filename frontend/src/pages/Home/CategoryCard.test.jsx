import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CategoryCard from "./CategoryCard";

const baseCategory = { id: 1, name: "Cabaña", icon: "tree-pine", description: null };

describe("CategoryCard - render", () => {
  it("renders the category name", () => {
    render(<CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />);
    expect(screen.getByText("Cabaña")).toBeInTheDocument();
  });

  it("renders as a semantic button with aria-pressed reflecting an active category", () => {
    render(<CategoryCard category={baseCategory} isActive={true} onClick={vi.fn()} />);
    const button = screen.getByRole("button", { name: /Cabaña/ });
    expect(button.tagName).toBe("BUTTON");
    expect(button).toHaveAttribute("aria-pressed", "true");
    expect(button).toHaveClass("active");
  });

  it("reflects an inactive category via aria-pressed and no active class", () => {
    render(<CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />);
    const button = screen.getByRole("button", { name: /Cabaña/ });
    expect(button).toHaveAttribute("aria-pressed", "false");
    expect(button).not.toHaveClass("active");
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

  it("calls onClick when Space is pressed while focused", async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(<CategoryCard category={baseCategory} isActive={false} onClick={onClick} />);
    screen.getByRole("button", { name: /Cabaña/ }).focus();
    await user.keyboard(" ");
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});

describe("CategoryCard - icon", () => {
  it("renders the Lucide icon mapped from category.icon", () => {
    const { container } = render(
      <CategoryCard category={baseCategory} isActive={false} onClick={vi.fn()} />
    );
    const icon = container.querySelector("svg.lucide-tree-pine");
    expect(icon).toBeInTheDocument();
    expect(icon).toHaveAttribute("width", "24");
    expect(icon).toHaveAttribute("aria-hidden", "true");
  });

  it("renders a placeholder mark when the icon name is unknown", () => {
    render(
      <CategoryCard
        category={{ ...baseCategory, icon: "not-a-real-icon" }}
        isActive={false}
        onClick={vi.fn()}
      />
    );
    expect(screen.getByText("?")).toBeInTheDocument();
  });

  it("does not render a representative photo even if imageUrl is present", () => {
    render(
      <CategoryCard
        category={{ ...baseCategory, imageUrl: "https://img.example.com/cabana.jpg" }}
        isActive={false}
        onClick={vi.fn()}
      />
    );
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });
});
