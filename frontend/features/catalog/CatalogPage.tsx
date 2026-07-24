import { useCallback, useEffect, useState } from "react";
import {
  createCatalogItem,
  deleteCatalogItem,
  listCatalogItems,
  updateCatalogItem,
  type CatalogItem,
  type CatalogItemType,
} from "../../shared/api/catalog";
import {
  BottomSheet,
  ConfirmDialog,
  Fab,
  IconJaw,
  IconPencil,
  IconPlate,
  IconTool,
  IconTrash,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./catalog.css";

const TABS: { type: CatalogItemType; label: string; icon: "tool" | "plate" | "jaw" }[] = [
  { type: "tool", label: "Инструмент", icon: "tool" },
  { type: "plate", label: "Пластины", icon: "plate" },
  { type: "jaw", label: "Кулачки", icon: "jaw" },
];

function TabIcon({ icon }: { icon: "tool" | "plate" | "jaw" }) {
  if (icon === "plate") {
    return <IconPlate size={16} />;
  }
  if (icon === "jaw") {
    return <IconJaw size={16} />;
  }
  return <IconTool size={16} />;
}

export function CatalogPage() {
  const { showError } = useToast();
  const [activeType, setActiveType] = useState<CatalogItemType>("tool");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [items, setItems] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState("");
  const [note, setNote] = useState("");
  const [sheetMode, setSheetMode] = useState<"create" | "edit" | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<CatalogItem | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(search.trim()), 350);
    return () => window.clearTimeout(timer);
  }, [search]);

  const loadItems = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listCatalogItems({
        type: activeType,
        q: debouncedSearch || undefined,
      });
      setItems(data);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить справочник");
    } finally {
      setLoading(false);
    }
  }, [activeType, debouncedSearch, showError]);

  useEffect(() => {
    void loadItems();
  }, [loadItems]);

  const resetForm = () => {
    setName("");
    setNote("");
    setEditingId(null);
    setSheetMode(null);
  };

  const openCreate = () => {
    setEditingId(null);
    setName("");
    setNote("");
    setSheetMode("create");
  };

  const openEdit = (item: CatalogItem) => {
    setEditingId(item.id);
    setName(item.name);
    setNote(item.note ?? "");
    setSheetMode("edit");
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim()) {
      return;
    }

    setSubmitting(true);
    try {
      if (sheetMode === "create") {
        await createCatalogItem({
          type: activeType,
          name: name.trim(),
          note: note.trim() || null,
        });
      } else if (sheetMode === "edit" && editingId !== null) {
        await updateCatalogItem(editingId, {
          name: name.trim(),
          note: note.trim() || null,
        });
      }
      resetForm();
      await loadItems();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось сохранить позицию");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setSubmitting(true);
    try {
      await deleteCatalogItem(pendingDelete.id);
      if (editingId === pendingDelete.id) {
        resetForm();
      }
      setPendingDelete(null);
      await loadItems();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось удалить позицию");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="catalog-page">
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
            <TabIcon icon={tab.icon} />
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

      {loading ? (
        <SkeletonStack rows={4} rowHeight="4rem" />
      ) : items.length === 0 ? (
        <p className="catalog-empty">Позиции не найдены</p>
      ) : (
        <ul className="catalog-list">
          {items.map((item) => (
            <li key={item.id} className="catalog-item">
              <div className="catalog-item-main">
                <h2>{item.name}</h2>
                {item.note && <p>{item.note}</p>}
              </div>
              <div className="catalog-item-actions">
                <button
                  className="ui-icon-btn"
                  type="button"
                  aria-label="Изменить"
                  onClick={() => openEdit(item)}
                  disabled={submitting}
                >
                  <IconPencil size={18} />
                </button>
                <button
                  className="ui-icon-btn danger"
                  type="button"
                  aria-label="Удалить"
                  onClick={() => setPendingDelete(item)}
                  disabled={submitting}
                >
                  <IconTrash size={18} />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Fab label="Новая позиция" onClick={openCreate} />

      <BottomSheet
        open={sheetMode !== null}
        title={sheetMode === "edit" ? "Изменить позицию" : "Новая позиция"}
        onClose={() => {
          if (!submitting) {
            resetForm();
          }
        }}
      >
        <form className="ui-form" onSubmit={handleSubmit}>
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
          <button className="ui-btn block" type="submit" disabled={submitting}>
            {sheetMode === "edit" ? "Сохранить" : "Создать"}
          </button>
        </form>
      </BottomSheet>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Удалить позицию?"
        message={`Удалить «${pendingDelete?.name ?? ""}»? Действие нельзя отменить`}
        busy={submitting}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => void handleDelete()}
      />
    </section>
  );
}
