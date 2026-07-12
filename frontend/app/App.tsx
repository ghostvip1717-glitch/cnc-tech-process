import { useEffect } from "react";
import { initTelegramWebApp } from "../telegram/init";

export function App() {
  useEffect(() => {
    initTelegramWebApp();
  }, []);

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
          Telegram Mini App — каркас проекта (этап 0)
        </p>
      </div>
    </main>
  );
}
