import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ShareModal from "./ShareModal";

const lodging = {
	name: "Cabaña del Lago",
	city: "Bariloche",
	imageUrls: ["https://example.com/cabana.jpg"],
};

describe("ShareModal", () => {
	it("exposes a named, described modal and initially focuses close", () => {
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		const dialog = screen.getByRole("dialog", { name: "Compartir" });
		expect(dialog.parentElement).toHaveClass("share-modal-overlay");
		expect(dialog.parentElement).not.toHaveClass("modal-overlay");
		expect(dialog).toHaveAttribute("aria-modal", "true");
		expect(dialog).toHaveAccessibleDescription(
			"Elegí cómo enviar este alojamiento.",
		);
		const description = screen.getByText("Elegí cómo enviar este alojamiento.");
		expect(description).toHaveClass("share-sr-only");
		expect(dialog).toHaveAttribute("aria-describedby", description.id);

		const closeButton = screen.getByRole("button", { name: "Cerrar" });
		const modalHeader = closeButton.closest("header");
		expect(closeButton).toHaveAttribute("type", "button");
		expect(closeButton).toHaveClass("modal-close");
		expect(modalHeader).toHaveClass("share-header");
		expect(modalHeader).not.toHaveClass("site-header");
		expect(dialog).toContainElement(modalHeader);
		expect(closeButton.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
		expect(closeButton).toHaveFocus();
	});

	it("uses distinct accessible IDs for each instance", () => {
		render(
			<>
				<ShareModal lodging={lodging} onClose={vi.fn()} />
				<ShareModal lodging={lodging} onClose={vi.fn()} />
			</>,
		);

		const dialogs = screen.getAllByRole("dialog", { name: "Compartir" });
		expect(dialogs[0].getAttribute("aria-labelledby")).not.toBe(
			dialogs[1].getAttribute("aria-labelledby"),
		);
		expect(dialogs[0].getAttribute("aria-describedby")).not.toBe(
			dialogs[1].getAttribute("aria-describedby"),
		);
	});

	it("renders the existing lodging image and readable lodging summary", () => {
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		expect(screen.getByRole("img", { name: "Cabaña del Lago" })).toHaveAttribute(
			"src",
			"https://example.com/cabana.jpg",
		);
		expect(screen.getByText("Cabaña del Lago")).toBeInTheDocument();
		expect(screen.getByText("Bariloche")).toBeInTheDocument();
		expect(screen.queryByText(window.location.href)).not.toBeInTheDocument();
	});

	it("copies the current URL and announces success", async () => {
		const user = userEvent.setup();
		const writeText = vi.fn().mockResolvedValue(undefined);
		Object.defineProperty(navigator, "clipboard", {
			configurable: true,
			value: { writeText },
		});
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		await user.click(screen.getByRole("button", { name: "Copiar enlace" }));

		expect(writeText).toHaveBeenCalledWith(window.location.href);
		expect(screen.getByText("Enlace copiado")).toHaveAttribute(
			"aria-live",
			"polite",
		);
	});

	it("announces a clipboard write failure", async () => {
		const user = userEvent.setup();
		Object.defineProperty(navigator, "clipboard", {
			configurable: true,
			value: { writeText: vi.fn().mockRejectedValue(new Error("Denied")) },
		});
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		await user.click(screen.getByRole("button", { name: "Copiar enlace" }));

		expect(screen.getByText("No se pudo copiar el enlace")).toHaveAttribute(
			"aria-live",
			"polite",
		);
	});

	it("uses the copy command fallback when Clipboard API is unavailable", async () => {
		const user = userEvent.setup();
		Object.defineProperty(navigator, "clipboard", {
			configurable: true,
			value: undefined,
		});
		const execCommand = vi.fn().mockReturnValue(true);
		Object.defineProperty(document, "execCommand", {
			configurable: true,
			value: execCommand,
		});
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		await user.click(screen.getByRole("button", { name: "Copiar enlace" }));

		expect(execCommand).toHaveBeenCalledWith("copy");
		expect(screen.getByText("Enlace copiado")).toBeInTheDocument();
		expect(document.querySelector(".share-copy-fallback")).toBeNull();
	});

	it("omits the image when the lodging has none", () => {
		render(
			<ShareModal lodging={{ ...lodging, imageUrls: [] }} onClose={vi.fn()} />,
		);

		expect(screen.queryByRole("img")).not.toBeInTheDocument();
	});

	it("preserves all share URLs and external-link attributes", () => {
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		const url = window.location.href;
		const text = "Mirá este alojamiento en TuHospedaje: Cabaña del Lago";
		const expectedLinks = {
			Facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
			Twitter: `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`,
			WhatsApp: `https://wa.me/?text=${encodeURIComponent(`${text} ${url}`)}`,
			Instagram: "https://www.instagram.com/",
		};

		for (const [name, href] of Object.entries(expectedLinks)) {
			const link = screen.getByRole("link", { name });
			expect(link).toHaveAttribute("href", href);
			expect(link).toHaveAttribute("target", "_blank");
			expect(link).toHaveAttribute("rel", "noopener noreferrer");
		}
	});

	it("keeps interactive foregrounds on the accessible secondary token", () => {
		const css = readFileSync(
			resolve(process.cwd(), "src/components/ShareModal/ShareModal.css"),
			"utf8",
		);

		expect(css).toMatch(
			/\.share-btn\s*{[^}]*color:\s*var\(--secondary\);[^}]*background:\s*color-mix\(in srgb, var\(--share-brand\) 8%, var\(--bg\)\);/,
		);
		expect(css).toMatch(
			/\.share-btn:hover\s*{[^}]*background:\s*color-mix\(in srgb, var\(--share-brand\) 18%, var\(--bg\)\);[^}]*border-color:\s*var\(--secondary\);[^}]*color:\s*var\(--secondary\);/,
		);
		expect(css).toMatch(
			/\.share-copy-btn:hover\s*{[^}]*background:\s*color-mix\(in srgb, var\(--accent\) 14%, var\(--bg\)\);[^}]*border-color:\s*var\(--secondary\);[^}]*color:\s*var\(--secondary\);/,
		);
		expect(css).not.toMatch(
			/\.(?:share-btn|share-copy-btn):hover\s*{[^}]*color:\s*#fff\b/,
		);
	});

	it("contains Tab and Shift+Tab across every enabled control and link", async () => {
		const user = userEvent.setup();
		render(<ShareModal lodging={lodging} onClose={vi.fn()} />);

		const closeButton = screen.getByRole("button", { name: "Cerrar" });
		const copyButton = screen.getByRole("button", { name: "Copiar enlace" });
		const links = ["Facebook", "Twitter", "WhatsApp", "Instagram"].map((name) =>
			screen.getByRole("link", { name }),
		);

		expect(closeButton).toHaveFocus();
		await user.tab();
		expect(copyButton).toHaveFocus();
		for (const link of links) {
			await user.tab();
			expect(link).toHaveFocus();
		}
		await user.tab();
		expect(closeButton).toHaveFocus();
		await user.tab({ shift: true });
		expect(links.at(-1)).toHaveFocus();
	});

	it("closes on Escape", async () => {
		const onClose = vi.fn();
		const user = userEvent.setup();
		render(<ShareModal lodging={lodging} onClose={onClose} />);

		await user.keyboard("{Escape}");

		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("closes on overlay click but not on an interior click", async () => {
		const onClose = vi.fn();
		const user = userEvent.setup();
		render(<ShareModal lodging={lodging} onClose={onClose} />);
		const dialog = screen.getByRole("dialog", { name: "Compartir" });

		await user.click(dialog);
		expect(onClose).not.toHaveBeenCalled();

		await user.click(dialog.parentElement);
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it("restores focus to the trigger when unmounted", async () => {
		const user = userEvent.setup();
		const { rerender } = render(<button>Compartir alojamiento</button>);
		const trigger = screen.getByRole("button", { name: "Compartir alojamiento" });
		await user.click(trigger);

		rerender(
			<>
				<button>Compartir alojamiento</button>
				<ShareModal lodging={lodging} onClose={vi.fn()} />
			</>,
		);
		expect(screen.getByRole("button", { name: "Cerrar" })).toHaveFocus();

		rerender(<button>Compartir alojamiento</button>);
		expect(trigger).toHaveFocus();
	});
});
