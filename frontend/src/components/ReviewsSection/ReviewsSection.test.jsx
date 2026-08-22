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

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

// Lets a test control the eligibility response independently from the
// ratings response, and lets ratings resolve differently across successive
// calls (e.g. before/after a successful submission refresh).
function makeGetMock({
  eligibility = Promise.resolve({ eligible: true, reason: "ELIGIBLE" }),
  ratingsSequence = [ratingsFixture],
} = {}) {
  let ratingsCallIndex = 0;
  return (endpoint) => {
    if (endpoint.endsWith("/eligibility")) return eligibility;
    const entry =
      ratingsSequence[Math.min(ratingsCallIndex, ratingsSequence.length - 1)];
    ratingsCallIndex += 1;
    return Promise.resolve(entry);
  };
}

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

describe("ReviewsSection - anonymous visitor", () => {
  it("does not request eligibility and shows no submission form", async () => {
    get.mockResolvedValue(ratingsFixture);
    render(<ReviewsSection lodgingId="1" user={null} />);

    await screen.findByText("4.5");

    expect(get).not.toHaveBeenCalledWith(expect.stringContaining("eligibility"));
    expect(screen.queryByText("Dejá tu reseña")).not.toBeInTheDocument();
  });
});

describe("ReviewsSection - eligibility loading", () => {
  it("shows an accessible loading state and no submission form while eligibility is pending", async () => {
    const eligibilityDeferred = deferred();
    get.mockImplementation(makeGetMock({ eligibility: eligibilityDeferred.promise }));
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    expect(screen.getByRole("status")).toHaveTextContent(/comprobando/i);
    expect(screen.queryByRole("button", { name: /estrella/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Enviar reseña" })).not.toBeInTheDocument();

    eligibilityDeferred.resolve({ eligible: true, reason: "ELIGIBLE" });
    expect(
      await screen.findByRole("button", { name: "Enviar reseña" }),
    ).toBeInTheDocument();
  });
});

describe("ReviewsSection - ineligible user", () => {
  it("shows the completed-stay explanation and no submission form", async () => {
    get.mockImplementation(
      makeGetMock({
        eligibility: Promise.resolve({
          eligible: false,
          reason: "COMPLETED_STAY_REQUIRED",
        }),
      }),
    );
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    expect(
      await screen.findByText(/no podés dejar una reseña/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Enviar reseña" }),
    ).not.toBeInTheDocument();
  });
});

describe("ReviewsSection - eligibility failure", () => {
  it("shows an accessible error with retry and becomes eligible after a successful retry", async () => {
    let eligibilityCalls = 0;
    get.mockImplementation((endpoint) => {
      if (endpoint.endsWith("/eligibility")) {
        eligibilityCalls += 1;
        return eligibilityCalls === 1
          ? Promise.reject(new Error("network"))
          : Promise.resolve({ eligible: true, reason: "ELIGIBLE" });
      }
      return Promise.resolve(ratingsFixture);
    });
    const user = userEvent.setup();
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(/no pudimos comprobar/i);

    await user.click(within(alert).getByRole("button", { name: "Reintentar" }));

    expect(
      await screen.findByRole("button", { name: "Enviar reseña" }),
    ).toBeInTheDocument();
    expect(eligibilityCalls).toBe(2);
  });
});

describe("ReviewsSection - eligible submission form", () => {
  it("renders five accessible, individually-named score controls and a disabled submit until a score is picked", async () => {
    get.mockImplementation(makeGetMock());
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");

    const submit = await screen.findByRole("button", { name: "Enviar reseña" });
    expect(submit).toBeDisabled();

    for (const label of [
      "1 estrella",
      "2 estrellas",
      "3 estrellas",
      "4 estrellas",
      "5 estrellas",
    ]) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
  });
});

describe("ReviewsSection - successful submission", () => {
  it("refreshes the average/count and clears the form after a successful submit", async () => {
    const refreshedFixture = {
      average: 4.7,
      count: 3,
      ratings: [
        ...ratingsFixture.ratings,
        { id: 3, userName: "Test User", score: 5, comment: "Genial", createdAt: "2026-03-01" },
      ],
    };
    get.mockImplementation(
      makeGetMock({ ratingsSequence: [ratingsFixture, refreshedFixture] }),
    );
    post.mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");
    await user.click(await screen.findByRole("button", { name: "1 estrella" }));

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
  it("shows an inline non-success message, keeps the score and comment, and never uses window.alert", async () => {
    get.mockImplementation(makeGetMock());
    post.mockRejectedValue(new Error("Ya calificaste este alojamiento"));
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});
    const user = userEvent.setup();
    render(<ReviewsSection lodgingId="1" user={loggedUser} />);

    await screen.findByText("4.5");
    await user.click(await screen.findByRole("button", { name: "2 estrellas" }));

    const textarea = screen.getByPlaceholderText("Contá tu experiencia...");
    await user.type(textarea, "Mal");

    await user.click(screen.getByRole("button", { name: "Enviar reseña" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Ya calificaste este alojamiento",
    );
    expect(textarea.value).toBe("Mal");
    expect(screen.getByRole("button", { name: "2 estrellas" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(alertSpy).not.toHaveBeenCalled();

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    alertSpy.mockRestore();
  });
});
