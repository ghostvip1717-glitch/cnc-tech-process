import { PartsListPage } from "../features/parts/PartsListPage";
import "./app.css";

interface HomePageProps {
  onOpenPart: (partId: number) => void;
  onOpenCatalog: () => void;
}

export function HomePage({ onOpenPart, onOpenCatalog }: HomePageProps) {
  return (
    <div>
      <div className="app-home-toolbar">
        <button className="app-catalog-button" type="button" onClick={onOpenCatalog}>
          Инструмент
        </button>
      </div>
      <PartsListPage onOpenPart={onOpenPart} showHeading={false} />
    </div>
  );
}
