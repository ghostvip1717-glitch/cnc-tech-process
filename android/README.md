# Android offline APK — Техпроцессы ЧПУ

Полностью локальное Android-приложение (Kotlin + Jetpack Compose + Room).
Без сети, без Google Sheets/Drive, без Telegram.

## Навигация

- Старт: экран «Детали» (список).
- Боковая шторка (`ModalNavigationDrawer`) по ☰ на верхнеуровневых экранах:
  - **Детали**
  - **Инструмент** (вкладки tool / plate / jaw)
  - **Настройки** (экспорт/импорт ZIP)
- Переключение пункта шторки сбрасывает стек раздела.
- Внутри разделов — стек с «← Назад» (карточка детали, ТП, галереи и т.д.).

## Требования

- JDK 17+
- Android SDK (API 26–35)
- `local.properties` с `sdk.dir=...` (создаётся локально, не в git)

## Сборка debug APK

```bash
cd android
./gradlew assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

Готовая сборка в ветке PR также лежит в `android/dist/cnc-tech-process-debug.apk`.

## Установка на телефон

1. Скопируйте APK на телефон.
2. Откройте файл и установите (разрешите установку из неизвестных источников).
3. Пакет: `com.cnctech.process`

## Резервные копии

Экран **Настройки** в шторке: ZIP с `database/cnc_tech_process.db` и `photos/**`.
Имя файла: `cnc-tech-process-backup-yyyyMMdd-HHmmss.zip`.
