import { useCallback, useEffect, useRef, useState } from "react";
import {
  deletePartPhoto,
  getPart,
  reorderPartPhotos,
  uploadPartPhoto,
  type Part,
  type PartPhoto,
} from "../../shared/api/parts";
import { resolveApiUrl } from "../../shared/api/config";
import {
  ConfirmDialog,
  DragList,
  IconTrash,
  IconUpload,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./parts.css";

interface PartGalleryPageProps {
  partId: number;
}

export function PartGalleryPage({ partId }: PartGalleryPageProps) {
  const { showError } = useToast();
  const [part, setPart] = useState<Part | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<PartPhoto | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadPart = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPart(partId);
      setPart(data);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить фото");
      setPart(null);
    } finally {
      setLoading(false);
    }
  }, [partId, showError]);

  useEffect(() => {
    void loadPart();
  }, [loadPart]);

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setSubmitting(true);
    try {
      await uploadPartPhoto(partId, file);
      await loadPart();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить фото");
    } finally {
      setSubmitting(false);
      event.target.value = "";
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setSubmitting(true);
    try {
      await deletePartPhoto(partId, pendingDelete.id);
      setPendingDelete(null);
      await loadPart();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось удалить фото");
    } finally {
      setSubmitting(false);
    }
  };

  const handleReorder = async (photoIds: number[]) => {
    if (!part) {
      return;
    }
    setSubmitting(true);
    try {
      const photos = await reorderPartPhotos(partId, photoIds);
      setPart({ ...part, photos });
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось изменить порядок фото");
      throw err;
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <section className="parts-page">
        <SkeletonStack rows={3} rowHeight="8rem" />
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
      <div className="parts-gallery-toolbar">
        <button
          className="ui-btn"
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={submitting}
        >
          <IconUpload size={18} />
          Загрузить фото
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          hidden
          onChange={handleUpload}
        />
      </div>

      {part.photos.length === 0 ? (
        <p className="parts-empty">Фото пока нет</p>
      ) : (
        <DragList
          items={part.photos}
          getId={(photo) => photo.id}
          disabled={submitting}
          onReorder={handleReorder}
          renderItem={(photo) => (
            <div className="parts-photo-row">
              <img src={resolveApiUrl(photo.url)} alt={`Фото ${part.number}`} />
              <button
                className="ui-icon-btn danger"
                type="button"
                aria-label="Удалить фото"
                disabled={submitting}
                onClick={() => setPendingDelete(photo)}
              >
                <IconTrash size={18} />
              </button>
            </div>
          )}
        />
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Удалить фото?"
        message="Удалить это фото? Действие нельзя отменить"
        busy={submitting}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => void handleDelete()}
      />
    </section>
  );
}
