import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ImageUpload from "./ImageUpload";

function makeFile(name = "photo.png") {
  return new File(["fake-image-content"], name, { type: "image/png" });
}

beforeEach(() => {
  document.cookie =
    "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie =
    "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
});

describe("ImageUpload - successful upload", () => {
  it("calls onUrlsChange with the new image url when the upload succeeds", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ url: "https://example.com/uploaded.png" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    await waitFor(() => {
      expect(onUrlsChange).toHaveBeenCalledWith(["https://example.com/uploaded.png"]);
    });
  });

  it("sends credentials: 'include' and the X-XSRF-TOKEN header, without an Authorization header", async () => {
    document.cookie = "XSRF-TOKEN=csrf-abc; path=/";
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ url: "https://example.com/uploaded.png" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalled();
    });

    const [, config] = fetchMock.mock.calls[0];
    expect(config.credentials).toBe("include");
    expect(config.headers).toMatchObject({ "X-XSRF-TOKEN": "csrf-abc" });
    expect(config.headers).not.toHaveProperty("Authorization");
  });
});

describe("ImageUpload - failed upload", () => {
  it("shows the backend error message for a 400 response and does not update urls", async () => {
    const errorMessage = "Only JPEG, PNG, WebP and GIF images are allowed.";
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ error: errorMessage }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    await userEvent.upload(document.getElementById("imageUpload"), makeFile());

    expect(await screen.findByText(errorMessage)).toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  it("shows the backend error message for a 413 response and does not update urls", async () => {
    const errorMessage = "The uploaded file is too large.";
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 413,
      json: async () => ({ error: errorMessage }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    await userEvent.upload(document.getElementById("imageUpload"), makeFile());

    expect(await screen.findByText(errorMessage)).toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  it("shows a visible error message and does not call onUrlsChange when the response is not ok", async () => {
    const responseBody = vi.fn();
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: responseBody,
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    expect(await screen.findByText(/no se pudo subir/i)).toBeInTheDocument();
    expect(responseBody).not.toHaveBeenCalled();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  it.each([
    ["a malformed body", vi.fn().mockRejectedValue(new SyntaxError("invalid JSON"))],
    ["an absent error field", vi.fn().mockResolvedValue({})],
    ["an empty error field", vi.fn().mockResolvedValue({ error: "   " })],
  ])("shows the generic error for a 400 response with %s", async (_description, responseBody) => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: responseBody,
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    await userEvent.upload(document.getElementById("imageUpload"), makeFile());

    expect(await screen.findByText(/no se pudo subir/i)).toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  it("shows the generic Spanish error message when fetch itself rejects (network error), not the raw exception text", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("network down"));
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    expect(await screen.findByText(/no se pudo subir/i)).toBeInTheDocument();
    expect(screen.queryByText("network down")).not.toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });
});
