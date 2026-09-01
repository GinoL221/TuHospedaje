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
  it("keeps the generic message on a 5xx and does not call onUrlsChange", async () => {
    // A server fault is not the user's to fix, and its body carries the
    // deliberately non-disclosing "Internal server error." — so the generic
    // Spanish message stays, rather than echoing backend internals.
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ error: "Internal server error." }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    expect(await screen.findByText(/no se pudo subir/i)).toBeInTheDocument();
    expect(screen.queryByText("Internal server error.")).not.toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  /**
   * The backend rejects a wrong content type with a localized, actionable 400.
   * Collapsing it into "Intentá de nuevo" tells the admin to repeat the exact
   * action that just failed — the message only helps if it survives to the UI.
   */
  it("shows the backend message on a 400 so the admin learns which formats are allowed", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ error: "Solo se permiten imágenes JPEG, PNG, WebP y GIF.", status: 400 }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    expect(
      await screen.findByText("Solo se permiten imágenes JPEG, PNG, WebP y GIF.")
    ).toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  it("shows the backend message on a 413 so the admin learns the file is too large", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 413,
      json: async () => ({ error: "La imagen supera el tamaño máximo permitido.", status: 413 }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

    expect(
      await screen.findByText("La imagen supera el tamaño máximo permitido.")
    ).toBeInTheDocument();
    expect(onUrlsChange).not.toHaveBeenCalled();
  });

  /** A 4xx with an unreadable or bodyless response must still say something useful. */
  it("falls back to the generic message when a 4xx body cannot be parsed", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 413,
      json: async () => {
        throw new Error("Unexpected end of JSON input");
      },
    });
    vi.stubGlobal("fetch", fetchMock);

    const onUrlsChange = vi.fn();
    render(<ImageUpload urls={[]} onUrlsChange={onUrlsChange} />);

    const input = document.getElementById("imageUpload");
    await userEvent.upload(input, makeFile());

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
