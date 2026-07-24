import { useCallback, useEffect, useState } from "react";
import { getPart, type Part } from "../../shared/api/parts";
import { resolveApiUrl } from "../../shared/api/config";
import {
  IconAssembly,
  IconChevronRight,
  IconImage,
  IconPencil,
  IconProcess,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./parts.css";

interface PartDetailPageProps {
  partId: number;
  onEdit: () => void;
  onOpenGallery: () => void;
  onOpenTechProcess: () => void;
  onOpenAssembly: () => void;
}

export function PartDetailPage({
  partId,
  onEdit,
  onOpenGallery,
  onOpenTechProcess,
  onOpenAssembly,
}: PartDetailPageProps) {
  const { showError } = useToast();
  const [part, setPart] = useState<Part | null>(null);
  const [loading, setLoading] = useState(true);

  const loadPart = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPart(partId);
      setPart(data);
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

  if (loading) {
    return (
      <section className="parts-page">
        <SkeletonStack rows={3} rowHeight="5rem" />
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

  const cover = part.photos[0];

  return (
    <section className="parts-page">
      <div className="parts-detail-header">
        <div className="parts-detail-title-row">
          <h1>
            {part.number} — {part.title}
          </h1>
          <button
            className="ui-icon-btn"
            type="button"
            aria-label="Изменить деталь"
            onClick={onEdit}
          >
            <IconPencil size={20} />
          </button>
        </div>
        <p>Создана: {new Date(part.created_at).toLocaleString("ru-RU")}</p>
      </div>

      <button className="parts-cover" type="button" onClick={onOpenGallery}>
        {cover ? (
          <img src={resolveApiUrl(cover.url)} alt={`Фото ${part.number}`} />
        ) : (
          <span className="parts-cover-empty">
            <IconImage size={28} />
            <span>Добавить фото</span>
          </span>
        )}
      </button>

      <nav className="parts-menu">
        <button className="parts-menu-item" type="button" onClick={onOpenTechProcess}>
          <span className="parts-menu-icon" aria-hidden>
            <IconProcess />
          </span>
          <span className="parts-menu-label">Техпроцесс</span>
          <IconChevronRight size={18} />
        </button>
        <button className="parts-menu-item" type="button" onClick={onOpenAssembly}>
          <span className="parts-menu-icon" aria-hidden>
            <IconAssembly />
          </span>
          <span className="parts-menu-label">Сборка</span>
          <IconChevronRight size={18} />
        </button>
      </nav>
    </section>
  );
}
