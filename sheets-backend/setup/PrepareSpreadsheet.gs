/**
 * Подготовка Google Таблицы под техпроцессы ЧПУ.
 *
 * Таблица: https://docs.google.com/spreadsheets/d/1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU
 *
 * КАК ЗАПУСТИТЬ (один раз):
 * 1. Открыть таблицу по ссылке выше (под своим Google-аккаунтом).
 * 2. Расширения → Apps Script.
 * 3. Удалить код по умолчанию, вставить этот файл целиком.
 * 4. Сохранить → выбрать функцию prepareSpreadsheet → Выполнить.
 * 5. Разрешить доступ при первом запуске.
 * 6. В таблице появятся листы с заголовками.
 *
 * Повторный запуск безопасен: не удаляет данные, только досоздаёт листы
 * и пишет заголовки в пустую первую строку.
 */

var SPREADSHEET_ID = '1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU';

var SHEET_SPECS = [
  {
    name: 'catalog_items',
    headers: ['id', 'type', 'name', 'note'],
    note: 'Справочник: type = tool | plate | jaw',
  },
  {
    name: 'parts',
    headers: ['id', 'number', 'title', 'created_at'],
    note: 'Детали',
  },
  {
    name: 'part_photos',
    headers: ['id', 'part_id', 'file_url', 'sort_order'],
    note: 'Фото деталей (ссылка Google Drive)',
  },
  {
    name: 'tech_processes',
    headers: ['id', 'part_id'],
    note: 'Один техпроцесс на деталь (part_id уникален)',
  },
  {
    name: 'setups',
    headers: ['id', 'tech_process_id', 'order', 'jaw_id'],
    note: 'Установы; jaw_id → catalog_items.id (type=jaw)',
  },
  {
    name: 'operations',
    headers: ['id', 'setup_id', 'order', 'op_number', 'title', 'tool_id', 'plate_id', 'comment'],
    note: 'Операции; tool_id/plate_id → catalog_items',
  },
  {
    name: 'meta',
    headers: ['key', 'value'],
    note: 'Счётчики id и служебные ключи',
    seedRows: [
      ['next_catalog_id', '1'],
      ['next_part_id', '1'],
      ['next_part_photo_id', '1'],
      ['next_tech_process_id', '1'],
      ['next_setup_id', '1'],
      ['next_operation_id', '1'],
      ['spreadsheet_prepared_at', ''],
    ],
  },
];

/**
 * Главная функция — запустите её из редактора Apps Script.
 */
function prepareSpreadsheet() {
  var ss = getTargetSpreadsheet_();
  var created = [];
  var updated = [];

  SHEET_SPECS.forEach(function (spec) {
    var sheet = ss.getSheetByName(spec.name);
    if (!sheet) {
      sheet = ss.insertSheet(spec.name);
      created.push(spec.name);
    } else {
      updated.push(spec.name);
    }

    ensureHeaders_(sheet, spec.headers);
    sheet.setFrozenRows(1);
    sheet.getRange(1, 1, 1, spec.headers.length).setFontWeight('bold');

    if (spec.name === 'catalog_items') {
      applyCatalogTypeValidation_(sheet);
    }

    if (spec.seedRows && sheet.getLastRow() < 2) {
      var rows = spec.seedRows.map(function (row) {
        return row.slice();
      });
      rows.forEach(function (row) {
        if (row[0] === 'spreadsheet_prepared_at') {
          row[1] = new Date().toISOString();
        }
      });
      sheet.getRange(2, 1, rows.length, 2).setValues(rows);
    }
  });

  removeDefaultEmptySheets_(ss);

  var message =
    'Готово.\nСозданы: ' +
    (created.length ? created.join(', ') : '—') +
    '\nУже были: ' +
    (updated.length ? updated.join(', ') : '—');

  Logger.log(message);
  try {
    SpreadsheetApp.getUi().alert(message);
  } catch (e) {
    // Запуск не из UI таблицы — достаточно лога.
  }
}

function getTargetSpreadsheet_() {
  var active = SpreadsheetApp.getActiveSpreadsheet();
  if (active && active.getId() === SPREADSHEET_ID) {
    return active;
  }
  if (active) {
    return active;
  }
  return SpreadsheetApp.openById(SPREADSHEET_ID);
}

/**
 * Можно вызвать из меню таблицы после установки onOpen.
 */
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('Техпроцессы ЧПУ')
    .addItem('Подготовить структуру листов', 'prepareSpreadsheet')
    .addToUi();
}

function ensureHeaders_(sheet, headers) {
  var range = sheet.getRange(1, 1, 1, headers.length);
  var existing = range.getValues()[0];
  var empty = existing.every(function (cell) {
    return cell === '' || cell === null;
  });

  if (empty) {
    range.setValues([headers]);
    return;
  }

  // Если заголовки уже есть — не перезаписываем (защита данных).
  for (var i = 0; i < headers.length; i++) {
    if (String(existing[i]).trim() !== headers[i]) {
      throw new Error(
        'Лист "' +
          sheet.getName() +
          '": строка 1 уже заполнена иначе, чем нужно. ' +
          'Ожидалось: ' +
          headers.join(', ') +
          '. Очистите первую строку или переименуйте лист и запустите снова.'
      );
    }
  }
}

function applyCatalogTypeValidation_(sheet) {
  var rule = SpreadsheetApp.newDataValidation()
    .requireValueInList(['tool', 'plate', 'jaw'], true)
    .setAllowInvalid(false)
    .setHelpText('type: tool | plate | jaw')
    .build();
  // Колонка B (type), строки 2..1000
  sheet.getRange('B2:B1000').setDataValidation(rule);
}

function removeDefaultEmptySheets_(ss) {
  var protectedNames = {};
  SHEET_SPECS.forEach(function (spec) {
    protectedNames[spec.name] = true;
  });

  ss.getSheets().forEach(function (sheet) {
    var name = sheet.getName();
    if (protectedNames[name]) {
      return;
    }
    // Удаляем пустой «Лист1» / «Sheet1» без данных
    if (sheet.getLastRow() === 0 || (sheet.getLastRow() === 1 && sheet.getLastColumn() === 0)) {
      if (ss.getSheets().length > 1) {
        ss.deleteSheet(sheet);
      }
      return;
    }
    if (
      (name === 'Лист1' || name === 'Sheet1') &&
      sheet.getLastRow() <= 1 &&
      String(sheet.getRange(1, 1).getValue()).trim() === ''
    ) {
      if (ss.getSheets().length > 1) {
        ss.deleteSheet(sheet);
      }
    }
  });
}
