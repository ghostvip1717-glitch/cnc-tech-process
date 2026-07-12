import { useEffect, useState } from "react";
import { initTelegramWebApp } from "../telegram/init";
import { CatalogPage } from "../features/catalog/CatalogPage";
import { PartDetailPage } from "../features/parts/PartDetailPage";
import { AppLayout, type AppTab } from "./AppLayout";
import { HomePage } from "./HomePage";
import "./app.css";

export function App() {
  const [tab, setTab] = useState<AppTab>("parts");
  const [selectedPartId, setSelectedPartId] = useState<number | null>(null);

  useEffect(() => {
    initTelegramWebApp();
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
      {tab === "parts" && selectedPartId === null && (
        <HomePage onOpenPart={openPart} onOpenCatalog={() => setTab("catalog")} />
      )}
      {isPartDetail && (
        <PartDetailPage partId={selectedPartId} onBack={() => setSelectedPartId(null)} />
      )}
      {tab === "catalog" && <CatalogPage showHeading={false} />}
    </AppLayout>
  );
}
