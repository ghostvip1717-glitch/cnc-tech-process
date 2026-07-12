import { useCallback, useEffect, useState } from "react";
import { listCatalogItems, type CatalogItem } from "../../shared/api/catalog";
import {
  createSetup,
  createTechProcess,
  deleteSetup,
  getTechProcess,
  updateSetup,
  type TechProcess,
} from "../../shared/api/tech-process";
import { OperationsTable } from "./OperationsTable";
import "./tech-process.css";

interface TechProcessBlockProps {
  partId: number;
}

export function TechProcessBlock({ partId }: TechProcessBlockProps) {
  const [techProcess, setTechProcess] = useState<TechProcess | null>(null);
  const [jaws, setJaws] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newJawId, setNewJawId] = useState<number | "">("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const jawItems = await listCatalogItems({ type: "jaw" });
      setJaws(jawItems);

      try {
        const process = await getTechProcess(partId);
        setTechProcess(process);
      } catch (err) {
        if (err instanceof Error && err.message === "Tech process not found") {
          setTechProcess(null);
        } else {
          throw err;
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить техпроцесс");
    } finally {
      setLoading(false);
    }
  }, [partId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleCreateTechProcess = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const process = await createTechProcess(partId);
      setTechProcess(process);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать техпроцесс");
    } finally {
      setSubmitting(false);
    }
  };

  const reloadTechProcess = async () => {
    const process = await getTechProcess(partId);
    setTechProcess(process);
  };

  const handleAddSetup = async () => {
    if (newJawId === "") {
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await createSetup(partId, newJawId);
      setNewJawId("");
      await reloadTechProcess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось добавить установ");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateSetup = async (setupId: number, jawId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await updateSetup(partId, setupId, jawId);
      await reloadTechProcess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось обновить установ");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSetup = async (setupId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await deleteSetup(partId, setupId);
      await reloadTechProcess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось удалить установ");
    } finally {
      setSubmitting(false);
    }
  };

  const jawName = (jawId: number) => jaws.find((jaw) => jaw.id === jawId)?.name ?? `#${jawId}`;

  if (loading) {
    return <p className="tech-process-empty">Загрузка техпроцесса...</p>;
  }

  return (
    <section className="tech-process-block">
      <h2>Техпроцесс</h2>

      {error && <p className="tech-process-error">{error}</p>}

      {techProcess === null ? (
        <div>
          <p className="tech-process-empty">Техпроцесс ещё не создан</p>
          <button
            className="tech-process-button"
            type="button"
            onClick={() => void handleCreateTechProcess()}
            disabled={submitting}
          >
            Создать техпроцесс
          </button>
        </div>
      ) : (
        <>
          {techProcess.setups.length === 0 ? (
            <p className="tech-process-empty">Установы не добавлены</p>
          ) : (
            techProcess.setups.map((setup) => (
              <div key={setup.id} className="tech-process-setup">
                <div className="tech-process-setup-header">
                  <h3>Установ {setup.order_label}</h3>
                  <button
                    className="tech-process-button danger"
                    type="button"
                    onClick={() => void handleDeleteSetup(setup.id)}
                    disabled={submitting}
                  >
                    Удалить
                  </button>
                </div>
                <label>
                  Кулачки
                  <select
                    value={setup.jaw_id}
                    onChange={(event) =>
                      void handleUpdateSetup(setup.id, Number(event.target.value))
                    }
                    disabled={submitting || jaws.length === 0}
                  >
                    {jaws.map((jaw) => (
                      <option key={jaw.id} value={jaw.id}>
                        {jaw.name}
                      </option>
                    ))}
                  </select>
                </label>
                <p className="tech-process-empty">Выбрано: {jawName(setup.jaw_id)}</p>
                <OperationsTable
                  partId={partId}
                  setupId={setup.id}
                  operations={setup.operations}
                  onChanged={reloadTechProcess}
                  submitting={submitting}
                  setSubmitting={setSubmitting}
                  setError={setError}
                />
              </div>
            ))
          )}

          <div className="tech-process-new-setup">
            <label>
              Новый установ — кулачки
              <select
                value={newJawId}
                onChange={(event) =>
                  setNewJawId(event.target.value ? Number(event.target.value) : "")
                }
                disabled={submitting || jaws.length === 0}
              >
                <option value="">Выберите кулачки</option>
                {jaws.map((jaw) => (
                  <option key={jaw.id} value={jaw.id}>
                    {jaw.name}
                  </option>
                ))}
              </select>
            </label>
            <div className="tech-process-actions">
              <button
                className="tech-process-button"
                type="button"
                onClick={() => void handleAddSetup()}
                disabled={submitting || newJawId === ""}
              >
                + Добавить установ
              </button>
            </div>
          </div>

          {jaws.length === 0 && (
            <p className="tech-process-error">
              Добавьте кулачки в справочник, чтобы назначить установ
            </p>
          )}
        </>
      )}
    </section>
  );
}
