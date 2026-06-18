import { renderHook, act } from "@testing-library/react";
import useConfirmCancel from "./useConfirmCancel";

describe("useConfirmCancel - cancel without changes", () => {
  it("calls onConfirmReset immediately and keeps showConfirm false", () => {
    const onConfirmReset = vi.fn();
    const { result } = renderHook(() => useConfirmCancel(false, onConfirmReset));

    act(() => {
      result.current.handleCancel();
    });

    expect(onConfirmReset).toHaveBeenCalledTimes(1);
    expect(result.current.showConfirm).toBe(false);
  });
});

describe("useConfirmCancel - cancel with changes", () => {
  it("shows the confirm dialog and does not call onConfirmReset yet", () => {
    const onConfirmReset = vi.fn();
    const { result } = renderHook(() => useConfirmCancel(true, onConfirmReset));

    act(() => {
      result.current.handleCancel();
    });

    expect(result.current.showConfirm).toBe(true);
    expect(onConfirmReset).not.toHaveBeenCalled();
  });
});

describe("useConfirmCancel - confirming the reset", () => {
  it("hides the confirm dialog and calls onConfirmReset", () => {
    const onConfirmReset = vi.fn();
    const { result } = renderHook(() => useConfirmCancel(true, onConfirmReset));

    act(() => {
      result.current.handleCancel();
    });
    expect(result.current.showConfirm).toBe(true);

    act(() => {
      result.current.confirmCancel();
    });

    expect(result.current.showConfirm).toBe(false);
    expect(onConfirmReset).toHaveBeenCalledTimes(1);
  });
});

describe("useConfirmCancel - dismissing the confirm dialog", () => {
  it("hides the confirm dialog without calling onConfirmReset", () => {
    const onConfirmReset = vi.fn();
    const { result } = renderHook(() => useConfirmCancel(true, onConfirmReset));

    act(() => {
      result.current.handleCancel();
    });
    expect(result.current.showConfirm).toBe(true);

    act(() => {
      result.current.dismissConfirm();
    });

    expect(result.current.showConfirm).toBe(false);
    expect(onConfirmReset).not.toHaveBeenCalled();
  });
});
