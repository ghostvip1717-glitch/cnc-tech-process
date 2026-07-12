import { useCallback, useEffect, useState } from "react";
import { getRequiredItems, type RequiredItems } from "../../shared/api/assembly";
import "./assembly.css";

interface AssemblyBlockProps {
  partId: number;
}

export function AssemblyBlock({ partId }: AssemblyBlockProps) {
  const [items, setItems] = useState<RequiredItems | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getRequiredItems(partId);
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить сводку");
    } finally {
      setLoading(false);
    }
  }, [partId]);

  useEffect(() => {
    void loadItems();
  }, [loadItems]);

  if (loading) {
    return <p className="assembly-empty">Загрузка сводки...</p>;
  }

  if (error) {
    return <p className="assembly-error">{error}</p>;
  }

  if (!items) {
    return null;
  }

  const isEmpty =
    items.tools.length === 0 && items.plates.length === 0 && items.jaws.length === 0;

  return (
    <section className="assembly-block">
      <h2>Нужно для изготовления</h2>
      {isEmpty ? (
        <p className="assembly-empty">Нет данных — добавьте установы и операции</p>
      ) : (
        <>
          {items.tools.length > 0 && (
            <div className="assembly-group">
              <h3>Инструмент</h3>
              <ul>
                {items.tools.map((item) => (
                  <li key={`tool-${item.id}`}>{item.name}</li>
                ))}
              </ul>
            </div>
          )}
          {items.plates.length > 0 && (
            <div className="assembly-group">
              <h3>Пластины</h3>
              <ul>
                {items.plates.map((item) => (
                  <li key={`plate-${item.id}`}>{item.name}</li>
                ))}
              </ul>
            </div>
          )}
          {items.jaws.length > 0 && (
            <div className="assembly-group">
              <h3>Кулачки</h3>
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
