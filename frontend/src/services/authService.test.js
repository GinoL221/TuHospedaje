import { describe, it, expect, beforeEach, vi } from "vitest";
import { get, post, bootstrapCsrf } from "./api";
import { getCurrentUser, login, register, logout } from "./authService";

vi.mock("./api");

const user = { email: "test@example.com" };

beforeEach(() => {
	vi.clearAllMocks();
	bootstrapCsrf.mockResolvedValue(undefined);
});

describe("authService", () => {
	it("gets the current user and waits for CSRF bootstrap before resolving", async () => {
		const calls = [];
		let resolveBootstrap;
		get.mockImplementation(() => {
			calls.push("get");
			return Promise.resolve(user);
		});
		bootstrapCsrf.mockImplementation(() => {
			calls.push("csrf");
			return new Promise((resolve) => {
				resolveBootstrap = resolve;
			});
		});

		const result = getCurrentUser();
		await vi.waitFor(() => expect(bootstrapCsrf).toHaveBeenCalledOnce());

		expect(get).toHaveBeenCalledWith("/auth/me");
		expect(calls).toEqual(["get", "csrf"]);
		resolveBootstrap();
		await expect(result).resolves.toBe(user);
	});

	it("logs in and waits for CSRF bootstrap before resolving the response", async () => {
		let resolveBootstrap;
		post.mockResolvedValue(user);
		bootstrapCsrf.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveBootstrap = resolve;
				}),
		);

		const result = login("test@example.com", "secret");
		await vi.waitFor(() => expect(bootstrapCsrf).toHaveBeenCalledOnce());

		expect(post).toHaveBeenCalledWith("/auth/login", {
			email: "test@example.com",
			password: "secret",
		});
		resolveBootstrap();
		await expect(result).resolves.toBe(user);
	});

	it("registers, then waits for CSRF bootstrap before resolving the response", async () => {
		let resolveBootstrap;
		post.mockResolvedValue(user);
		bootstrapCsrf.mockImplementation(
			() =>
				new Promise((resolve) => {
					resolveBootstrap = resolve;
				}),
		);

		const result = register("Test", "User", "test@example.com", "secret");
		await vi.waitFor(() => expect(bootstrapCsrf).toHaveBeenCalledOnce());

		expect(post).toHaveBeenCalledWith("/auth/register", {
			firstName: "Test",
			lastName: "User",
			email: "test@example.com",
			password: "secret",
		});
		resolveBootstrap();
		await expect(result).resolves.toBe(user);
	});

	it("throws the retry-via-login message when CSRF bootstrap fails after registration", async () => {
		post.mockResolvedValue(user);
		bootstrapCsrf.mockRejectedValue(new Error("CSRF unavailable"));

		await expect(
			register("Test", "User", "test@example.com", "secret"),
		).rejects.toThrow(
			"Tu cuenta se creó correctamente, pero no pudimos iniciar sesión automáticamente. Iniciá sesión con tus credenciales.",
		);
	});

	it("propagates registration failures without bootstrapping CSRF", async () => {
		const error = new Error("El email ya está registrado");
		post.mockRejectedValue(error);

		await expect(
			register("Test", "User", "test@example.com", "secret"),
		).rejects.toBe(error);
		expect(bootstrapCsrf).not.toHaveBeenCalled();
	});

	it("logs out through the auth endpoint", async () => {
		post.mockResolvedValue(null);

		await expect(logout()).resolves.toBeNull();
		expect(post).toHaveBeenCalledWith("/auth/logout");
		expect(bootstrapCsrf).not.toHaveBeenCalled();
	});
});
