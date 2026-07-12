import { useCallback, useEffect, useState } from "react";
import { createPart, listParts, type Part } from "../../shared/api/parts";
import "./parts.css";

interface PartsListPageProps {
  onOpenPart: (partId: number) => void;
}

export function PartsListPage({ onOpenPart }: PartsListPageProps) {
  const [parts, setParts] = useState<Part[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [number, setNumber] = useState("");
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadParts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listParts(search.trim() || undefined);
      setParts(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить детали");
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    void loadParts();
  }, [loadParts]);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !title.trim()) {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const part = await createPart({
        number: number.trim(),
        title: title.trim(),
      });
      setNumber("");
      setTitle("");
      setShowForm(false);
      await loadParts();
      onOpenPart(part.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать деталь");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="parts-page">
      <h1>Детали</h1>

      <div className="parts-toolbar">
        <input
          className="parts-search"
          type="search"
          placeholder="Поиск по номеру или названию"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <button
          className="parts-button"
          type="button"
          onClick={() => setShowForm((value) => !value)}
        >
          + Новая деталь
        </button>
      </div>

      {showForm && (
        <form className="parts-form" onSubmit={handleCreate}>
          <label>
            Номер
            <input
              value={number}
              onChange={(event) => setNumber(event.target.value)}
              placeholder="В-204"
              required
            />
          </label>
          <label>
            Название
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Втулка"
              required
            />
          </label>
          <div className="parts-form-actions">
            <button className="parts-button" type="submit" disabled={submitting}>
              Создать
            </button>
            <button
              className="parts-button secondary"
              type="button"
              onClick={() => setShowForm(false)}
              disabled={submitting}
            >
              Отмена
            </button>
          </div>
        </form>
      )}

      {error && <p className="parts-error">{error}</p>}
      {loading ? (
        <p className="parts-loading">Загрузка...</p>
      ) : parts.length === 0 ? (
        <p className="parts-empty">Детали не найдены</p>
      ) : (
        <ul className="parts-list">
          {parts.map((part) => (
            <li
              key={part.id}
              className="parts-list-item"
              onClick={() => onOpenPart(part.id)}
            >
              <h2>
                {part.number} — {part.title}
              </h2>
              <p>Фото: {part.photos.length}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
