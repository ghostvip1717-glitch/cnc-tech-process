# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

План реализации: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)

## Локальный запуск (этап 0)

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

Открыть http://localhost:5173 — заглушка «Техпроцессы ЧПУ».

## Стек

- API: FastAPI + PostgreSQL
- Frontend: React + Vite + `@twa-dev/sdk`
