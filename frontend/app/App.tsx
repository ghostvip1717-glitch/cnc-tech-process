import { useEffect, useState } from "react";
import { initTelegramWebApp } from "../telegram/init";
import { CatalogPage } from "../features/catalog/CatalogPage";
import { PartDetailPage } from "../features/parts/PartDetailPage";
import { PartEditPage } from "../features/parts/PartEditPage";
import { PartGalleryPage } from "../features/parts/PartGalleryPage";
import { PartsListPage } from "../features/parts/PartsListPage";
import { AssemblyPage } from "../features/assembly/AssemblyPage";
import { SetupPage } from "../features/tech-process/SetupPage";
import { TechProcessPage } from "../features/tech-process/TechProcessPage";
import { ToastProvider } from "../shared/ui";
import { warmUpApi } from "../shared/api/client";
import { AppLayout } from "./AppLayout";
import { HomePage } from "./HomePage";
import { routeTitle, type Route } from "./navigation";
import "./app.css";

export function App() {
  const [stack, setStack] = useState<Route[]>([{ screen: "hub" }]);
  const route = stack[stack.length - 1] ?? { screen: "hub" as const };

  useEffect(() => {
    initTelegramWebApp();
    warmUpApi();
  }, []);

  const push = (next: Route) => setStack((prev) => [...prev, next]);
  const pop = () => setStack((prev) => (prev.length > 1 ? prev.slice(0, -1) : prev));

  return (
    <ToastProvider>
      <AppLayout title={routeTitle(route)} showBack={stack.length > 1} onBack={pop}>
        {route.screen === "hub" && (
          <HomePage
            onOpenParts={() => push({ screen: "parts" })}
            onOpenCatalog={() => push({ screen: "catalog" })}
          />
        )}

        {route.screen === "parts" && (
          <PartsListPage onOpenPart={(partId) => push({ screen: "part", partId })} />
        )}

        {route.screen === "part" && (
          <PartDetailPage
            partId={route.partId}
            onEdit={() => push({ screen: "part-edit", partId: route.partId })}
            onOpenGallery={() => push({ screen: "part-gallery", partId: route.partId })}
            onOpenTechProcess={() => push({ screen: "tech-process", partId: route.partId })}
            onOpenAssembly={() => push({ screen: "assembly", partId: route.partId })}
          />
        )}

        {route.screen === "part-edit" && (
          <PartEditPage
            partId={route.partId}
            onSaved={() => pop()}
            onDeleted={() => {
              setStack((prev) => {
                const withoutEdit = prev.slice(0, -1);
                if (withoutEdit[withoutEdit.length - 1]?.screen === "part") {
                  return withoutEdit.slice(0, -1);
                }
                return withoutEdit;
              });
            }}
          />
        )}

        {route.screen === "part-gallery" && <PartGalleryPage partId={route.partId} />}

        {route.screen === "tech-process" && (
          <TechProcessPage
            partId={route.partId}
            onOpenSetup={(setupId) =>
              push({ screen: "setup", partId: route.partId, setupId })
            }
          />
        )}

        {route.screen === "setup" && (
          <SetupPage partId={route.partId} setupId={route.setupId} onDeleted={pop} />
        )}

        {route.screen === "assembly" && <AssemblyPage partId={route.partId} />}

        {route.screen === "catalog" && <CatalogPage />}
      </AppLayout>
    </ToastProvider>
  );
}
