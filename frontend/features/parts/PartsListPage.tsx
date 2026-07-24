import { useCallback, useEffect, useState } from "react";
import { createPart, listParts, type Part } from "../../shared/api/parts";
import { resolveApiUrl } from "../../shared/api/config";
import {
  BottomSheet,
  Fab,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./parts.css";

interface PartsListPageProps {
  onOpenPart: (partId: number) => void;
}

export function PartsListPage({ onOpenPart }: PartsListPageProps) {
  const { showError } = useToast();
  const [parts, setParts] = useState<Part[]>([]);
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [number, setNumber] = useState("");
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(search.trim()), 350);
    return () => window.clearTimeout(timer);
  }, [search]);

  const loadParts = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listParts(debouncedSearch || undefined);
      setParts(data);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить детали");
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, showError]);

  useEffect(() => {
    void loadParts();
  }, [loadParts]);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !title.trim()) {
      return;
    }

    setSubmitting(true);
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
      showError(err instanceof Error ? err.message : "Не удалось создать деталь");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="parts-page">
      <div className="parts-toolbar">
        <input
          className="parts-search"
          type="search"
          placeholder="Поиск по номеру или названию"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {loading ? (
        <SkeletonStack rows={4} rowHeight="4.25rem" />
      ) : parts.length === 0 ? (
        <p className="parts-empty">Детали не найдены</p>
      ) : (
        <ul className="parts-list">
          {parts.map((part) => {
            const cover = part.photos[0];
            return (
              <li
                key={part.id}
                className="parts-list-item"
                onClick={() => onOpenPart(part.id)}
              >
                <div className="parts-list-item-main">
                  <h2>
                    {part.number} — {part.title}
                  </h2>
                  <p>Фото: {part.photos.length}</p>
                </div>
                {cover ? (
                  <img
                    className="parts-list-thumb"
                    src={resolveApiUrl(cover.url)}
                    alt=""
                  />
                ) : (
                  <div className="parts-list-thumb placeholder" aria-hidden />
                )}
              </li>
            );
          })}
        </ul>
      )}

      <Fab label="Новая деталь" onClick={() => setShowForm(true)} />

      <BottomSheet
        open={showForm}
        title="Новая деталь"
        onClose={() => {
          if (!submitting) {
            setShowForm(false);
          }
        }}
      >
        <form className="ui-form" onSubmit={handleCreate}>
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
          <button className="ui-btn block" type="submit" disabled={submitting}>
            Создать
          </button>
        </form>
      </BottomSheet>
    </section>
  );
}
