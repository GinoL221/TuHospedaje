import { del, get, post, put } from "./api";
import {
  createCategory,
  createFeature,
  createPolicy,
  deleteCategory,
  deleteFeature,
  deleteLodging,
  deletePolicy,
  getAdminLodgings,
  getAdminStats,
  getCategories,
  getFeatures,
  getPolicies,
  getUsers,
  updateCategory,
  updateFeature,
  updatePolicy,
  updateUserRole,
} from "./adminCatalogService";

vi.mock("./api");

describe("adminCatalogService", () => {
  it("routes catalog reads and writes through their established API endpoints", () => {
    const category = { name: "Cabañas" };
    const feature = { name: "WiFi" };
    const policy = { name: "Check-in" };

    getCategories();
    getFeatures();
    getPolicies();
    getUsers();
    createCategory(category);
    updateCategory(1, category);
    deleteCategory(1);
    createFeature(feature);
    updateFeature(2, feature);
    deleteFeature(2);
    createPolicy(policy);
    updatePolicy(3, policy);
    deletePolicy(3);
    updateUserRole(4, "ADMIN");
    deleteLodging(5);

    expect(get).toHaveBeenCalledWith("/categories");
    expect(get).toHaveBeenCalledWith("/features");
    expect(get).toHaveBeenCalledWith("/policies");
    expect(get).toHaveBeenCalledWith("/users");
    expect(post).toHaveBeenCalledWith("/categories", category);
    expect(put).toHaveBeenCalledWith("/categories/1", category);
    expect(del).toHaveBeenCalledWith("/categories/1");
    expect(post).toHaveBeenCalledWith("/features", feature);
    expect(put).toHaveBeenCalledWith("/features/2", feature);
    expect(del).toHaveBeenCalledWith("/features/2");
    expect(post).toHaveBeenCalledWith("/policies", policy);
    expect(put).toHaveBeenCalledWith("/policies/3", policy);
    expect(del).toHaveBeenCalledWith("/policies/3");
    expect(put).toHaveBeenCalledWith("/users/4/role", { role: "ADMIN" });
    expect(del).toHaveBeenCalledWith("/lodgings/5");
  });

  it("preserves dashboard and lodging pagination query parameters", () => {
    getAdminStats();
    getAdminLodgings({
      page: 2,
      size: 4,
      sort: "name",
      direction: "desc",
      q: "lago",
    });

    expect(get).toHaveBeenCalledWith("/admin/stats");
    expect(get).toHaveBeenCalledWith(
      "/lodgings/admin?page=2&size=4&sort=name&direction=desc&q=lago",
    );
  });
});
