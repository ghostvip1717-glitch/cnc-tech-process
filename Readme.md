# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

План реализации: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)

## Статус этапов

| Этап | Модуль | Статус |
|------|--------|--------|
| 0 | Каркас + health | готов |
| 1 | Справочник `catalog` | готов |
| 2 | Детали `parts` + фото | готов |
| 3 | Техпроцесс: установы | готов |
| 4 | Операции | готов |
| 5 | Сводка `assembly` | готов |
| 6 | Главный экран и навигация | готов |
| 7 | Telegram auth | готов |
| 8–9 | Копирование ТП, история | — |
| 10 | Деплой GitHub Pages | готов |

**Продакшен-хранилище:** Google Sheets + Apps Script + Drive.  
FastAPI/`backend/` — [архив](./backend/ARCHIVED.md), для MVP не нужен (Render/VPS/Postgres не требуются).

## Архитектура

```
Telegram Mini App (GitHub Pages)
        │
        ▼  HTTPS POST (JSON envelope)
Google Apps Script Web App
        │
        ├─► Google Sheets (листы = таблицы)
        └─► Google Drive  (фото деталей)
```

Код API: [`sheets-backend/`](./sheets-backend/)

---

## 1. Google Таблица + Apps Script

### Создать таблицу и скрипт

1. Создайте пустую Google Таблицу **или** в редакторе Apps Script выполните `createBlankSpreadsheetForProject` — создаст таблицу и папку фото, залогирует id.
2. **Extensions → Apps Script** (или отдельный standalone-проект).
3. Скопируйте файлы из `sheets-backend/`:
   - `Code.gs`, `Core.gs`, `SheetsStore.gs`
   - `Catalog.gs`, `Parts.gs`, `TechProcess.gs`, `Assembly.gs`
   - `InitSheets.gs`, `appsscript.json`
4. Выполните функцию `initializeSpreadsheet` — появятся листы из [sheets-template.md](./sheets-backend/sheets-template.md).

### Script Properties

**Project Settings → Script properties:**

| Property | Назначение |
|----------|------------|
| `SPREADSHEET_ID` | ID Google Таблицы |
| `DRIVE_FOLDER_ID` | ID папки Drive для фото |
| `BOT_TOKEN` | Токен бота (BotFather) |
| `TELEGRAM_AUTH_ENABLED` | `false` отладка / `true` прод |
| `TELEGRAM_ALLOWED_USER_IDS` | Опционально, через запятую |

### Деплой Web App

1. **Deploy → New deployment → Web app**
2. Execute as: **Me**
3. Who has access: **Anyone**
4. Deploy → скопировать URL вида  
   `https://script.google.com/macros/s/.../exec`

После изменений кода: **Manage deployments → Edit → New version**.

### Проверка API

```bash
# health (GET query)
curl "https://script.google.com/macros/s/XXXX/exec?path=/health&method=GET"

# или POST envelope
curl -X POST "https://script.google.com/macros/s/XXXX/exec" \
  -H "Content-Type: text/plain;charset=utf-8" \
  -d '{"path":"/health","method":"GET","query":{},"body":null}'
```

Ожидается: `{"ok":true,"httpStatus":200,"data":{"status":"OK"}}`

---

## 2. Frontend (GitHub Pages)

### Secret и пересборка

**GitHub → Settings → Secrets and variables → Actions:**

| Secret | Значение |
|--------|----------|
| `VITE_API_URL` | URL Web App `/exec` **без** слэша в конце |

Затем **Actions → Deploy Frontend → Run workflow**.

Mini App URL: `https://ghostvip1717-glitch.github.io/cnc-tech-process/`

### Локально

```bash
cd frontend
cp ../.env.example ../.env   # или задать VITE_API_URL
# в .env: VITE_API_URL=https://script.google.com/macros/s/XXXX/exec
npm install
npm run dev
```

Фронт ходит в Apps Script **одним** транспортом (`frontend/shared/api/client.ts`):  
POST на `VITE_API_URL` с телом `{ path, method, query, body, telegramInitData }`.  
Заголовок `X-Telegram-Init-Data` передаётся внутри envelope (CORS без preflight).

---

## 3. Telegram auth (этап 7)

- Фронт по-прежнему берёт `WebApp.initData`
- Apps Script: проверка HMAC в `Core.gs` (`BOT_TOKEN` из Properties)
- `TELEGRAM_AUTH_ENABLED=false` — API открыт (отладка)
- `true` + нет initData → **401** (`detail` в envelope)
- `GET /health` — без auth

```bash
# auth=true → 401 без initData
curl -X POST "https://script.google.com/macros/s/XXXX/exec" \
  -H "Content-Type: text/plain;charset=utf-8" \
  -d '{"path":"/api/v1/parts","method":"GET","query":{},"body":null}'
# → {"ok":false,"httpStatus":401,"detail":"Missing Telegram init data"}
```

---

## 4. Чеклист проверки

1. `health` → OK  
2. Создать catalog item (инструмент) + деталь «В-204 Втулка» — строки в таблице  
3. Загрузить фото — файл в папке Drive, `file_url` в `part_photos`  
4. Два установа + операции — листы `setups` / `operations`  
5. «Нужно для изготовления» (`required-items`) совпадает с ТП  
6. `TELEGRAM_AUTH_ENABLED=true` без initData → 401  

---

## API (контракты модулей)

Пути те же, что у прежнего FastAPI. Транспорт — envelope к Web App.

| Модуль | Пути |
|--------|------|
| health | `GET /health` |
| catalog | `GET/POST /api/v1/catalog`, `GET/PATCH/DELETE /api/v1/catalog/{id}` |
| parts | CRUD `/api/v1/parts`, photos upload/delete/reorder |
| tech_process | `/api/v1/parts/{id}/tech-process` + setups/operations |
| assembly | `GET /api/v1/parts/{id}/required-items` |

Фото: multipart на фронте → base64 в envelope → файл в Drive → `url` = публичная ссылка.

---

## Устаревшее (не для MVP)

| Путь | Статус |
|------|--------|
| `backend/` (FastAPI + PostgreSQL) | архив, см. [ARCHIVED.md](./backend/ARCHIVED.md) |
| `docker-compose.yml`, `render.yaml` | не нужны для Pages MVP |
| `.github/workflows/deploy-api.yml` | проверка архивного backend, внешний деплой не требуется |

## Стек (прод)

- API: Google Apps Script Web App  
- БД: Google Sheets  
- Фото: Google Drive  
- Frontend: React + Vite + `@twa-dev/sdk` на GitHub Pages  
