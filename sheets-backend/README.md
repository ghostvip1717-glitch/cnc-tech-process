# Sheets backend (Google Apps Script)

Продакшен API для Mini App: Sheets = БД, Apps Script = HTTP API, Drive = фото.

## Быстрый старт

1. Создайте Google Таблицу (или выполните `createBlankSpreadsheetForProject` в редакторе).
2. Extensions → Apps Script → скопируйте все файлы из этой папки (`Code.gs`, `Core.gs`, …).
3. Project Settings → Script properties:

| Property | Значение |
|----------|----------|
| `SPREADSHEET_ID` | id таблицы |
| `DRIVE_FOLDER_ID` | id папки Drive для фото |
| `BOT_TOKEN` | токен бота |
| `TELEGRAM_AUTH_ENABLED` | `false` (отладка) / `true` (прод) |
| `TELEGRAM_ALLOWED_USER_IDS` | опционально, через запятую |

4. Выполните `initializeSpreadsheet`.
5. Deploy → New deployment → Web app: **Execute as Me**, **Anyone** → скопируйте URL `/exec`.
6. GitHub Secret `VITE_API_URL` = этот URL → пересборка Pages.

Подробности и чеклист — в корневом [Readme.md](../Readme.md).

Структура листов: [sheets-template.md](./sheets-template.md).
