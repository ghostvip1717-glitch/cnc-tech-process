/**
 * Spreadsheet repository helpers: sheets as tables, meta id counters.
 */

function getSpreadsheet_() {
  var config = getConfig_();
  if (!config.spreadsheetId) {
    throw new HttpError_(500, 'SPREADSHEET_ID is not configured');
  }
  return SpreadsheetApp.openById(config.spreadsheetId);
}

function getSheetByName_(name) {
  var sheet = getSpreadsheet_().getSheetByName(name);
  if (!sheet) {
    throw new HttpError_(500, 'Sheet not found: ' + name);
  }
  return sheet;
}

function ensureSheetsInitialized_() {
  var ss = getSpreadsheet_();
  Object.keys(SHEET_HEADERS).forEach(function (name) {
    var sheet = ss.getSheetByName(name);
    if (!sheet) {
      sheet = ss.insertSheet(name);
      sheet.appendRow(SHEET_HEADERS[name]);
    } else if (sheet.getLastRow() === 0) {
      sheet.appendRow(SHEET_HEADERS[name]);
    }
  });

  var meta = ss.getSheetByName(SHEET_NAMES.META);
  var keys = [
    SHEET_NAMES.CATALOG,
    SHEET_NAMES.PARTS,
    SHEET_NAMES.PART_PHOTOS,
    SHEET_NAMES.TECH_PROCESSES,
    SHEET_NAMES.SETUPS,
    SHEET_NAMES.OPERATIONS,
  ];
  keys.forEach(function (key) {
    if (getMetaValue_(key) === null) {
      setMetaValue_(key, 0);
    }
  });
}

function getMetaValue_(key) {
  var sheet = getSheetByName_(SHEET_NAMES.META);
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) {
      return values[i][1];
    }
  }
  return null;
}

function setMetaValue_(key, value) {
  var sheet = getSheetByName_(SHEET_NAMES.META);
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) {
      sheet.getRange(i + 1, 2).setValue(value);
      return;
    }
  }
  sheet.appendRow([key, value]);
}

function nextId_(counterKey) {
  var current = Number(getMetaValue_(counterKey) || 0);
  if (isNaN(current)) {
    current = 0;
  }
  var next = current + 1;
  setMetaValue_(counterKey, next);
  return next;
}

function rowsToObjects_(sheetName) {
  var sheet = getSheetByName_(sheetName);
  var values = sheet.getDataRange().getValues();
  if (values.length < 2) {
    return [];
  }
  var headers = values[0].map(function (h) {
    return String(h);
  });
  var rows = [];
  for (var r = 1; r < values.length; r++) {
    var row = values[r];
    if (row[0] === '' || row[0] === null) {
      continue;
    }
    var obj = { __row: r + 1 };
    for (var c = 0; c < headers.length; c++) {
      obj[headers[c]] = row[c];
    }
    rows.push(obj);
  }
  return rows;
}

function findById_(sheetName, id) {
  var rows = rowsToObjects_(sheetName);
  for (var i = 0; i < rows.length; i++) {
    if (Number(rows[i].id) === Number(id)) {
      return rows[i];
    }
  }
  return null;
}

function appendObject_(sheetName, obj) {
  var headers = SHEET_HEADERS[sheetName];
  var sheet = getSheetByName_(sheetName);
  var row = headers.map(function (key) {
    return obj[key] === undefined ? '' : obj[key];
  });
  sheet.appendRow(row);
}

function updateObject_(sheetName, rowNumber, obj) {
  var headers = SHEET_HEADERS[sheetName];
  var sheet = getSheetByName_(sheetName);
  var values = headers.map(function (key) {
    return obj[key] === undefined ? '' : obj[key];
  });
  sheet.getRange(rowNumber, 1, 1, headers.length).setValues([values]);
}

function deleteRow_(sheetName, rowNumber) {
  getSheetByName_(sheetName).deleteRow(rowNumber);
}

function emptyToNull_(value) {
  if (value === '' || value === undefined) {
    return null;
  }
  return value;
}

function toInt_(value) {
  return Number(value);
}
