import { useCallback, useEffect, useState } from "react";
import { getRequiredItems, type RequiredItems } from "../../shared/api/assembly";
import { IconJaw, IconPlate, IconTool, SkeletonStack, useToast } from "../../shared/ui";
import "./assembly.css";

interface AssemblyPageProps {
  partId: number;
}

export function AssemblyPage({ partId }: AssemblyPageProps) {
  const { showError } = useToast();
  const [items, setItems] = useState<RequiredItems | null>(null);
  const [loading, setLoading] = useState(true);

  const loadItems = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getRequiredItems(partId);
      setItems(data);
    } catch (err) {
      showError(err instanceof Error ? err.message : "Не удалось загрузить сводку");
      setItems(null);
    } finally {
      setLoading(false);
    }
  }, [partId, showError]);

  useEffect(() => {
    void loadItems();
  }, [loadItems]);

  if (loading) {
    return (
      <section className="assembly-page">
        <SkeletonStack rows={3} rowHeight="5rem" />
      </section>
    );
  }

  if (!items) {
    return (
      <section className="assembly-page">
        <p className="assembly-empty">Нет данных</p>
      </section>
    );
  }

  const isEmpty =
    items.tools.length === 0 && items.plates.length === 0 && items.jaws.length === 0;

  return (
    <section className="assembly-page">
      <p className="assembly-lead">Нужно для изготовления</p>
      {isEmpty ? (
        <p className="assembly-empty">Нет данных — добавьте установы и операции</p>
      ) : (
        <>
          {items.tools.length > 0 && (
            <div className="assembly-group">
              <h3>
                <IconTool size={18} />
                Инструмент
              </h3>
              <ul>
                {items.tools.map((item) => (
                  <li key={`tool-${item.id}`}>{item.name}</li>
                ))}
              </ul>
            </div>
          )}
          {items.plates.length > 0 && (
            <div className="assembly-group">
              <h3>
                <IconPlate size={18} />
                Пластины
              </h3>
              <ul>
                {items.plates.map((item) => (
                  <li key={`plate-${item.id}`}>{item.name}</li>
                ))}
              </ul>
            </div>
          )}
          {items.jaws.length > 0 && (
            <div className="assembly-group">
              <h3>
                <IconJaw size={18} />
                Кулачки
              </h3>
              <ul>
                {items.jaws.map((item) => (
                  <li key={`jaw-${item.id}`}>{item.name}</li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  );
}
