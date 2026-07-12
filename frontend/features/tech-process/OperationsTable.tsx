import { useEffect, useState } from "react";
import { listCatalogItems, type CatalogItem } from "../../shared/api/catalog";
import {
  createOperation,
  deleteOperation,
  reorderOperations,
  updateOperation,
  type Operation,
} from "../../shared/api/tech-process";

interface OperationsTableProps {
  partId: number;
  setupId: number;
  operations: Operation[];
  onChanged: () => Promise<void>;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  setError: (value: string | null) => void;
}

const emptyForm = {
  op_number: "",
  title: "",
  tool_id: "" as number | "",
  plate_id: "" as number | "",
  comment: "",
};

export function OperationsTable({
  partId,
  setupId,
  operations,
  onChanged,
  submitting,
  setSubmitting,
  setError,
}: OperationsTableProps) {
  const [tools, setTools] = useState<CatalogItem[]>([]);
  const [plates, setPlates] = useState<CatalogItem[]>([]);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    void (async () => {
      const [toolItems, plateItems] = await Promise.all([
        listCatalogItems({ type: "tool" }),
        listCatalogItems({ type: "plate" }),
      ]);
      setTools(toolItems);
      setPlates(plateItems);
    })();
  }, []);

  const toolName = (id: number) => tools.find((item) => item.id === id)?.name ?? `#${id}`;
  const plateName = (id: number) => plates.find((item) => item.id === id)?.name ?? `#${id}`;

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.op_number.trim() || !form.title.trim() || form.tool_id === "" || form.plate_id === "") {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await createOperation(partId, setupId, {
        op_number: form.op_number.trim(),
        title: form.title.trim(),
        tool_id: form.tool_id,
        plate_id: form.plate_id,
        comment: form.comment.trim() || null,
      });
      setForm(emptyForm);
      await onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось добавить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleFieldUpdate = async (
    operation: Operation,
    field: "tool_id" | "plate_id" | "title" | "op_number" | "comment",
    value: string | number,
  ) => {
    setSubmitting(true);
    setError(null);
    try {
      await updateOperation(partId, operation.id, { [field]: value });
      await onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось обновить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (operationId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await deleteOperation(partId, operationId);
      await onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleMove = async (operationId: number, direction: "up" | "down") => {
    const ids = operations.map((operation) => operation.id);
    const index = ids.indexOf(operationId);
    if (index === -1) {
      return;
    }
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= ids.length) {
      return;
    }
    [ids[index], ids[targetIndex]] = [ids[targetIndex], ids[index]];

    setSubmitting(true);
    setError(null);
    try {
      await reorderOperations(partId, setupId, ids);
      await onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось изменить порядок операций");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="tech-process-operations">
      <h4>Операции</h4>

      {operations.length === 0 ? (
        <p className="tech-process-empty">Операции не добавлены</p>
      ) : (
        <div className="tech-process-operations-list">
          {operations.map((operation, index) => (
            <div key={operation.id} className="tech-process-operation-card">
              <div className="tech-process-operation-header">
                <strong>{operation.op_number}</strong>
                <div className="tech-process-operation-actions">
                  <button
                    className="tech-process-button secondary"
                    type="button"
                    disabled={submitting || index === 0}
                    onClick={() => void handleMove(operation.id, "up")}
                  >
                    ↑
                  </button>
                  <button
                    className="tech-process-button secondary"
                    type="button"
                    disabled={submitting || index === operations.length - 1}
                    onClick={() => void handleMove(operation.id, "down")}
                  >
                    ↓
                  </button>
                  <button
                    className="tech-process-button danger"
                    type="button"
                    disabled={submitting}
                    onClick={() => void handleDelete(operation.id)}
                  >
                    ✕
                  </button>
                </div>
              </div>

              <label>
                Номер
                <input
                  defaultValue={operation.op_number}
                  disabled={submitting}
                  onBlur={(event) => {
                    if (event.target.value !== operation.op_number) {
                      void handleFieldUpdate(operation, "op_number", event.target.value);
                    }
                  }}
                />
              </label>

              <label>
                Что делаем
                <input
                  defaultValue={operation.title}
                  disabled={submitting}
                  onBlur={(event) => {
                    if (event.target.value !== operation.title) {
                      void handleFieldUpdate(operation, "title", event.target.value);
                    }
                  }}
                />
              </label>

              <label>
                Инструмент
                <select
                  value={operation.tool_id}
                  disabled={submitting || tools.length === 0}
                  onChange={(event) =>
                    void handleFieldUpdate(operation, "tool_id", Number(event.target.value))
                  }
                >
                  {tools.map((tool) => (
                    <option key={tool.id} value={tool.id}>
                      {tool.name}
                    </option>
                  ))}
                </select>
              </label>
              <p className="tech-process-empty">{toolName(operation.tool_id)}</p>

              <label>
                Пластина
                <select
                  value={operation.plate_id}
                  disabled={submitting || plates.length === 0}
                  onChange={(event) =>
                    void handleFieldUpdate(operation, "plate_id", Number(event.target.value))
                  }
                >
                  {plates.map((plate) => (
                    <option key={plate.id} value={plate.id}>
                      {plate.name}
                    </option>
                  ))}
                </select>
              </label>
              <p className="tech-process-empty">{plateName(operation.plate_id)}</p>

              <label>
                Комментарий
                <input
                  defaultValue={operation.comment ?? ""}
                  disabled={submitting}
                  onBlur={(event) => {
                    const value = event.target.value.trim() || null;
                    if (value !== operation.comment) {
                      void updateOperation(partId, operation.id, { comment: value }).then(onChanged);
                    }
                  }}
                />
              </label>
            </div>
          ))}
        </div>
      )}

      <form className="tech-process-operation-form" onSubmit={handleCreate}>
        <h5>Новая операция</h5>
        <label>
          Номер
          <input
            value={form.op_number}
            onChange={(event) => setForm({ ...form, op_number: event.target.value })}
            placeholder="010"
            required
          />
        </label>
        <label>
          Что делаем
          <input
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
            placeholder="Точить наружный диаметр"
            required
          />
        </label>
        <label>
          Инструмент
          <select
            value={form.tool_id}
            onChange={(event) =>
              setForm({ ...form, tool_id: event.target.value ? Number(event.target.value) : "" })
            }
            required
            disabled={tools.length === 0}
          >
            <option value="">Выберите инструмент</option>
            {tools.map((tool) => (
              <option key={tool.id} value={tool.id}>
                {tool.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Пластина
          <select
            value={form.plate_id}
            onChange={(event) =>
              setForm({ ...form, plate_id: event.target.value ? Number(event.target.value) : "" })
            }
            required
            disabled={plates.length === 0}
          >
            <option value="">Выберите пластину</option>
            {plates.map((plate) => (
              <option key={plate.id} value={plate.id}>
                {plate.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Комментарий
          <input
            value={form.comment}
            onChange={(event) => setForm({ ...form, comment: event.target.value })}
          />
        </label>
        <button className="tech-process-button" type="submit" disabled={submitting}>
          + Добавить операцию
        </button>
      </form>
    </div>
  );
}
