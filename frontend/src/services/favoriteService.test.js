import { getFavorites, addFavorite, removeFavorite } from "./favoriteService";
import { get, post, del } from "./api";

vi.mock("./api");

describe("favoriteService", () => {
  it("getFavorites calls get with /favorites", async () => {
    get.mockResolvedValue([{ id: 1 }]);

    const result = await getFavorites();

    expect(get).toHaveBeenCalledWith("/favorites");
    expect(result).toEqual([{ id: 1 }]);
  });

  it("addFavorite calls post with /favorites/{id}", async () => {
    post.mockResolvedValue(null);

    await addFavorite(5);

    expect(post).toHaveBeenCalledWith("/favorites/5");
  });

  it("removeFavorite calls del with /favorites/{id}", async () => {
    del.mockResolvedValue(null);

    await removeFavorite(5);

    expect(del).toHaveBeenCalledWith("/favorites/5");
  });
});
