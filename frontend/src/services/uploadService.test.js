import { uploadImage } from "./uploadService";
import { postMultipart } from "./api";

vi.mock("./api");

describe("uploadService - uploadImage", () => {
  it("sends the selected file to the upload endpoint as multipart form data", async () => {
    const file = new File(["first-image"], "first.png", { type: "image/png" });
    postMultipart.mockResolvedValue({ url: "https://example.com/first.png" });

    await uploadImage(file);

    expect(postMultipart).toHaveBeenCalledWith("/upload", expect.any(FormData));
    const [, formData] = postMultipart.mock.calls[0];
    expect(formData.get("file")).toBe(file);
  });

  it("returns the upload response for another selected image", async () => {
    const file = new File(["second-image"], "second.webp", {
      type: "image/webp",
    });
    const response = { url: "https://example.com/second.webp" };
    postMultipart.mockResolvedValue(response);

    const result = await uploadImage(file);

    expect(postMultipart).toHaveBeenCalledWith("/upload", expect.any(FormData));
    expect(result).toEqual(response);
  });
});
