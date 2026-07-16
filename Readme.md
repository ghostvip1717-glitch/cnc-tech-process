# Техпроцессы ЧПУ

Telegram Mini App для учёта техпроцессов на токарном ЧПУ.

План: [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)  
Установка API: [sheets-backend/SETUP.md](./sheets-backend/SETUP.md)

## Статус

| Этап | Модуль | Статус |
|------|--------|--------|
| 0–7, 10 | UI + auth + Pages | готов |
| API prod | Google Sheets + Apps Script | готов в репо → вставить в GAS + New version |
| 8–9 | Копирование ТП, история | — |

**Прод-путь:** Pages → Apps Script `/exec` → Sheets (+ Drive фото).  
Единственный backend: `sheets-backend/` (Apps Script + Sheets + Drive). Контракт: [sheets-backend/API.md](./sheets-backend/API.md).

## Готовые ресурсы

| Что | Значение |
|-----|----------|
| Таблица | https://docs.google.com/spreadsheets/d/1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU/edit |
| SPREADSHEET_ID | `1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU` |
| Web App | `https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec` |

Конфиг: `sheets-backend/CONFIG.env`.

---

## Деплой Apps Script (после правок кода)

1. Таблица → **Расширения → Apps Script** → вставить [`ONE_FILE.gs`](./sheets-backend/ONE_FILE.gs) (или файлы из `sheets-backend/`, см. [SETUP.md](./sheets-backend/SETUP.md)).
2. Включить манифест → вставить [`appsscript.json`](./sheets-backend/appsscript.json) с `oauthScopes` (`spreadsheets` + **`drive`** + `script.container.ui`) → **Сохранить**.
3. При запросе прав → **Allow / разрешить доступ к Google Drive** (без этого upload фото → 500).
4. Script Properties: `SPREADSHEET_ID`, `DRIVE_PHOTOS_FOLDER_ID`, `BOT_TOKEN`, `TELEGRAM_AUTH_ENABLED`.
5. **Deploy → Manage deployments → Edit → Version: New version → Deploy**.
6. GitHub Secret `VITE_API_URL` = URL `/exec` → **Actions → Deploy Frontend**.

### Папка фото Drive

Готова: https://drive.google.com/drive/folders/1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC  
Property: `DRIVE_PHOTOS_FOLDER_ID` = `1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC`.

После New version: upload фото → файл в папке + строка в `part_photos`.

### Проверка

```bash
WEB=https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec

curl -sL "$WEB?path=/health"
# {"ok":true,"httpStatus":200,"data":{"status":"OK"}}

curl -sL -X POST "$WEB" -H "Content-Type: text/plain;charset=utf-8" \
  -d '{"path":"/api/v1/catalog","method":"POST","query":{},"body":{"type":"tool","name":"CNMG 120408"}}'
```

Фото (после Allow Drive + New version): `POST` envelope `path=/api/v1/parts/{id}/photos`, body `{fileName,mimeType,contentBase64}` → `201` + файл в Drive.
---

## Frontend

Единый клиент: `frontend/shared/api/client.ts` — POST envelope  
`{ path, method, query, body, initData }` на `VITE_API_URL`.

```bash
cd frontend
# VITE_API_URL из .env.example
npm install
npm run dev
```

## Auth

- `TELEGRAM_AUTH_ENABLED=false` — отладка
- `true` + нет `initData` → 401
- `/health` без auth

## Чеклист

1. health OK  
2. catalog: CNMG 120408 + Кулачки высота 15  
3. part В-204 Втулка + 2 фото  
4. 2 установа + операции  
5. required-items совпадает  
6. auth enabled без initData → 401  

## Структура API в репо

```
sheets-backend/
├── ONE_FILE.gs          # весь API для вставки в GAS
├── appsscript.json      # oauthScopes: spreadsheets + drive + ui
├── Code.gs
├── CONFIG.env
├── SETUP.md
├── setup/PrepareSpreadsheet.gs
├── core/{Auth,SheetStore,Response}.gs
├── catalog/{CatalogRepository,CatalogService}.gs
├── parts/{PartsRepository,PartsService,Photos}.gs
├── tech_process/{TechProcessRepository,TechProcessService}.gs
└── assembly/AssemblyService.gs
```

id из листа `meta` (`next_*_id`).
