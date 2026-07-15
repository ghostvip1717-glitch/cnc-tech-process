# Подготовка Google Таблицы

Таблица: https://docs.google.com/spreadsheets/d/1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU/edit?usp=sharing

## Один раз

1. Открыть таблицу (аккаунт владельца).
2. **Расширения → Apps Script**.
3. Вставить код из `PrepareSpreadsheet.gs`, сохранить.
4. Функция **`prepareSpreadsheet`** → **Выполнить** → разрешить доступ.
5. В таблице появятся листы с заголовками.

Либо после сохранения скрипта: в таблице меню **Техпроцессы ЧПУ → Подготовить структуру листов** (после обновления страницы).

## Листы

| Лист | Колонки |
|------|---------|
| `catalog_items` | id, type (tool\|plate\|jaw), name, note |
| `parts` | id, number, title, created_at |
| `part_photos` | id, part_id, file_url, sort_order |
| `tech_processes` | id, part_id |
| `setups` | id, tech_process_id, order, jaw_id |
| `operations` | id, setup_id, order, op_number, title, tool_id, plate_id, comment |
| `meta` | key, value (счётчики next_*_id) |

Повторный запуск не затирает данные: только создаёт недостающие листы и пишет заголовки, если строка 1 пустая.

ID таблицы зашит в скрипте: `1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU`.
