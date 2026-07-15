# State

- 2026-07-12: IMPLEMENTATION_PLAN + этапы 0–7, 10 (FastAPI/Pages)
- 2026-07-12: render.yaml / VITE_API_URL sync для старого API
- 2026-07-15: prepare Google Sheets — таблица `1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU`, листы+meta next_*_id, Web App `/exec` URL в CONFIG.env
- 2026-07-15: sheets-backend API модули (Code/core/catalog/parts/tech_process/assembly); контракт POST envelope path/method/query/body/initData; meta next_*_id; DRIVE_PHOTOS_FOLDER_ID; фронт client.ts → VITE_API_URL=/exec; FastAPI=архив
- 2026-07-15: DRIVE_PHOTOS_FOLDER_ID=1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC
- 2026-07-15: sheets-backend/ONE_FILE.gs — весь API одним файлом для вставки в Apps Script
- 2026-07-15: merge sheets backend в main; фронт с DEFAULT/VITE_API_URL → Apps Script /exec; sync Pages static
- 2026-07-15: audit — Pages+API OK; ТП/операции/assembly OK; фото Drive 500 (нет oauthScopes); auth prod+BotFather, этапы 8–9 открыты
- 2026-07-15: oauthScopes drive в манифесте смержены; upload всё ещё 500 на createFile — нужен authorizeDrive с createFile + Allow write
- 2026-07-15: фото переведены на Drive API (UrlFetch+OAuth); scope script.external_request; ONE_FILE пересобран
- 2026-07-15: photo upload OK — file 1lub7j5iy2fG4TvYWX0fDPr_jk4DLrJRj в Drive, part_photos id=1

- 2026-07-15: fix Drive photo upload — appsscript.json oauthScopes (spreadsheets+drive+script.container.ui); rebuild ONE_FILE.gs; SETUP/Readme: Allow Drive + New version
- 2026-07-15: speed — FE GET cache 60s + invalidate on mutate; tabs keepMounted; warmup health+parts+catalog; search debounce 350ms; GAS CacheService sheetRows_ 45s + invalidate on write; ONE_FILE sync; Pages rebuild
- 2026-07-15: policy — всегда мержить в main без спроса; Pages из main
- 2026-07-15: merge speed-up → main (PR#13); Pages deploy; GAS ONE_FILE ещё руками New version
- 2026-07-15: Pages LIVE `index-D4hph2i7.js` (CI sync); FE cache+keepMounted в проде; GAS sheet-cache не задеплоен пока нет New version
