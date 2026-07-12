import { useCallback, useEffect, useState } from "react";
import {
  createCatalogItem,
  deleteCatalogItem,
  listCatalogItems,
  updateCatalogItem,
  type CatalogItem,
  type CatalogItemType,
} from "../../shared/api/catalog";
import "./catalog.css";

const TABS: { type: CatalogItemType; label: string }[] = [
  { type: "tool", label: "Инструмент" },
  { type: "plate", label: "Пластины" },
  { type: "jaw", label: "Кулачки" },
];

export function CatalogPage({ showHeading = true }: { showHeading?: boolean }) {
  const [activeType, setActiveType] = useState<CatalogItemType>("tool");
  const [search, setSearch] = useState("");
  const [items, setItems] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [note, setNote] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listCatalogItems({
        type: activeType,
        q: search.trim() || undefined,
      });
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить справочник");
    } finally {
      setLoading(false);
    }
  }, [activeType, search]);

  useEffect(() => {
    void loadItems();
  }, [loadItems]);

  const resetForm = () => {
    setName("");
    setNote("");
    setEditingId(null);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim()) {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      if (editingId === null) {
        await createCatalogItem({
          type: activeType,
          name: name.trim(),
          note: note.trim() || null,
        });
      } else {
        await updateCatalogItem(editingId, {
          name: name.trim(),
          note: note.trim() || null,
        });
      }
      resetForm();
      await loadItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось сохранить позицию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (item: CatalogItem) => {
    setEditingId(item.id);
    setName(item.name);
    setNote(item.note ?? "");
  };

  const handleDelete = async (itemId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await deleteCatalogItem(itemId);
      if (editingId === itemId) {
        resetForm();
      }
      await loadItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить позицию");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="catalog-page">
      {showHeading && <h1>Справочник</h1>}

      <div className="catalog-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.type}
            type="button"
            className={`catalog-tab ${activeType === tab.type ? "active" : ""}`}
            onClick={() => {
              setActiveType(tab.type);
              resetForm();
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <input
        className="catalog-search"
        type="search"
        placeholder="Поиск по названию"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
      />

      <form className="catalog-form" onSubmit={handleSubmit}>
        <label>
          Название
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="CNMG 120408"
            required
          />
        </label>
        <label>
          Примечание
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={2}
            placeholder="Необязательно"
          />
        </label>
        <div className="catalog-form-actions">
          <button className="catalog-button" type="submit" disabled={submitting}>
            {editingId === null ? "Добавить" : "Сохранить"}
          </button>
          {editingId !== null && (
            <button
              className="catalog-button secondary"
              type="button"
              onClick={resetForm}
              disabled={submitting}
            >
              Отмена
            </button>
          )}
        </div>
      </form>

      {error && <p className="catalog-error">{error}</p>}
      {loading ? (
        <p className="catalog-empty">Загрузка...</p>
      ) : items.length === 0 ? (
        <p className="catalog-empty">Позиции не найдены</p>
      ) : (
        <ul className="catalog-list">
          {items.map((item) => (
            <li key={item.id} className="catalog-item">
              <h2>{item.name}</h2>
              {item.note && <p>{item.note}</p>}
              <div className="catalog-item-actions">
                <button
                  className="catalog-button secondary"
                  type="button"
                  onClick={() => handleEdit(item)}
                  disabled={submitting}
                >
                  Изменить
                </button>
                <button
                  className="catalog-button danger"
                  type="button"
                  onClick={() => void handleDelete(item.id)}
                  disabled={submitting}
                >
                  Удалить
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
