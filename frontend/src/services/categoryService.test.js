import { getCategories } from "./categoryService";
import { get } from "./api";

vi.mock("./api");

describe("categoryService - getCategories", () => {
  it("calls get with /categories", async () => {
    get.mockResolvedValue([{ id: 1, name: "Cabaña" }]);

    await getCategories();

    expect(get).toHaveBeenCalledWith("/categories");
  });

  it("resolves with the response returned by get", async () => {
    const categories = [{ id: 1, name: "Cabaña" }, { id: 2, name: "Hotel" }];
    get.mockResolvedValue(categories);

    const result = await getCategories();

    expect(result).toEqual(categories);
  });
});
