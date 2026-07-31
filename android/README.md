# Android offline APK — Техпроцессы ЧПУ

Полностью локальное Android-приложение (Kotlin + Jetpack Compose + Room).
Без сети, без Google Sheets/Drive, без Telegram.

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

## Установка на телефон

1. Скопируйте `app-debug.apk` на телефон.
2. Откройте файл и установите (разрешите установку из неизвестных источников).
3. Пакет: `com.cnctech.process`

## Резервные копии

Экран «Резервная копия» с хаба: ZIP с `database/cnc_tech_process.db` и `photos/**`.
Имя файла: `cnc-tech-process-backup-yyyyMMdd-HHmmss.zip`.
