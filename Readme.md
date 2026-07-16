# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

- План: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)
- Backend setup: [sheets-backend/SETUP.md](./sheets-backend/SETUP.md)
- API: [sheets-backend/API.md](./sheets-backend/API.md)

## Архитектура (прод)

| Слой | Технология |
|------|------------|
| UI | React 19 + Vite → **GitHub Pages** (`actions/deploy-pages`) |
| API | **`sheets-backend/`** — Google Apps Script + Sheets + Drive |
| Auth | Telegram `initData` (Script Property `TELEGRAM_AUTH_ENABLED`) |

Транспорт фронта: единый POST envelope `{path,method,query,body,initData}` как `text/plain` на URL Web App `/exec` (обход CORS preflight). **Не менять** без явного запроса.

## Статус

| Этап | Модуль | Статус |
|------|--------|--------|
| 0–7, 10 | UI + Pages | готов |
| API | sheets-backend | готов |
| 8 | Копирование ТП между деталями | не сделано (вне этой доработки) |
| 9 | История изменений (audit) | не сделано (вне этой доработки) |

## Деплой

### Frontend

Пуш в `main` (`frontend/**`) → workflow **Deploy Frontend**.

Обязательно: **Settings → Pages → Source = GitHub Actions** (не branch `/`).

```bash
cd frontend && npm ci && npm run lint && npm run build
```

### Backend (Apps Script)

Пуш в `main` (`sheets-backend/**`) → workflow **Deploy Sheets Backend (clasp)** при настроенных secrets:

- `CLASP_CREDENTIALS`, `GAS_SCRIPT_ID`, `GAS_DEPLOYMENT_ID`

Подробности и fallback через `ONE_FILE.gs`: [SETUP.md](./sheets-backend/SETUP.md).

```bash
cd sheets-backend && npm ci && npm test && npm run lint
```

## Структура

```
frontend/           React Mini App
sheets-backend/     Apps Script API (единственный backend)
  pure/             чистые правила + тесты (vitest)
  ONE_FILE.gs       generated fallback для ручной вставки
.github/workflows/  ci.yml, deploy-frontend.yml, deploy-sheets.yml
```

## Auth (кратко)

- `TELEGRAM_AUTH_ENABLED=false` — отладка
- `true` + нет валидного `initData` → 401
- `/health` без auth
