import { useEffect, useRef, type ReactNode } from "react";
import "./ui.css";

interface BottomSheetProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export function BottomSheet({ open, title, onClose, children }: BottomSheetProps) {
  const sheetRef = useRef<HTMLDivElement>(null);
  const startY = useRef<number | null>(null);
  const dragging = useRef(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  const onPointerDown = (event: React.PointerEvent) => {
    const target = event.target as HTMLElement;
    if (!target.closest(".ui-bottom-sheet-handle")) {
      return;
    }
    startY.current = event.clientY;
    dragging.current = true;
    target.setPointerCapture?.(event.pointerId);
  };

  const onPointerMove = (event: React.PointerEvent) => {
    if (!dragging.current || startY.current === null || !sheetRef.current) {
      return;
    }
    const delta = Math.max(0, event.clientY - startY.current);
    sheetRef.current.style.transform = `translateY(${delta}px)`;
  };

  const onPointerUp = (event: React.PointerEvent) => {
    if (!dragging.current || startY.current === null || !sheetRef.current) {
      return;
    }
    const delta = event.clientY - startY.current;
    dragging.current = false;
    startY.current = null;
    sheetRef.current.style.transform = "";
    if (delta > 80) {
      onClose();
    }
  };

  return (
    <div
      className="ui-overlay"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div
        ref={sheetRef}
        className="ui-bottom-sheet"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
      >
        <div className="ui-bottom-sheet-handle" />
        <h2 className="ui-bottom-sheet-title">{title}</h2>
        {children}
      </div>
    </div>
  );
}
