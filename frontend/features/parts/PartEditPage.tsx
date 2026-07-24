import { useCallback, useEffect, useState } from "react";
import { deletePart, getPart, updatePart, type Part } from "../../shared/api/parts";
import { ConfirmDialog, IconSave, IconTrash, SkeletonStack, useToast } from "../../shared/ui";
import "./parts.css";

interface PartEditPageProps {
  partId: number;
  onSaved: () => void;
  onDeleted: () => void;
}

export function PartEditPage({ partId, onSaved, onDeleted }: PartEditPageProps) {
  const { showError } = useToast();
  const [part, setPart] = useState<Part | null>(null);
  const [loading, setLoading] = useState(true);
  const [number, setNumber] = useState("");
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const loadPart = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPart(partId);
      setPart(data);
      setNumber(data.number);
      setTitle(data.title);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить деталь");
      setPart(null);
    } finally {
      setLoading(false);
    }
  }, [partId, showError]);

  useEffect(() => {
    void loadPart();
  }, [loadPart]);

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !title.trim()) {
      return;
    }

    setSubmitting(true);
    try {
      await updatePart(partId, {
        number: number.trim(),
        title: title.trim(),
      });
      onSaved();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось сохранить деталь");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setSubmitting(true);
    try {
      await deletePart(partId);
      setConfirmDelete(false);
      onDeleted();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось удалить деталь");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <section className="parts-page">
        <SkeletonStack rows={3} rowHeight="3.5rem" />
      </section>
    );
  }

  if (!part) {
    return (
      <section className="parts-page">
        <p className="parts-empty">Деталь не найдена</p>
      </section>
    );
  }

  return (
    <section className="parts-page">
      <form className="ui-form parts-edit-form" onSubmit={handleSave}>
        <label>
          Номер
          <input value={number} onChange={(event) => setNumber(event.target.value)} required />
        </label>
        <label>
          Название
          <input value={title} onChange={(event) => setTitle(event.target.value)} required />
        </label>
        <button className="ui-btn block" type="submit" disabled={submitting}>
          <IconSave size={18} />
          Сохранить
        </button>
      </form>

      <button
        className="ui-btn danger block parts-delete-btn"
        type="button"
        onClick={() => setConfirmDelete(true)}
        disabled={submitting}
      >
        <IconTrash size={18} />
        Удалить деталь
      </button>

      <ConfirmDialog
        open={confirmDelete}
        title="Удалить деталь?"
        message={`Удалить деталь ${part.number}? Действие нельзя отменить`}
        busy={submitting}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => void handleDelete()}
      />
    </section>
  );
}
