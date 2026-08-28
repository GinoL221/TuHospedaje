import { customRender, screen, userEvent, makeAuthValue, fireEvent } from "../../test/test-utils";
import { readFileSync } from "node:fs";
import ProductCard from "./ProductCard";
import { post, del } from "../../services/api";

vi.mock("../../services/api");

const lodgingFixture = {
  id: 1,
  name: "Cabaña del Lago",
  city: "Bariloche",
  country: "Argentina",
  description: "Una cabaña con vista al lago.",
  imageUrls: ["https://example.com/img.jpg"],
  averageRating: 0,
};

function renderProductCard(props = {}, { authValue } = {}) {
  return customRender(<ProductCard lodging={lodgingFixture} {...props} />, { authValue });
}

describe("ProductCard - rendering lodging data", () => {
	it("shows explicit zero average and rating count for an unrated lodging", () => {
		renderProductCard({ lodging: { ...lodgingFixture, ratingCount: 0 } });

		expect(screen.getByText("0.0 (0 opiniones)")).toBeInTheDocument();
	});

  it("renders the lodging name, location, description and image", () => {
    renderProductCard();

    expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
    expect(screen.getByText("Bariloche, Argentina")).toBeInTheDocument();
    expect(screen.getByText("Una cabaña con vista al lago.")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Cabaña del Lago" })).toHaveAttribute(
      "src",
      "https://example.com/img.jpg"
    );
  });

  it("links to the lodging detail page", () => {
    renderProductCard();

    expect(screen.getByRole("link")).toHaveAttribute("href", "/lodgings/1");
  });
});

describe("ProductCard - broken image fallback", () => {
  it("falls back to the placeholder image when the lodging image fails to load", () => {
    renderProductCard();

    const img = screen.getByRole("img", { name: "Cabaña del Lago" });
    fireEvent.error(img);

    expect(img).toHaveAttribute("src", "https://placehold.co/400x300?text=Sin+imagen");
  });
});

describe("ProductCard - favorite button visibility", () => {
  it("hides the favorite button for anonymous users even when showFavoriteButton is true", () => {
    renderProductCard({ showFavoriteButton: true }, { authValue: null });

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("shows the favorite button for logged-in users", () => {
    renderProductCard({}, { authValue: makeAuthValue() });

    expect(
      screen.getByRole("button", { name: "Agregar a favoritos" })
    ).toBeInTheDocument();
  });
});

describe("ProductCard - favorite toggle success", () => {
  it("optimistically marks the card as favorite and persists via POST on success", async () => {
    post.mockResolvedValue(undefined);
    const onFavoriteToggle = vi.fn();
    const user = userEvent.setup();
    renderProductCard(
      { defaultFavorite: false, onFavoriteToggle },
      { authValue: makeAuthValue() }
    );

    await user.click(screen.getByRole("button", { name: "Agregar a favoritos" }));

    expect(post).toHaveBeenCalledWith("/favorites/1");
    expect(onFavoriteToggle).toHaveBeenCalledWith(1, true);
    expect(
      await screen.findByRole("button", { name: "Quitar de favoritos" })
    ).toBeInTheDocument();
  });

  it("calls DELETE when un-favoriting an already-favorite card", async () => {
    del.mockResolvedValue(undefined);
    const onFavoriteToggle = vi.fn();
    const user = userEvent.setup();
    renderProductCard(
      { defaultFavorite: true, onFavoriteToggle },
      { authValue: makeAuthValue() }
    );

    await user.click(screen.getByRole("button", { name: "Quitar de favoritos" }));

    expect(del).toHaveBeenCalledWith("/favorites/1");
    expect(onFavoriteToggle).toHaveBeenCalledWith(1, false);
  });
});

describe("ProductCard - favorite toggle rollback on failure", () => {
  it("reverts isFavorite and re-notifies the parent when the request fails", async () => {
    post.mockRejectedValue(new Error("network error"));
    const onFavoriteToggle = vi.fn();
    const user = userEvent.setup();
    // Silence the expected console.error from ProductCard's catch block —
    // this is existing production behavior (logs instead of surfacing UI
    // error state), characterized as-is per spec Risks policy.
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    renderProductCard(
      { defaultFavorite: false, onFavoriteToggle },
      { authValue: makeAuthValue() }
    );

    await user.click(screen.getByRole("button", { name: "Agregar a favoritos" }));

    const button = await screen.findByRole("button", { name: "Agregar a favoritos" });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
    expect(onFavoriteToggle).toHaveBeenNthCalledWith(1, 1, true);
    expect(onFavoriteToggle).toHaveBeenNthCalledWith(2, 1, false);

    consoleErrorSpy.mockRestore();
  });
});

describe("ProductCard - favorite toggle race condition guard", () => {
  it("ignores a second click while the first request is still in flight", async () => {
    let resolvePost;
    post.mockReturnValue(
      new Promise((resolve) => {
        resolvePost = resolve;
      })
    );
    const onFavoriteToggle = vi.fn();
    const user = userEvent.setup();
    renderProductCard(
      { defaultFavorite: false, onFavoriteToggle },
      { authValue: makeAuthValue() }
    );

    const button = screen.getByRole("button", { name: "Agregar a favoritos" });
    await user.click(button);
    await user.click(button);

    expect(post).toHaveBeenCalledTimes(1);
    expect(button).toBeDisabled();

    resolvePost(undefined);
    await screen.findByRole("button", { name: "Quitar de favoritos" });

    expect(
      screen.getByRole("button", { name: "Quitar de favoritos" })
    ).not.toBeDisabled();
  });
});

describe("ProductCard - useAuth integration", () => {
  it("reads the current user from useAuth via AuthContext to decide button visibility", () => {
    const authValue = makeAuthValue({ user: null });
    renderProductCard({}, { authValue });

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
	});
});

describe("ProductCard - visitor accessibility contract", () => {
	it("reports an idle favorite action as not busy", () => {
		renderProductCard({}, { authValue: makeAuthValue() });

		expect(screen.getByRole("button", { name: "Agregar a favoritos" })).toHaveAttribute(
			"aria-busy",
			"false",
		);
	});

	it("announces pending favorite persistence without blocking card navigation", async () => {
		post.mockReturnValue(new Promise(() => {}));
		const user = userEvent.setup();
		renderProductCard({}, { authValue: makeAuthValue() });

		await user.click(screen.getByRole("button", { name: "Agregar a favoritos" }));

		expect(screen.getByRole("button", { name: "Quitar de favoritos" })).toHaveAttribute(
			"aria-busy",
			"true",
		);
		expect(screen.getByRole("link")).toHaveAttribute("href", "/lodgings/1");
	});

	it("defines tokenized focus, touch-target, and narrow-screen card rules", () => {
		const css = readFileSync("src/components/ProductCard/ProductCard.css", "utf8");

		expect(css).toMatch(/--product-card-surface:/);
		expect(css).toMatch(/\.hotel-card-link:focus-visible/);
		expect(css).toMatch(/\.fav-btn[\s\S]*?min-width: 44px/);
		expect(css).toMatch(/@media \(max-width: 360px\)/);
	});

	it("pins the mobile title minimum and favorite hit target", () => {
		const css = readFileSync("src/components/ProductCard/ProductCard.css", "utf8");
		const narrowRules = css.match(
			/@media \(max-width: 360px\)\s*\{([\s\S]*)\}\s*$/,
		)?.[1] ?? "";

		expect(narrowRules).toMatch(
			/\.hotel-card-body h3[\s\S]*?font-size:\s*max\(16px,\s*var\(--type-body-size\)\)/,
		);
		expect(narrowRules).toMatch(/\.fav-btn[\s\S]*?width:\s*44px;[\s\S]*?height:\s*44px;/);
	});
});
