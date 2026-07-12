# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

План реализации: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)

## Статус этапов

| Этап | Модуль | Статус |
|------|--------|--------|
| 0 | Каркас FastAPI + React/Vite | готов |
| 1 | Справочник `catalog` | готов |
| 2 | Детали `parts` + фото | готов |
| 3 | Техпроцесс: установы | готов |
| 4 | Операции | готов |
| 5 | Сводка `assembly` | готов |
| 6 | Главный экран и навигация | готов |
| 7 | Telegram auth | готов |
| 8–9 | Копирование ТП, история | — |
| 10 | Деплой GitHub Pages + Actions | — |

## Локальный запуск

### 1. PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Backend (FastAPI)

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp ../.env.example ../.env
uvicorn main:app --reload --app-dir .
```

Проверка: `curl http://localhost:8000/health` → `{"status":"OK"}`

### 3. Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Открыть http://localhost:5173 → навигация «Детали» / «Инструмент».

## Telegram auth (этап 7)

По умолчанию для локальной разработки auth **выключен**:

```env
TELEGRAM_AUTH_ENABLED=false
```

Для продакшена:

```env
BOT_TOKEN=<токен бота из BotFather>
TELEGRAM_AUTH_ENABLED=true
# опционально — только эти telegram user id:
TELEGRAM_ALLOWED_USER_IDS=123456789
```

- Фронт передаёт `WebApp.initData` в заголовке `X-Telegram-Init-Data` (общий клиент `frontend/shared/api/client.ts`)
- Бэкенд проверяет подпись через `BOT_TOKEN` (`backend/core/telegram_auth.py`)
- `GET /health` и статика `/uploads/` — без auth
- При `TELEGRAM_AUTH_ENABLED=true` запрос к `/api/v1/*` без заголовка → **401**

Проверка:

```bash
# auth выключен — OK
curl http://localhost:8000/api/v1/parts

# auth включён — 401 без заголовка
TELEGRAM_AUTH_ENABLED=true BOT_TOKEN=xxx uvicorn main:app --app-dir backend
curl -i http://localhost:8000/api/v1/parts
```

## API

### Справочник (`/api/v1/catalog`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/catalog` | Список (`?type=tool\|plate\|jaw`, `?q=`) |
| POST | `/api/v1/catalog` | `{type, name, note?}` |
| GET/PATCH/DELETE | `/api/v1/catalog/{id}` | CRUD |

### Детали (`/api/v1/parts`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET/POST | `/api/v1/parts` | Список (`?q=`) / создание |
| GET/PATCH/DELETE | `/api/v1/parts/{id}` | Карточка |
| POST | `/api/v1/parts/{id}/photos` | Upload фото (`file`) |
| DELETE | `/api/v1/parts/{id}/photos/{photo_id}` | Удаление фото |
| PATCH | `/api/v1/parts/{id}/photos/reorder` | `{photo_ids}` |

Фото: `backend/uploads/parts/{part_id}/`, URL `/uploads/...`

### Техпроцесс (`/api/v1/parts/{id}/tech-process`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET/PUT | `.../tech-process` | Получить / создать |
| POST/PATCH/DELETE | `.../setups/{setup_id}` | Установы (`jaw_id`) |
| POST/PATCH/DELETE | `.../operations/{op_id}` | Операции |
| PATCH | `.../setups/{setup_id}/operations/reorder` | Порядок операций |

### Сводка (`/api/v1/parts/{id}/required-items`)

Уникальные инструмент, пластины, кулачки для детали.

## Стек

- API: FastAPI + PostgreSQL
- Frontend: React + Vite + `@twa-dev/sdk`
