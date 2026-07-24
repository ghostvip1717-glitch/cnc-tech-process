import { useEffect, useRef, useState, type ReactNode } from "react";
import { IconGrip } from "./Icons";
import "./ui.css";

interface DragListProps<T> {
  items: T[];
  getId: (item: T) => number;
  onReorder: (orderedIds: number[]) => void | Promise<void>;
  renderItem: (item: T) => ReactNode;
  disabled?: boolean;
}

export function DragList<T>({
  items,
  getId,
  onReorder,
  renderItem,
  disabled = false,
}: DragListProps<T>) {
  const [localItems, setLocalItems] = useState(items);
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const dragActive = useRef(false);
  const pointerId = useRef<number | null>(null);
  const orderRef = useRef(items);

  useEffect(() => {
    if (!dragActive.current) {
      setLocalItems(items);
      orderRef.current = items;
    }
  }, [items]);

  const moveItem = (from: number, to: number) => {
    if (from === to || from < 0 || to < 0) {
      return;
    }
    setLocalItems((prev) => {
      if (to >= prev.length) {
        return prev;
      }
      const next = [...prev];
      const [item] = next.splice(from, 1);
      next.splice(to, 0, item);
      orderRef.current = next;
      return next;
    });
  };

  const finishDrag = async () => {
    if (!dragActive.current) {
      return;
    }
    dragActive.current = false;
    pointerId.current = null;
    setDraggingId(null);
    const ids = orderRef.current.map(getId);
    const prevIds = items.map(getId);
    if (ids.every((id, index) => id === prevIds[index])) {
      return;
    }
    try {
      await onReorder(ids);
    } catch {
      setLocalItems(items);
      orderRef.current = items;
    }
  };

  const onHandlePointerDown = (event: React.PointerEvent, id: number) => {
    if (disabled) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    dragActive.current = true;
    pointerId.current = event.pointerId;
    setDraggingId(id);
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  };

  const onHandlePointerMove = (event: React.PointerEvent) => {
    if (!dragActive.current || pointerId.current !== event.pointerId || draggingId === null) {
      return;
    }
    const el = document.elementFromPoint(event.clientX, event.clientY);
    const row = el?.closest("[data-drag-id]") as HTMLElement | null;
    if (!row) {
      return;
    }
    const overId = Number(row.dataset.dragId);
    const current = orderRef.current;
    const from = current.findIndex((item) => getId(item) === draggingId);
    const to = current.findIndex((item) => getId(item) === overId);
    if (from !== -1 && to !== -1 && from !== to) {
      moveItem(from, to);
    }
  };

  return (
    <div className="ui-drag-list">
      {localItems.map((item) => {
        const id = getId(item);
        return (
          <div
            key={id}
            className={`ui-drag-item ${draggingId === id ? "dragging" : ""}`}
            data-drag-id={id}
          >
            <button
              type="button"
              className="ui-drag-handle"
              aria-label="Перетащить"
              disabled={disabled}
              onPointerDown={(event) => onHandlePointerDown(event, id)}
              onPointerMove={onHandlePointerMove}
              onPointerUp={() => void finishDrag()}
              onPointerCancel={() => void finishDrag()}
            >
              <IconGrip />
            </button>
            <div className="ui-drag-body">{renderItem(item)}</div>
          </div>
        );
      })}
    </div>
  );
}
