import { IconParts, IconTool } from "../shared/ui";
import "./app.css";

interface HomePageProps {
  onOpenParts: () => void;
  onOpenCatalog: () => void;
}

export function HomePage({ onOpenParts, onOpenCatalog }: HomePageProps) {
  return (
    <section className="app-hub">
      <p className="app-hub-lead">Выберите раздел</p>
      <div className="app-hub-grid">
        <button className="app-hub-tile" type="button" onClick={onOpenParts}>
          <span className="app-hub-tile-icon" aria-hidden>
            <IconParts />
          </span>
          <span className="app-hub-tile-title">Детали</span>
          <span className="app-hub-tile-text">Карточки, техпроцесс, сборка</span>
        </button>
        <button className="app-hub-tile" type="button" onClick={onOpenCatalog}>
          <span className="app-hub-tile-icon" aria-hidden>
            <IconTool />
          </span>
          <span className="app-hub-tile-title">Инструмент</span>
          <span className="app-hub-tile-text">Справочник, пластины, кулачки</span>
        </button>
      </div>
    </section>
  );
}
