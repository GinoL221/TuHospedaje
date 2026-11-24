import { act, fireEvent, render, screen } from "@testing-library/react";
import BookingDateFields from "./BookingDateFields";

function createAnimationFrameController() {
	let nextId = 1;
	const callbacks = new Map();
	const cancelSpy = vi
		.spyOn(window, "cancelAnimationFrame")
		.mockImplementation((id) => {
			callbacks.delete(id);
		});
	const requestSpy = vi
		.spyOn(window, "requestAnimationFrame")
		.mockImplementation((callback) => {
			const id = nextId++;
			callbacks.set(id, callback);
			return id;
		});

	const flushFrame = () => {
		const entries = Array.from(callbacks.entries());
		callbacks.clear();
		act(() => {
			for (const [, callback] of entries) {
				callback(performance.now());
			}
		});
	};

	return {
		callbacks,
		requestSpy,
		cancelSpy,
		flushFrame,
		restore() {
			requestSpy.mockRestore();
			cancelSpy.mockRestore();
		},
	};
}

function renderDateFields(overrides = {}) {
	const props = {
		availabilityStatus: "ready",
		occupiedRanges: [],
		retryAvailability: vi.fn(),
		availabilityMessageId: "availability-msg",
		dateFieldsetDescribedBy: "availability-msg",
		checkIn: null,
		checkOut: null,
		onCheckInChange: vi.fn(),
		onCheckOutChange: vi.fn(),
		isDateOccupied: () => false,
		isCheckOutDateAvailable: () => true,
		...overrides,
	};
	return render(<BookingDateFields {...props} />);
}

async function flushTimeoutTick() {
	await act(async () => {
		await new Promise((resolve) => setTimeout(resolve, 0));
	});
}

describe("BookingDateFields - Escape focus restoration", () => {
	let raf;

	beforeEach(() => {
		raf = createAnimationFrameController();
	});

	afterEach(() => {
		raf.restore();
	});

	it("keeps focus on #booking-check-in after Escape when rAF deferBlur runs after setTimeout(0)", async () => {
		renderDateFields();

		const checkIn = screen.getByLabelText("Check-in");
		act(() => {
			checkIn.focus();
			fireEvent.click(checkIn);
		});
		expect(document.querySelector(".react-datepicker")).toBeInTheDocument();

		act(() => {
			fireEvent.keyDown(checkIn, { key: "Escape", code: "Escape" });
		});

		// Simulate WebKit ordering: react-datepicker's setTimeout(0) refocus runs
		// before its rAF deferBlur() blurs the input back to document.body.
		await flushTimeoutTick();
		raf.flushFrame();
		raf.flushFrame();

		expect(checkIn).toHaveFocus();
		expect(document.querySelector(".react-datepicker")).not.toBeInTheDocument();

		// Focus restoration must not leave preventOpenOnFocus stuck on.
		act(() => {
			fireEvent.keyDown(checkIn, { key: "Enter", code: "Enter" });
		});
		expect(document.querySelector(".react-datepicker")).toBeInTheDocument();
	});

	it("keeps focus on #booking-check-out after Escape when rAF deferBlur runs after setTimeout(0)", async () => {
		renderDateFields();

		const checkOut = screen.getByLabelText("Check-out");
		act(() => {
			checkOut.focus();
			fireEvent.click(checkOut);
		});
		expect(document.querySelector(".react-datepicker")).toBeInTheDocument();

		act(() => {
			fireEvent.keyDown(checkOut, { key: "Escape", code: "Escape" });
		});

		await flushTimeoutTick();
		raf.flushFrame();
		raf.flushFrame();

		expect(checkOut).toHaveFocus();
		expect(document.querySelector(".react-datepicker")).not.toBeInTheDocument();
	});

	it("cancels pending focus-restore animation frames on unmount", () => {
		const { unmount } = renderDateFields();

		const checkIn = screen.getByLabelText("Check-in");
		act(() => {
			checkIn.focus();
			fireEvent.click(checkIn);
		});
		expect(document.querySelector(".react-datepicker")).toBeInTheDocument();

		act(() => {
			fireEvent.keyDown(checkIn, { key: "Escape", code: "Escape" });
		});

		// Advance past the outer frame so the inner frame is pending, then unmount.
		raf.flushFrame();
		const pendingBeforeUnmount = Array.from(raf.callbacks.keys());
		expect(pendingBeforeUnmount.length).toBeGreaterThan(0);

		unmount();

		for (const id of pendingBeforeUnmount) {
			expect(raf.cancelSpy).toHaveBeenCalledWith(id);
		}
		expect(raf.callbacks.size).toBe(0);
	});

	it("does not restore focus when the calendar closes without Escape", async () => {
		renderDateFields();

		const checkIn = screen.getByLabelText("Check-in");
		act(() => {
			checkIn.focus();
			fireEvent.click(checkIn);
		});
		expect(document.querySelector(".react-datepicker")).toBeInTheDocument();

		act(() => {
			fireEvent.keyDown(checkIn, { key: "Tab", code: "Tab" });
		});

		await flushTimeoutTick();
		raf.flushFrame();
		raf.flushFrame();

		expect(checkIn).not.toHaveFocus();
		expect(document.querySelector(".react-datepicker")).not.toBeInTheDocument();
	});
});

describe("BookingDateFields - input hints and aria-invalid defaults", () => {
	it("exposes name, enterKeyHint, and explicit aria-invalid=false on check-in and check-out inputs", () => {
		renderDateFields();

		const checkIn = screen.getByLabelText("Check-in");
		const checkOut = screen.getByLabelText("Check-out");

		expect(checkIn).toHaveAttribute("name", "checkIn");
		expect(checkIn).toHaveAttribute("enterkeyhint", "next");
		expect(checkIn).toHaveAttribute("aria-invalid", "false");

		expect(checkOut).toHaveAttribute("name", "checkOut");
		expect(checkOut).toHaveAttribute("enterkeyhint", "next");
		expect(checkOut).toHaveAttribute("aria-invalid", "false");
	});
});
