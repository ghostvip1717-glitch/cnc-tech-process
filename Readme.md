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

Открыть http://localhost:5173 → «Открыть справочник».

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
