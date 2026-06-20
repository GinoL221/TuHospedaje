import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConfirmDialog from "./ConfirmDialog";

describe("ConfirmDialog - visibility", () => {
  it("renders nothing when show is false", () => {
    render(
      <ConfirmDialog
        show={false}
        message="¿Confirmar?"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.queryByTestId("confirm-delete")).not.toBeInTheDocument();
  });
});

describe("ConfirmDialog - overlay and propagation", () => {
  it("calls onCancel when the overlay (outside the modal) is clicked", async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByTestId("confirm-delete"));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("does not call onCancel when clicking inside the modal", async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByText("¿Confirmar?"));

    expect(onCancel).not.toHaveBeenCalled();
  });
});

describe("ConfirmDialog - basic confirm/cancel actions", () => {
  it("calls onConfirm when the confirm button is clicked", async () => {
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    await user.click(screen.getByTestId("confirm-delete-yes"));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("calls onCancel when the cancel button is clicked", async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={vi.fn().mockResolvedValue(undefined)}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByTestId("confirm-delete-no"));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

describe("ConfirmDialog - double-click guard while onConfirm is in flight", () => {
  it("calls onConfirm only once when the confirm button is double-clicked before it resolves", async () => {
    let resolveConfirm;
    const onConfirm = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveConfirm = resolve;
        })
    );
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    const confirmButton = screen.getByTestId("confirm-delete-yes");
    await user.click(confirmButton);
    await user.click(confirmButton);

    expect(onConfirm).toHaveBeenCalledTimes(1);

    resolveConfirm();
    await waitFor(() => expect(confirmButton).not.toBeDisabled());
  });

  it("disables the confirm button while onConfirm is pending and re-enables it after it resolves", async () => {
    let resolveConfirm;
    const onConfirm = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveConfirm = resolve;
        })
    );
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    const confirmButton = screen.getByTestId("confirm-delete-yes");
    await user.click(confirmButton);

    expect(confirmButton).toBeDisabled();

    resolveConfirm();
    await waitFor(() => expect(confirmButton).not.toBeDisabled());
  });

  it("re-enables the confirm button after onConfirm rejects", async () => {
    let rejectConfirm;
    const onConfirm = vi.fn(
      () =>
        new Promise((_resolve, reject) => {
          rejectConfirm = reject;
        })
    );
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    const confirmButton = screen.getByTestId("confirm-delete-yes");
    await user.click(confirmButton);

    expect(confirmButton).toBeDisabled();

    rejectConfirm(new Error("request failed"));
    await waitFor(() => expect(confirmButton).not.toBeDisabled());
  });

  it("ignores clicks on cancel while onConfirm is pending", async () => {
    let resolveConfirm;
    const onConfirm = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveConfirm = resolve;
        })
    );
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        show={true}
        message="¿Confirmar?"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByTestId("confirm-delete-yes"));
    await user.click(screen.getByTestId("confirm-delete-no"));

    expect(onCancel).not.toHaveBeenCalled();

    resolveConfirm();
    await waitFor(() =>
      expect(screen.getByTestId("confirm-delete-no")).not.toBeDisabled()
    );
  });
});
