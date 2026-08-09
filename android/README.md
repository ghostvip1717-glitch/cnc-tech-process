# Android offline APK — Техпроцессы ЧПУ

Полностью локальное Android-приложение (Kotlin + Jetpack Compose + Room).
Без сети, без Google Sheets/Drive, без Telegram.

## Навигация

- Старт: экран «Детали».
- Шторка ☰: **Детали**, **Инструмент**, **Настройки**.
- Внутри разделов — стек с «← Назад».

## Темы

В **Настройки → Стиль приложения** — 8 тем (Индустриальный, Графит, OLED, Неон, Янтарь, Изумруд, Ультрафиолет, Светлый). Выбор сохраняется в SharedPreferences.

## Требования

- JDK 17+
- Android SDK (API 26–35)
- `local.properties` с `sdk.dir=...` (локально, не в git)

## Сборка debug APK

```bash
cd android
./gradlew assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`  
Готовая сборка: `android/dist/cnc-tech-process-debug.apk`

## Перенос данных из Telegram/Sheets

```bash
cd android/tools
pip install -r requirements.txt
python export_from_sheets.py --out ../dist/migration.zip
```

В приложении: **Настройки → Импортировать из старой версии** → выбрать `migration.zip`.

Готовый экспорт (снимок): `android/dist/migration.zip`.

## Резервные копии

**Настройки**: экспорт/импорт ZIP базы Room + фото (`cnc-tech-process-backup-*.zip`).
