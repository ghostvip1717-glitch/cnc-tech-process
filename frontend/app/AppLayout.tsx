import type { ReactNode } from "react";
import "./app.css";

export type AppTab = "parts" | "catalog";

interface AppLayoutProps {
  title: string;
  activeTab: AppTab;
  onTabChange: (tab: AppTab) => void;
  showBack?: boolean;
  onBack?: () => void;
  children: ReactNode;
}

export function AppLayout({
  title,
  activeTab,
  onTabChange,
  showBack = false,
  onBack,
  children,
}: AppLayoutProps) {
  return (
    <div className="app-shell">
      <header className="app-header">
        {showBack && onBack ? (
          <button className="app-header-back" type="button" onClick={onBack}>
            ← Назад
          </button>
        ) : null}
        <h1>{title}</h1>
      </header>

      <main className="app-content">{children}</main>

      <nav className="app-bottom-nav">
        <button
          type="button"
          className={`app-nav-button ${activeTab === "parts" ? "active" : ""}`}
          onClick={() => onTabChange("parts")}
        >
          Детали
        </button>
        <button
          type="button"
          className={`app-nav-button ${activeTab === "catalog" ? "active" : ""}`}
          onClick={() => onTabChange("catalog")}
        >
          Инструмент
        </button>
      </nav>
    </div>
  );
}
