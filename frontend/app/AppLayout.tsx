import type { ReactNode } from "react";
import { IconBack } from "../shared/ui";
import "./app.css";

interface AppLayoutProps {
  title: string;
  showBack?: boolean;
  onBack?: () => void;
  children: ReactNode;
}

export function AppLayout({ title, showBack = false, onBack, children }: AppLayoutProps) {
  return (
    <div className="app-shell">
      <header className="app-header">
        {showBack && onBack ? (
          <button className="app-header-back" type="button" onClick={onBack} aria-label="Назад">
            <IconBack size={22} />
            <span>Назад</span>
          </button>
        ) : null}
        <h1>{title}</h1>
      </header>

      <main className="app-content">{children}</main>
    </div>
  );
}
