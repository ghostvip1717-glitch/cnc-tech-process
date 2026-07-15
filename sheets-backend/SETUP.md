# Google Sheets backend — установка и деплой

## Готово

| Что | Значение |
|-----|----------|
| Таблица | https://docs.google.com/spreadsheets/d/1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU/edit |
| SPREADSHEET_ID | `1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU` |
| Web App URL | `https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec` |
| Листы | подготовлены (`prepareSpreadsheet`) |

См. также `CONFIG.env`.

---

## 1. Вставить код API в Apps Script (простой способ)

**Один файл:** [`ONE_FILE.gs`](./ONE_FILE.gs) — весь backend.

1. Таблица → **Расширения → Apps Script**.
2. Удали содержимое всех файлов; оставь один файл (например `Code`).
3. Открой [`ONE_FILE.gs`](https://raw.githubusercontent.com/ghostvip1717-glitch/cnc-tech-process/cursor/sheets-apps-script-backend-81bc/sheets-backend/ONE_FILE.gs) → скопируй всё → вставь → **Сохранить**.
4. Script Properties (см. ниже) → **Новая версия** Web App (доступ: Все).

### Альтернатива: по файлам

Удалить старые/лишние файлы, создать файлы **с теми же именами** (папки репо в GAS не обязательны — важен текст файла):

| Файл в репо | Имя в Apps Script |
|-------------|-------------------|
| `Code.gs` | `Code` |
| `core/Response.gs` | `Response` |
| `core/Auth.gs` | `Auth` |
| `core/SheetStore.gs` | `SheetStore` |
| `catalog/CatalogRepository.gs` | `CatalogRepository` |
| `catalog/CatalogService.gs` | `CatalogService` |
| `parts/PartsRepository.gs` | `PartsRepository` |
| `parts/PartsService.gs` | `PartsService` |
| `parts/Photos.gs` | `Photos` |
| `tech_process/TechProcessRepository.gs` | `TechProcessRepository` |
| `tech_process/TechProcessService.gs` | `TechProcessService` |
| `assembly/AssemblyService.gs` | `AssemblyService` |
| `setup/PrepareSpreadsheet.gs` | `PrepareSpreadsheet` (уже был) |

3. Сохранить проект (Ctrl+S).

---

## 2. Script Properties

**Проект → Параметры проекта → Свойства скрипта:**

| Property | Значение |
|----------|----------|
| `SPREADSHEET_ID` | `1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU` |
| `DRIVE_PHOTOS_FOLDER_ID` | `1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC` |
| `BOT_TOKEN` | токен BotFather |
| `TELEGRAM_AUTH_ENABLED` | `false` (отладка) / `true` (прод) |
| `TELEGRAM_ALLOWED_USER_IDS` | опционально, через запятую |

### Папка Drive для фото

Готовая папка: https://drive.google.com/drive/folders/1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC

ID: `1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC` → свойство `DRIVE_PHOTOS_FOLDER_ID`.

Apps Script должен выполняться **от твоего имени** (Execute as: Me), чтобы писать файлы в эту папку.

---

## 3. Новая версия Web App (обязательно после правок!)

URL `/exec` **не меняется**, но код не подхватится без новой версии:

1. **Deploy → Manage deployments**
2. Карандаш (Edit) у существующего Web App
3. Version: **New version**
4. Execute as: **Me**
5. Who has access: **Anyone**
6. **Deploy**

Проверка:

```bash
curl -sL "https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec?path=/health"
# → {"ok":true,"httpStatus":200,"data":{"status":"OK"}}
```

---

## 4. Frontend (GitHub Pages)

Secret Actions:

- Name: `VITE_API_URL`
- Value: `https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec`

Затем **Actions → Deploy Frontend → Run workflow**.

---

## 5. HTTP-контракт

Единый формат:

- GET: `.../exec?path=/health`
- POST: body JSON `{"path","method","query","body","initData"}`, `Content-Type: text/plain` (без CORS preflight)

Ответ всегда envelope: `{ ok, httpStatus, data|detail }`.

---

## Чеклист

1. health → OK  
2. catalog: «CNMG 120408», «Кулачки высота 15»  
3. part «В-204 Втулка» + 2 фото в Drive  
4. 2 установа + операции  
5. required-items совпадает  
6. `TELEGRAM_AUTH_ENABLED=true` без initData → 401  
