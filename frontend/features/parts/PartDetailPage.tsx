import { useCallback, useEffect, useRef, useState } from "react";
import {
  deletePart,
  deletePartPhoto,
  getPart,
  reorderPartPhotos,
  updatePart,
  uploadPartPhoto,
  type Part,
} from "../../shared/api/parts";
import "./parts.css";

interface PartDetailPageProps {
  partId: number;
  onBack: () => void;
}

export function PartDetailPage({ partId, onBack }: PartDetailPageProps) {
  const [part, setPart] = useState<Part | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [number, setNumber] = useState("");
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadPart = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getPart(partId);
      setPart(data);
      setNumber(data.number);
      setTitle(data.title);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить деталь");
    } finally {
      setLoading(false);
    }
  }, [partId]);

  useEffect(() => {
    void loadPart();
  }, [loadPart]);

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !title.trim()) {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const updated = await updatePart(partId, {
        number: number.trim(),
        title: title.trim(),
      });
      setPart(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось сохранить деталь");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await uploadPartPhoto(partId, file);
      await loadPart();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить фото");
    } finally {
      setSubmitting(false);
      event.target.value = "";
    }
  };

  const handleDeletePhoto = async (photoId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await deletePartPhoto(partId, photoId);
      await loadPart();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить фото");
    } finally {
      setSubmitting(false);
    }
  };

  const handleMovePhoto = async (photoId: number, direction: "left" | "right") => {
    if (!part) {
      return;
    }

    const ids = part.photos.map((photo) => photo.id);
    const index = ids.indexOf(photoId);
    if (index === -1) {
      return;
    }

    const targetIndex = direction === "left" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= ids.length) {
      return;
    }

    [ids[index], ids[targetIndex]] = [ids[targetIndex], ids[index]];

    setSubmitting(true);
    setError(null);
    try {
      const photos = await reorderPartPhotos(partId, ids);
      setPart({ ...part, photos });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось изменить порядок фото");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeletePart = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await deletePart(partId);
      onBack();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить деталь");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p className="parts-loading">Загрузка...</p>;
  }

  if (!part) {
    return <p className="parts-error">{error ?? "Деталь не найдена"}</p>;
  }

  return (
    <section className="parts-page">
      <div className="parts-detail-header">
        <h1>
          {part.number} — {part.title}
        </h1>
        <p>Создана: {new Date(part.created_at).toLocaleString("ru-RU")}</p>
      </div>

      <form className="parts-form" onSubmit={handleSave}>
        <label>
          Номер
          <input value={number} onChange={(event) => setNumber(event.target.value)} required />
        </label>
        <label>
          Название
          <input value={title} onChange={(event) => setTitle(event.target.value)} required />
        </label>
        <div className="parts-form-actions">
          <button className="parts-button" type="submit" disabled={submitting}>
            Сохранить
          </button>
          <button
            className="parts-button danger"
            type="button"
            onClick={() => void handleDeletePart()}
            disabled={submitting}
          >
            Удалить деталь
          </button>
        </div>
      </form>

      <div className="parts-upload">
        <button
          className="parts-button"
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={submitting}
        >
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

      {error && <p className="parts-error">{error}</p>}

      {part.photos.length === 0 ? (
        <p className="parts-empty">Фото пока нет</p>
      ) : (
        <div className="parts-gallery">
          {part.photos.map((photo, index) => (
            <div key={photo.id} className="parts-photo-card">
              <img src={photo.url} alt={`Фото ${part.number}`} />
              <div className="parts-photo-actions">
                <button
                  className="parts-button secondary"
                  type="button"
                  onClick={() => void handleMovePhoto(photo.id, "left")}
                  disabled={submitting || index === 0}
                >
                  ←
                </button>
                <button
                  className="parts-button secondary"
                  type="button"
                  onClick={() => void handleMovePhoto(photo.id, "right")}
                  disabled={submitting || index === part.photos.length - 1}
                >
                  →
                </button>
                <button
                  className="parts-button danger"
                  type="button"
                  onClick={() => void handleDeletePhoto(photo.id)}
                  disabled={submitting}
                >
                  ✕
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
