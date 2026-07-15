import { useEffect, useState } from "react";
import { initTelegramWebApp } from "../telegram/init";
import { CatalogPage } from "../features/catalog/CatalogPage";
import { PartDetailPage } from "../features/parts/PartDetailPage";
import { warmUpApi } from "../shared/api/client";
import { AppLayout, type AppTab } from "./AppLayout";
import { HomePage } from "./HomePage";
import "./app.css";

export function App() {
  const [tab, setTab] = useState<AppTab>("parts");
  const [selectedPartId, setSelectedPartId] = useState<number | null>(null);

  useEffect(() => {
    initTelegramWebApp();
    warmUpApi();
  }, []);

  const openPart = (partId: number) => {
    setSelectedPartId(partId);
    setTab("parts");
  };

  const handleTabChange = (nextTab: AppTab) => {
    setTab(nextTab);
    if (nextTab === "parts") {
      setSelectedPartId(null);
    }
  };

  const isPartDetail = tab === "parts" && selectedPartId !== null;
  const showPartsList = tab === "parts" && selectedPartId === null;
  const showCatalog = tab === "catalog";

  const title = isPartDetail
    ? "Карточка детали"
    : tab === "catalog"
      ? "Справочник"
      : "Техпроцессы ЧПУ";

  return (
    <AppLayout
      title={title}
      activeTab={tab}
      onTabChange={handleTabChange}
      showBack={isPartDetail}
      onBack={() => setSelectedPartId(null)}
    >
      {/* Держим экраны смонтированными — без повторной загрузки при каждом клике */}
      <div style={{ display: showPartsList ? "block" : "none" }}>
        <HomePage onOpenPart={openPart} onOpenCatalog={() => setTab("catalog")} />
      </div>
      {selectedPartId !== null && (
        <div style={{ display: isPartDetail ? "block" : "none" }}>
          <PartDetailPage partId={selectedPartId} onBack={() => setSelectedPartId(null)} />
        </div>
      )}
      <div style={{ display: showCatalog ? "block" : "none" }}>
        <CatalogPage showHeading={false} />
      </div>
    </AppLayout>
  );
}
