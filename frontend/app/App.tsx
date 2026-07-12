import { useEffect, useState } from "react";
import { initTelegramWebApp } from "../telegram/init";
import { CatalogPage } from "../features/catalog/CatalogPage";

type Screen = "home" | "catalog";

export function App() {
  const [screen, setScreen] = useState<Screen>("home");

  useEffect(() => {
    initTelegramWebApp();
  }, []);

  if (screen === "catalog") {
    return (
      <main style={{ minHeight: "100vh", fontFamily: "system-ui, sans-serif" }}>
        <header
          style={{
            padding: "0.75rem 1rem",
            borderBottom: "1px solid #e5e5e5",
          }}
        >
          <button
            type="button"
            onClick={() => setScreen("home")}
            style={{
              border: "none",
              background: "transparent",
              color: "var(--tg-theme-link-color, #2481cc)",
              font: "inherit",
              cursor: "pointer",
              padding: 0,
            }}
          >
            ← Назад
          </button>
        </header>
        <CatalogPage />
      </main>
    );
  }

  return (
    <main
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "system-ui, sans-serif",
        padding: "1.5rem",
        textAlign: "center",
      }}
    >
      <div>
        <h1 style={{ margin: 0, fontSize: "1.75rem" }}>Техпроцессы ЧПУ</h1>
        <p style={{ marginTop: "0.75rem", color: "#555" }}>
          Telegram Mini App — этап 1: справочник инструмента
        </p>
        <button
          type="button"
          onClick={() => setScreen("catalog")}
          style={{
            marginTop: "1rem",
            border: "none",
            borderRadius: "0.5rem",
            padding: "0.625rem 1rem",
            background: "var(--tg-theme-button-color, #2481cc)",
            color: "var(--tg-theme-button-text-color, #fff)",
            font: "inherit",
            cursor: "pointer",
          }}
        >
          Открыть справочник
        </button>
      </div>
    </main>
  );
}
