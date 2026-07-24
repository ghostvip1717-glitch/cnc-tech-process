import { useCallback, useMemo, useState, type ReactNode } from "react";
import { ToastContext } from "./toast-context";
import "./ui.css";

interface ToastItem {
  id: number;
  message: string;
  kind: "error" | "info";
}

let toastSeq = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, kind: "error" | "info" = "info") => {
      const id = ++toastSeq;
      setToasts((prev) => [...prev, { id, message, kind }]);
      window.setTimeout(() => remove(id), 2500);
    },
    [remove],
  );

  const showError = useCallback(
    (message: string) => showToast(message, "error"),
    [showToast],
  );

  const value = useMemo(() => ({ showToast, showError }), [showToast, showError]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="ui-toast-host" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`ui-toast ${toast.kind}`}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
