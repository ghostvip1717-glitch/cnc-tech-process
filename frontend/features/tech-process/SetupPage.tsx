import { useCallback, useEffect, useState } from "react";
import { listCatalogItems, type CatalogItem } from "../../shared/api/catalog";
import {
  createOperation,
  deleteOperation,
  deleteSetup,
  getTechProcess,
  reorderOperations,
  updateOperation,
  updateSetup,
  type Operation,
  type Setup,
} from "../../shared/api/tech-process";
import {
  BottomSheet,
  ConfirmDialog,
  DragList,
  Fab,
  IconTrash,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./tech-process.css";

interface SetupPageProps {
  partId: number;
  setupId: number;
  onDeleted: () => void;
}

const emptyForm = {
  op_number: "",
  title: "",
  tool_id: "" as number | "",
  plate_id: "" as number | "",
  comment: "",
};

export function SetupPage({ partId, setupId, onDeleted }: SetupPageProps) {
  const { showError } = useToast();
  const [setup, setSetup] = useState<Setup | null>(null);
  const [jaws, setJaws] = useState<CatalogItem[]>([]);
  const [tools, setTools] = useState<CatalogItem[]>([]);
  const [plates, setPlates] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [confirmSetupDelete, setConfirmSetupDelete] = useState(false);
  const [pendingOpDelete, setPendingOpDelete] = useState<Operation | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [jawItems, toolItems, plateItems, process] = await Promise.all([
        listCatalogItems({ type: "jaw" }),
        listCatalogItems({ type: "tool" }),
        listCatalogItems({ type: "plate" }),
        getTechProcess(partId),
      ]);
      setJaws(jawItems);
      setTools(toolItems);
      setPlates(plateItems);
      const found = process.setups.find((item) => item.id === setupId) ?? null;
      setSetup(found);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить установ");
      setSetup(null);
    } finally {
      setLoading(false);
    }
  }, [partId, setupId, showError]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const reloadSetup = async () => {
    const process = await getTechProcess(partId);
    const found = process.setups.find((item) => item.id === setupId) ?? null;
    setSetup(found);
  };

  const handleUpdateJaw = async (jawId: number) => {
    setSubmitting(true);
    try {
      await updateSetup(partId, setupId, jawId);
      await reloadSetup();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось обновить установ");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSetup = async () => {
    setSubmitting(true);
    try {
      await deleteSetup(partId, setupId);
      setConfirmSetupDelete(false);
      onDeleted();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось удалить установ");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateOperation = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.op_number.trim() || !form.title.trim() || form.tool_id === "" || form.plate_id === "") {
      return;
    }

    setSubmitting(true);
    try {
      await createOperation(partId, setupId, {
        op_number: form.op_number.trim(),
        title: form.title.trim(),
        tool_id: form.tool_id,
        plate_id: form.plate_id,
        comment: form.comment.trim() || null,
      });
      setForm(emptyForm);
      setShowForm(false);
      await reloadSetup();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось добавить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleFieldUpdate = async (
    operation: Operation,
    field: "tool_id" | "plate_id" | "title" | "op_number" | "comment",
    value: string | number | null,
  ) => {
    setSubmitting(true);
    try {
      await updateOperation(partId, operation.id, { [field]: value });
      await reloadSetup();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось обновить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteOperation = async () => {
    if (!pendingOpDelete) {
      return;
    }
    setSubmitting(true);
    try {
      await deleteOperation(partId, pendingOpDelete.id);
      setPendingOpDelete(null);
      await reloadSetup();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось удалить операцию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleReorder = async (operationIds: number[]) => {
    setSubmitting(true);
    try {
      await reorderOperations(partId, setupId, operationIds);
      await reloadSetup();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось изменить порядок операций");
      throw err;
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <section className="tech-process-page">
        <SkeletonStack rows={4} rowHeight="5rem" />
      </section>
    );
  }

  if (!setup) {
    return (
      <section className="tech-process-page">
        <p className="tech-process-empty">Установ не найден</p>
      </section>
    );
  }

  return (
    <section className="tech-process-page">
      <div className="tech-process-setup-head">
        <h2>Установ {setup.order_label}</h2>
        <button
          className="ui-icon-btn danger"
          type="button"
          aria-label="Удалить установ"
          disabled={submitting}
          onClick={() => setConfirmSetupDelete(true)}
        >
          <IconTrash size={18} />
        </button>
      </div>

      <label className="tech-process-jaw-field">
        Кулачки
        <select
          value={setup.jaw_id}
          onChange={(event) => void handleUpdateJaw(Number(event.target.value))}
          disabled={submitting || jaws.length === 0}
        >
          {jaws.map((jaw) => (
            <option key={jaw.id} value={jaw.id}>
              {jaw.name}
            </option>
          ))}
        </select>
      </label>

      <h3 className="tech-process-ops-title">Операции</h3>

      {setup.operations.length === 0 ? (
        <p className="tech-process-empty">Операции не добавлены</p>
      ) : (
        <DragList
          items={setup.operations}
          getId={(operation) => operation.id}
          disabled={submitting}
          onReorder={handleReorder}
          renderItem={(operation) => (
            <div className="tech-process-operation-card">
              <div className="tech-process-operation-header">
                <strong>{operation.op_number}</strong>
                <button
                  className="ui-icon-btn danger"
                  type="button"
                  aria-label={`Удалить операцию ${operation.op_number}`}
                  disabled={submitting}
                  onClick={() => setPendingOpDelete(operation)}
                >
                  <IconTrash size={16} />
                </button>
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

              <label>
                Комментарий
                <input
                  defaultValue={operation.comment ?? ""}
                  disabled={submitting}
                  onBlur={(event) => {
                    const value = event.target.value.trim() || null;
                    if (value !== operation.comment) {
                      void handleFieldUpdate(operation, "comment", value);
                    }
                  }}
                />
              </label>
            </div>
          )}
        />
      )}

      <Fab label="Новая операция" onClick={() => setShowForm(true)} />

      <BottomSheet
        open={showForm}
        title="Новая операция"
        onClose={() => {
          if (!submitting) {
            setShowForm(false);
          }
        }}
      >
        <form className="ui-form" onSubmit={handleCreateOperation}>
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
                setForm({
                  ...form,
                  plate_id: event.target.value ? Number(event.target.value) : "",
                })
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
          <button className="ui-btn block" type="submit" disabled={submitting}>
            Создать
          </button>
        </form>
      </BottomSheet>

      <ConfirmDialog
        open={confirmSetupDelete}
        title="Удалить установ?"
        message={`Удалить установ ${setup.order_label}? Действие нельзя отменить`}
        busy={submitting}
        onCancel={() => setConfirmSetupDelete(false)}
        onConfirm={() => void handleDeleteSetup()}
      />

      <ConfirmDialog
        open={pendingOpDelete !== null}
        title="Удалить операцию?"
        message={`Удалить операцию ${pendingOpDelete?.op_number ?? ""}? Действие нельзя отменить`}
        busy={submitting}
        onCancel={() => setPendingOpDelete(null)}
        onConfirm={() => void handleDeleteOperation()}
      />
    </section>
  );
}
