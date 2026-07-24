import { useCallback, useEffect, useState } from "react";
import { listCatalogItems, type CatalogItem } from "../../shared/api/catalog";
import {
  createSetup,
  createTechProcess,
  getTechProcess,
  type TechProcess,
} from "../../shared/api/tech-process";
import {
  BottomSheet,
  Fab,
  IconChevronRight,
  IconJaw,
  SkeletonStack,
  useToast,
} from "../../shared/ui";
import "./tech-process.css";

interface TechProcessPageProps {
  partId: number;
  onOpenSetup: (setupId: number) => void;
}

function opsLabel(count: number): string {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) {
    return `${count} операция`;
  }
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return `${count} операции`;
  }
  return `${count} операций`;
}

export function TechProcessPage({ partId, onOpenSetup }: TechProcessPageProps) {
  const { showError } = useToast();
  const [techProcess, setTechProcess] = useState<TechProcess | null>(null);
  const [jaws, setJaws] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [newJawId, setNewJawId] = useState<number | "">("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
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
      showError(err instanceof Error ? err.message : "Не удалось загрузить техпроцесс");
    } finally {
      setLoading(false);
    }
  }, [partId, showError]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const jawName = (jawId: number) => jaws.find((jaw) => jaw.id === jawId)?.name ?? `#${jawId}`;

  const ensureTechProcess = async (): Promise<TechProcess> => {
    if (techProcess) {
      return techProcess;
    }
    const process = await createTechProcess(partId);
    setTechProcess(process);
    return process;
  };

  const handleAddSetup = async (event: React.FormEvent) => {
    event.preventDefault();
    if (newJawId === "") {
      return;
    }

    setSubmitting(true);
    try {
      await ensureTechProcess();
      const setup = await createSetup(partId, newJawId);
      setNewJawId("");
      setShowForm(false);
      await loadData();
      onOpenSetup(setup.id);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось добавить установ");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <section className="tech-process-page">
        <SkeletonStack rows={3} rowHeight="4.5rem" />
      </section>
    );
  }

  const setups = techProcess?.setups ?? [];

  return (
    <section className="tech-process-page">
      {setups.length === 0 ? (
        <p className="tech-process-empty">Установы не добавлены</p>
      ) : (
        <ul className="tech-process-setup-list">
          {setups.map((setup) => (
            <li key={setup.id}>
              <button
                className="tech-process-setup-card"
                type="button"
                onClick={() => onOpenSetup(setup.id)}
              >
                <span className="tech-process-setup-card-icon" aria-hidden>
                  <IconJaw />
                </span>
                <span className="tech-process-setup-card-body">
                  <strong>Установ {setup.order_label}</strong>
                  <span>
                    Кулачки {jawName(setup.jaw_id)} · {opsLabel(setup.operations.length)}
                  </span>
                </span>
                <IconChevronRight size={18} />
              </button>
            </li>
          ))}
        </ul>
      )}

      {jaws.length === 0 && (
        <p className="tech-process-hint">
          Добавьте кулачки в справочник, чтобы назначить установ
        </p>
      )}

      <Fab
        label="Новый установ"
        onClick={() => setShowForm(true)}
        disabled={jaws.length === 0}
      />

      <BottomSheet
        open={showForm}
        title="Новый установ"
        onClose={() => {
          if (!submitting) {
            setShowForm(false);
          }
        }}
      >
        <form className="ui-form" onSubmit={handleAddSetup}>
          <label>
            Кулачки
            <select
              value={newJawId}
              onChange={(event) =>
                setNewJawId(event.target.value ? Number(event.target.value) : "")
              }
              required
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
          <button className="ui-btn block" type="submit" disabled={submitting || newJawId === ""}>
            Создать
          </button>
        </form>
      </BottomSheet>
    </section>
  );
}
