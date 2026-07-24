import { createContext } from "react";

type ToastKind = "error" | "info";

export interface ToastContextValue {
  showToast: (message: string, kind?: ToastKind) => void;
  showError: (message: string) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);
