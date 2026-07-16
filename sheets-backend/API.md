# HTTP API (Apps Script Web App)

Единственный прод-контракт. Транспорт: POST envelope `text/plain` на URL `/exec`
(или GET `?path=...` для простых запросов).

## Envelope

```json
{
  "path": "/api/v1/catalog",
  "method": "GET",
  "query": {},
  "body": null,
  "initData": null
}
```

Ответ всегда:

```json
{ "ok": true, "httpStatus": 200, "data": ... }
{ "ok": false, "httpStatus": 401, "detail": "..." }
```

Apps Script наружу часто отдаёт HTTP 200; статус приложения — в `httpStatus`.

## Маршруты

| Method | Path | Описание |
|--------|------|----------|
| GET | `/health` | Health (без auth) |
| GET | `/api/v1/catalog` | Список (`type`, `q`) |
| POST | `/api/v1/catalog` | Создать `{type,name,note?}` |
| GET | `/api/v1/catalog/{itemId}` | Получить |
| PATCH | `/api/v1/catalog/{itemId}` | Обновить |
| DELETE | `/api/v1/catalog/{itemId}` | Удалить (409 если используется) |
| GET | `/api/v1/parts` | Список (`q`) |
| POST | `/api/v1/parts` | Создать `{number,title}` |
| GET | `/api/v1/parts/{partId}` | Карточка + фото |
| PATCH | `/api/v1/parts/{partId}` | Обновить |
| DELETE | `/api/v1/parts/{partId}` | Удалить (каскад ТП/фото) |
| POST | `/api/v1/parts/{partId}/photos` | Upload `{fileName,mimeType,contentBase64}` |
| PATCH | `/api/v1/parts/{partId}/photos/reorder` | `{ordered_ids:[...]}` |
| DELETE | `/api/v1/parts/{partId}/photos/{photoId}` | Удалить фото |
| GET | `/api/v1/parts/{partId}/tech-process` | ТП + setups + operations |
| PUT | `/api/v1/parts/{partId}/tech-process` | Создать пустой ТП |
| POST | `/api/v1/parts/{partId}/tech-process/setups` | Добавить установ `{jaw_id?}` |
| PATCH | `/api/v1/parts/{partId}/tech-process/setups/{setupId}` | Обновить установ |
| DELETE | `/api/v1/parts/{partId}/tech-process/setups/{setupId}` | Удалить установ (+ ops) |
| POST | `.../setups/{setupId}/operations` | Создать операцию |
| PATCH | `.../setups/{setupId}/operations/reorder` | `{ordered_ids:[...]}` |
| PATCH | `.../operations/{operationId}` | Обновить операцию |
| DELETE | `.../operations/{operationId}` | Удалить операцию |
| GET | `/api/v1/parts/{partId}/required-items` | Сводка tool/plate/jaw |

## Не реализовано (план)

- `POST /api/v1/parts/{id}/copy-tech-process-from/{source_part_id}` — копирование ТП
- audit log / история изменений
