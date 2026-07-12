import { useEffect, useState } from "react";
import { initTelegramWebApp } from "../telegram/init";
import { CatalogPage } from "../features/catalog/CatalogPage";
import { PartDetailPage } from "../features/parts/PartDetailPage";
import { PartsListPage } from "../features/parts/PartsListPage";

type Screen = "home" | "parts" | "part-detail" | "catalog";

const navButtonStyle = (active: boolean) => ({
  flex: 1,
  border: "none",
  borderRadius: "0.5rem",
  padding: "0.625rem 0.75rem",
  background: active ? "var(--tg-theme-button-color, #2481cc)" : "#e8e8e8",
  color: active ? "var(--tg-theme-button-text-color, #fff)" : "#222",
  font: "inherit",
  cursor: "pointer",
});

export function App() {
  const [screen, setScreen] = useState<Screen>("home");
  const [selectedPartId, setSelectedPartId] = useState<number | null>(null);

  useEffect(() => {
    initTelegramWebApp();
  }, []);

  const openPart = (partId: number) => {
    setSelectedPartId(partId);
    setScreen("part-detail");
  };

  const showNav = screen === "parts" || screen === "catalog" || screen === "part-detail";

  if (showNav) {
    return (
      <main style={{ minHeight: "100vh", fontFamily: "system-ui, sans-serif" }}>
        <header
          style={{
            padding: "0.75rem 1rem",
            borderBottom: "1px solid #e5e5e5",
            display: "grid",
            gap: "0.75rem",
          }}
        >
          <button
            type="button"
            onClick={() => {
              setScreen("home");
              setSelectedPartId(null);
            }}
            style={{
              border: "none",
              background: "transparent",
              color: "var(--tg-theme-link-color, #2481cc)",
              font: "inherit",
              cursor: "pointer",
              padding: 0,
              justifySelf: "start",
            }}
          >
            ← На главную
          </button>
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <button
              type="button"
              style={navButtonStyle(screen === "parts" || screen === "part-detail")}
              onClick={() => {
                setScreen("parts");
                setSelectedPartId(null);
              }}
            >
              Детали
            </button>
            <button
              type="button"
              style={navButtonStyle(screen === "catalog")}
              onClick={() => {
                setScreen("catalog");
                setSelectedPartId(null);
              }}
            >
              Справочник
            </button>
          </div>
        </header>

        {screen === "parts" && <PartsListPage onOpenPart={openPart} />}
        {screen === "part-detail" && selectedPartId !== null && (
          <PartDetailPage partId={selectedPartId} onBack={() => setScreen("parts")} />
        )}
        {screen === "catalog" && <CatalogPage />}
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
          Telegram Mini App — этап 2: детали с фото
        </p>
        <div style={{ display: "flex", gap: "0.75rem", justifyContent: "center", marginTop: "1rem" }}>
          <button
            type="button"
            onClick={() => setScreen("parts")}
            style={{
              border: "none",
              borderRadius: "0.5rem",
              padding: "0.625rem 1rem",
              background: "var(--tg-theme-button-color, #2481cc)",
              color: "var(--tg-theme-button-text-color, #fff)",
              font: "inherit",
              cursor: "pointer",
            }}
          >
            Детали
          </button>
          <button
            type="button"
            onClick={() => setScreen("catalog")}
            style={{
              border: "none",
              borderRadius: "0.5rem",
              padding: "0.625rem 1rem",
              background: "#e8e8e8",
              color: "#222",
              font: "inherit",
              cursor: "pointer",
            }}
          >
            Справочник
          </button>
        </div>
      </div>
    </main>
  );
}
