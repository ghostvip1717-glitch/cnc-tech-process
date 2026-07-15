# Структура листов Google Sheets

Заголовки — первая строка. Данные со 2-й строки. Счётчики id — лист `meta`.

| Лист | Колонки |
|------|---------|
| `catalog_items` | `id`, `type`, `name`, `note` |
| `parts` | `id`, `number`, `title`, `created_at` |
| `part_photos` | `id`, `part_id`, `file_url`, `sort_order`, `drive_file_id` |
| `tech_processes` | `id`, `part_id` |
| `setups` | `id`, `tech_process_id`, `order`, `jaw_id` |
| `operations` | `id`, `setup_id`, `order`, `op_number`, `title`, `tool_id`, `plate_id`, `comment` |
| `meta` | `key`, `value` |

## meta — пример

| key | value |
|-----|-------|
| catalog_items | 0 |
| parts | 0 |
| part_photos | 0 |
| tech_processes | 0 |
| setups | 0 |
| operations | 0 |

Инициализация: в Apps Script выполнить `initializeSpreadsheet` или `createBlankSpreadsheetForProject`.

`type` в `catalog_items`: только `tool` \| `plate` \| `jaw`.
`file_url` — публичная ссылка Drive (`https://drive.google.com/uc?export=view&id=...`).
