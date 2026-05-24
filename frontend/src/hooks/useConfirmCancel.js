import { useState } from "react";

export default function useConfirmCancel(hasChanges, onConfirmReset) {
  const [showConfirm, setShowConfirm] = useState(false);

  const handleCancel = () => {
    if (hasChanges) {
      setShowConfirm(true);
    } else {
      onConfirmReset();
    }
  };

  const confirmCancel = () => {
    setShowConfirm(false);
    onConfirmReset();
  };

  return { showConfirm, handleCancel, confirmCancel, dismissConfirm: () => setShowConfirm(false) };
}
