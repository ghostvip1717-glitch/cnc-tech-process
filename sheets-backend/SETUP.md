# Google Sheets backend — установка и деплой

Единственный прод-backend Mini App. Контракт HTTP: [API.md](./API.md).

## Готово

| Что | Значение |
|-----|----------|
| Таблица | https://docs.google.com/spreadsheets/d/1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU/edit |
| SPREADSHEET_ID | Script Property (см. `CONFIG.env`) |
| Web App URL | GitHub Secret `VITE_API_URL` / Script deploy URL |
| Drive photos | Script Property `DRIVE_PHOTOS_FOLDER_ID` |

---

## Основной деплой: clasp + GitHub Actions

1. Создай Apps Script проект (или возьми существующий), скопируй **Script ID** (Project Settings).
2. Локально один раз: `npm i -g @google/clasp && clasp login` → файл `~/.clasprc.json`.
3. GitHub Secrets репозитория:
   - `CLASP_CREDENTIALS` — содержимое `~/.clasprc.json`
   - `GAS_SCRIPT_ID` — Script ID
   - `GAS_DEPLOYMENT_ID` — id Web App deployment (`clasp deployments`)
   - `VITE_API_URL` — URL `/exec` для фронта
4. Пуш в `main` по путям `sheets-backend/**` → workflow **Deploy Sheets Backend (clasp)** делает `clasp push` + `clasp deploy`.
5. Пример локального `.clasp.json`: скопируй [`.clasp.json.example`](./.clasp.json.example).

Модульные `.gs` — источник правды. `ONE_FILE.gs` генерируется (`npm run build:one-file`) только как **ручной fallback**.

### Локально

```bash
cd sheets-backend
npm ci
npm test
npm run lint
npm run build:one-file   # обновить ONE_FILE.gs fallback
clasp push --force
clasp deploy -i "$GAS_DEPLOYMENT_ID" -d "local"
```

---

## Fallback: ручная вставка ONE_FILE.gs

Если секреты clasp ещё не настроены:

1. Таблица → **Расширения → Apps Script**.
2. Вставь сгенерированный [`ONE_FILE.gs`](./ONE_FILE.gs) (или модули по таблице ниже).
3. Манифест [`appsscript.json`](./appsscript.json) → **Allow** Drive.
4. Script Properties → **Deploy → New version**.

| Файл в репо | Имя в Apps Script |
|-------------|-------------------|
| `Code.gs` | `Code` |
| `core/*.gs` | `Response`, `Auth`, `SheetStore` |
| `catalog/*` | `CatalogRepository`, `CatalogService` |
| `parts/*` | `PartsRepository`, `PartsService`, `Photos` |
| `tech_process/*` | `TechProcessRepository`, `TechProcessRules`, `TechProcessService` |
| `assembly/AssemblyService.gs` | `AssemblyService` |
| `setup/PrepareSpreadsheet.gs` | `PrepareSpreadsheet` |

---

## Script Properties

| Property | Назначение |
|----------|------------|
| `SPREADSHEET_ID` | ID таблицы |
| `DRIVE_PHOTOS_FOLDER_ID` | Папка фото |
| `BOT_TOKEN` | BotFather |
| `TELEGRAM_AUTH_ENABLED` | `false` / `true` |
| `TELEGRAM_ALLOWED_USER_IDS` | опционально |

### oauthScopes

См. `appsscript.json` (`spreadsheets`, `drive`, `script.container.ui`, `script.external_request`).

---

## Frontend (GitHub Pages)

Деплой только через **Actions → Deploy Frontend** (`actions/deploy-pages`), без коммита `dist` в репо.

В настройках репозитория: **Settings → Pages → Source = GitHub Actions**.

Secret: `VITE_API_URL` = URL `/exec`.

---

## HTTP-контракт

- GET: `.../exec?path=/health`
- POST: JSON envelope `{path,method,query,body,initData}`, `Content-Type: text/plain`
- Ответ: `{ ok, httpStatus, data|detail }`

Полный список маршрутов: [API.md](./API.md).

---

## Тесты / lint

```bash
cd sheets-backend && npm test && npm run lint
```

CI: `.github/workflows/ci.yml`.

---

## Чеклист

1. health → OK  
2. catalog + parts + photos  
3. установы I–X + операции + reorder  
4. required-items  
5. без валидного initData при `TELEGRAM_AUTH_ENABLED=true` → 401  
