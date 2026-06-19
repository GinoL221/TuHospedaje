import { render, screen, userEvent, waitFor, within } from "../../test/test-utils";
import ReviewsSection from "./ReviewsSection";
import { get, post } from "../../services/api";

vi.mock("../../services/api");

const ratingsFixture = {
  average: 4.5,
  count: 2,
  ratings: [
    { id: 1, userName: "Ana", score: 5, comment: "Excelente lugar", createdAt: "2026-01-01" },
    { id: 2, userName: "Beto", score: 4, comment: "", createdAt: "2026-02-01" },
  ],
};

const loggedUser = { firstName: "Test", lastName: "User" };

describe("ReviewsSection - reviews listing", () => {
  it("renders the average score, review count and each review item", async () => {
    get.mockResolvedValue(ratingsFixture);
    render(<ReviewsSection lodgingId="1" user={null} />);

    expect(get).toHaveBeenCalledWith("/ratings/lodging/1");
    expect(await screen.findByText("4.5")).toBeInTheDocument();
    expect(screen.getByText("(2 reseñas)")).toBeInTheDocument();
    expect(screen.getByText("Ana")).toBeInTheDocument();
    expect(screen.getByText("Excelente lugar")).toBeInTheDocument();
    expect(screen.getByText("Beto")).toBeInTheDocument();
  });
});

describe("ReviewsSection - empty state", () => {
  it("renders zero average and no review items when there are no ratings yet", async () => {
    get.mockResolvedValue({ average: 0, count: 0, ratings: [] });
    render(<ReviewsSection lodgingId="1" user={null} />);

    expect(await screen.findByText("0.0")).toBeInTheDocument();
    expect(screen.getByText("(0 reseñas)")).toBeInTheDocument();
  });
});

describe("ReviewsSection - review form visibility", () => {
  it("hides the review form for anonymous users", async () => {
    get.mockResolvedValue(ratingsFixture);
    render(<ReviewsSection lodgingId="1" user={null} />);

    await screen.findByText("4.5");

    expect(screen.queryByText("Dejá tu reseña")).not.toBeInTheDocument();
  });

  it("shows the review form for logged-in users", async () => {
    get.mockResolvedValue(ratingsFixture);
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    expect(screen.getByText("Dejá tu reseña")).toBeInTheDocument();
  });
});

describe("ReviewsSection - submit gating at score 0", () => {
  it("disables the submit button while no star is selected", async () => {
    get.mockResolvedValue(ratingsFixture);
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    expect(screen.getByRole("button", { name: "Enviar reseña" })).toBeDisabled();
  });
});

describe("ReviewsSection - successful submission", () => {
  it("refreshes the average/count and clears the form after a successful submit", async () => {
    get.mockResolvedValueOnce(ratingsFixture);
    post.mockResolvedValue(undefined);
    const refreshedFixture = {
      average: 4.7,
      count: 3,
      ratings: [...ratingsFixture.ratings, { id: 3, userName: "Test User", score: 5, comment: "Genial", createdAt: "2026-03-01" }],
    };
    get.mockResolvedValueOnce(refreshedFixture);
    const user = userEvent.setup();
    const { container } = render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    const starSelector = container.querySelector(".star-selector");
    const stars = within(starSelector).getAllByText("★");
    await user.click(stars[0]);

    const textarea = screen.getByPlaceholderText("Contá tu experiencia...");
    await user.type(textarea, "Genial");

    await user.click(screen.getByRole("button", { name: "Enviar reseña" }));

    expect(post).toHaveBeenCalledWith("/ratings", {
      lodgingId: "1",
      score: 1,
      comment: "Genial",
    });
    expect(await screen.findByText("4.7")).toBeInTheDocument();
    expect(screen.getByText("(3 reseñas)")).toBeInTheDocument();
    expect(textarea.value).toBe("");
  });
});

describe("ReviewsSection - failed submission", () => {
  it("alerts with the server error message and keeps the form values unchanged", async () => {
    get.mockResolvedValue(ratingsFixture);
    post.mockRejectedValue(new Error("fail"));
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    const { container } = render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    const starSelector = container.querySelector(".star-selector");
    const stars = within(starSelector).getAllByText("★");
    await user.click(stars[1]);

    const textarea = screen.getByPlaceholderText("Contá tu experiencia...");
    await user.type(textarea, "Mal");

    await user.click(screen.getByRole("button", { name: "Enviar reseña" }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith("fail");
    });
    expect(textarea.value).toBe("Mal");

    alertSpy.mockRestore();
  });
});
