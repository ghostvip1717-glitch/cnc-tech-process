# State

- 2026-07-12: создан IMPLEMENTATION_PLAN.md — поэтапный план Telegram Mini App (техпроцессы ЧПУ)
- 2026-07-12: IMPLEMENTATION_PLAN.md в корне main на GitHub (ghostvip1717-glitch/cnc-tech-process)
- 2026-07-12: IMPLEMENTATION_PLAN.md — убраны сервисы/инфра/деплой, оставлены этапы 0–9
- 2026-07-12: этап 10 — развёртывание на GitHub (Pages + Actions)
- 2026-07-12: этап 0 — каркас backend (FastAPI, core, health) + frontend (React/Vite, TWA SDK), docker-compose postgres
- 2026-07-12: этап 1 — catalog: catalog_items, CRUD API /api/v1/catalog, экран справочника (вкладки tool/plate/jaw, поиск)
- 2026-07-12: этап 2 — parts: parts/part_photos, CRUD + upload фото в backend/uploads/, экраны списка и карточки
- 2026-07-12: этап 3 — tech_process: tech_processes/setups, API /parts/{id}/tech-process, блок установов на карточке детали
- 2026-07-12: этап 4 — operations: CRUD + reorder, таблица операций в установе, запрет удаления catalog item в ТП
- 2026-07-12: этап 5 — assembly: GET /parts/{id}/required-items, блок «Нужно для изготовления»
- 2026-07-12: этап 6 — главный экран: поиск+список деталей, нижняя навигация Детали|Инструмент, AppLayout
- 2026-07-12: этап 7 — telegram_auth: middleware X-Telegram-Init-Data, TELEGRAM_AUTH_ENABLED, api client wrapper
- 2026-07-12: этап 10 — GitHub Actions deploy-frontend (Pages) + deploy-api, VITE_BASE_PATH=/cnc-tech-process/, backend/Dockerfile
- 2026-07-12: fix Pages — статика Mini App в корне + docs/ (.nojekyll), merge stage-10 в main
- 2026-07-12: render.yaml для API; workflow синхронизирует статику с VITE_API_URL
- 2026-07-15: backend prod → Google Sheets + Apps Script + Drive (`sheets-backend/`); FastAPI `backend/` = архив; фронт client.ts = единый POST envelope на VITE_API_URL; auth/Script Properties; обновлены Readme/State/план
