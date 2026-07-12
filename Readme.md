# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

План реализации: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)

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

Открыть http://localhost:5173 → навигация «Детали» / «Справочник».

## API деталей (этап 2)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/parts` | Список (`?q=поиск` по номеру/названию) |
| POST | `/api/v1/parts` | Создание `{number, title}` |
| GET/PATCH/DELETE | `/api/v1/parts/{id}` | Карточка детали |
| POST | `/api/v1/parts/{id}/photos` | Upload фото (`multipart/form-data`, поле `file`) |
| DELETE | `/api/v1/parts/{id}/photos/{photo_id}` | Удаление фото |
| PATCH | `/api/v1/parts/{id}/photos/reorder` | `{photo_ids: [id, ...]}` |

Фото хранятся в `backend/uploads/parts/{part_id}/`, отдаются по `/uploads/...`.

Проверка:

```bash
curl -X POST http://localhost:8000/api/v1/parts -H 'Content-Type: application/json' \
  -d '{"number":"В-204","title":"Втулка"}'
curl -X POST http://localhost:8000/api/v1/parts/1/photos -F 'file=@photo.jpg;type=image/jpeg'
curl 'http://localhost:8000/api/v1/parts?q=В-204'
```

## API справочника (этап 1)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/catalog` | Список (`?type=tool\|plate\|jaw`, `?q=поиск`) |
| POST | `/api/v1/catalog` | Создание `{type, name, note?}` |
| GET | `/api/v1/catalog/{id}` | Одна позиция |
| PATCH | `/api/v1/catalog/{id}` | Изменение `{name?, note?}` |
| DELETE | `/api/v1/catalog/{id}` | Удаление |

Проверка:

```bash
curl -X POST http://localhost:8000/api/v1/catalog -H 'Content-Type: application/json' \
  -d '{"type":"tool","name":"CNMG 120408"}'
curl 'http://localhost:8000/api/v1/catalog?q=CNMG'
```

## Стек

- API: FastAPI + PostgreSQL
- Frontend: React + Vite + `@twa-dev/sdk`
