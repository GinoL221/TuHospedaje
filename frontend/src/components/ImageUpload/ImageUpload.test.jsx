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
  it("shows a visible error message and does not call onUrlsChange when the response is not ok", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ error: "Upload failed" }),
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
