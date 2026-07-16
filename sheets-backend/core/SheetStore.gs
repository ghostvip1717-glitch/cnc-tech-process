/**
 * Spreadsheet access: rows as objects, meta next_*_id counters.
 * Script Properties: SPREADSHEET_ID (optional if bound to the sheet).
 */

var DEFAULT_SPREADSHEET_ID = '1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU';

var SHEET_NAMES = {
  CATALOG: 'catalog_items',
  PARTS: 'parts',
  PART_PHOTOS: 'part_photos',
  TECH_PROCESSES: 'tech_processes',
  SETUPS: 'setups',
  OPERATIONS: 'operations',
  META: 'meta',
};

var SHEET_HEADERS = {
  catalog_items: ['id', 'type', 'name', 'note'],
  parts: ['id', 'number', 'title', 'created_at'],
  part_photos: ['id', 'part_id', 'file_url', 'sort_order'],
  tech_processes: ['id', 'part_id'],
  setups: ['id', 'tech_process_id', 'order', 'jaw_id'],
  operations: [
    'id',
    'setup_id',
    'order',
    'op_number',
    'title',
    'tool_id',
    'plate_id',
    'comment',
  ],
  meta: ['key', 'value'],
};

var META_COUNTERS = {
  catalog_items: 'next_catalog_id',
  parts: 'next_part_id',
  part_photos: 'next_part_photo_id',
  tech_processes: 'next_tech_process_id',
  setups: 'next_setup_id',
  operations: 'next_operation_id',
};

/** Кэш чтения листов (секунды) — меньше ждущих открытий Sheets на каждый клик. */
var SHEET_CACHE_TTL_SEC = 45;

function sheetCache_() {
  return CacheService.getScriptCache();
}

function sheetCacheKey_(sheetName) {
  return 'sheet_rows_v1_' + sheetName;
}

function sheetCacheInvalidate_(sheetName) {
  try {
    sheetCache_().remove(sheetCacheKey_(sheetName));
  } catch (e) {
    // ignore
  }
}

function sheetCacheInvalidateAll_() {
  Object.keys(SHEET_HEADERS).forEach(function (name) {
    sheetCacheInvalidate_(name);
  });
}

function getSpreadsheetId_() {
  return getScriptProp_('SPREADSHEET_ID', DEFAULT_SPREADSHEET_ID);
}

function getSpreadsheet_() {
  var id = getSpreadsheetId_();
  var active = SpreadsheetApp.getActiveSpreadsheet();
  if (active && active.getId() === id) {
    return active;
  }
  return SpreadsheetApp.openById(id);
}

function getSheetByName_(name) {
  var sheet = getSpreadsheet_().getSheetByName(name);
  if (!sheet) {
    throw new HttpError_(500, 'Sheet not found: ' + name);
  }
  return sheet;
}

function sheetGetMeta_(key) {
  var sheet = getSheetByName_(SHEET_NAMES.META);
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) {
      return values[i][1];
    }
  }
  return null;
}

function sheetSetMeta_(key, value) {
  var sheet = getSheetByName_(SHEET_NAMES.META);
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) {
      sheet.getRange(i + 1, 2).setValue(value);
      sheetCacheInvalidate_(SHEET_NAMES.META);
      return;
    }
  }
  sheet.appendRow([key, value]);
  sheetCacheInvalidate_(SHEET_NAMES.META);
}

/**
 * Allocate next integer id from meta (keys like next_catalog_id).
 * Seed from prepareSpreadsheet starts at 1.
 * Locked to avoid duplicate ids under concurrent Web App requests.
 */
function sheetNextId_(sheetName) {
  var lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    var key = META_COUNTERS[sheetName];
    if (!key) {
      throw new HttpError_(500, 'No meta counter for sheet: ' + sheetName);
    }
    var current = Number(sheetGetMeta_(key));
    if (isNaN(current) || current < 1) {
      current = 1;
    }
    sheetSetMeta_(key, current + 1);
    return current;
  } finally {
    lock.releaseLock();
  }
}

function sheetRows_(sheetName) {
  var cacheKey = sheetCacheKey_(sheetName);
  try {
    var cached = sheetCache_().get(cacheKey);
    if (cached) {
      return JSON.parse(cached);
    }
  } catch (e) {
    // ignore cache errors
  }

  var sheet = getSheetByName_(sheetName);
  var values = sheet.getDataRange().getValues();
  if (values.length < 2) {
    return [];
  }
  var headers = values[0].map(String);
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

  try {
    sheetCache_().put(cacheKey, JSON.stringify(rows), SHEET_CACHE_TTL_SEC);
  } catch (e2) {
    // Cache quota / size — ignore
  }
  return rows;
}

function sheetFindById_(sheetName, id) {
  var rows = sheetRows_(sheetName);
  for (var i = 0; i < rows.length; i++) {
    if (Number(rows[i].id) === Number(id)) {
      return rows[i];
    }
  }
  return null;
}

function sheetAppend_(sheetName, obj) {
  var headers = SHEET_HEADERS[sheetName];
  var row = headers.map(function (key) {
    return obj[key] === undefined ? '' : obj[key];
  });
  getSheetByName_(sheetName).appendRow(row);
  sheetCacheInvalidate_(sheetName);
}

function sheetUpdate_(sheetName, rowNumber, obj) {
  var headers = SHEET_HEADERS[sheetName];
  var values = headers.map(function (key) {
    return obj[key] === undefined ? '' : obj[key];
  });
  getSheetByName_(sheetName).getRange(rowNumber, 1, 1, headers.length).setValues([values]);
  sheetCacheInvalidate_(sheetName);
}

function sheetDeleteRow_(sheetName, rowNumber) {
  getSheetByName_(sheetName).deleteRow(rowNumber);
  sheetCacheInvalidate_(sheetName);
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

function matchPath_(pattern, path) {
  var patternParts = pattern.split('/');
  var pathParts = path.split('/');
  if (patternParts.length !== pathParts.length) {
    return null;
  }
  var params = {};
  for (var i = 0; i < patternParts.length; i++) {
    var pp = patternParts[i];
    var vp = pathParts[i];
    if (pp.charAt(0) === '{' && pp.charAt(pp.length - 1) === '}') {
      var name = pp.substring(1, pp.length - 1);
      var num = Number(vp);
      if (String(num) !== vp || isNaN(num)) {
        return null;
      }
      params[name] = num;
    } else if (pp !== vp) {
      return null;
    }
  }
  return params;
}

function parseRequestEnvelope_(e) {
  var path = '/health';
  var method = 'GET';
  var query = {};
  var body = null;
  var initData = null;

  if (e && e.parameter) {
    if (e.parameter.path) {
      path = String(e.parameter.path);
    }
    if (e.parameter.method) {
      method = String(e.parameter.method).toUpperCase();
    }
    if (e.parameter.initData) {
      initData = String(e.parameter.initData);
    }
    Object.keys(e.parameter).forEach(function (key) {
      if (key !== 'path' && key !== 'method' && key !== 'initData') {
        query[key] = e.parameter[key];
      }
    });
  }

  if (e && e.postData && e.postData.contents) {
    var parsed;
    try {
      parsed = JSON.parse(e.postData.contents);
    } catch (err) {
      throw new HttpError_(422, 'Invalid JSON body');
    }
    if (parsed.path) {
      path = String(parsed.path);
    }
    if (parsed.method) {
      method = String(parsed.method).toUpperCase();
    }
    if (parsed.query && typeof parsed.query === 'object') {
      query = parsed.query;
    }
    if (parsed.body !== undefined) {
      body = parsed.body;
    }
    if (parsed.initData) {
      initData = parsed.initData;
    } else if (parsed.telegramInitData) {
      initData = parsed.telegramInitData;
    }
    if (parsed.headers && parsed.headers[INIT_DATA_HEADER]) {
      initData = parsed.headers[INIT_DATA_HEADER];
    }
  }

  if (path.charAt(0) !== '/') {
    path = '/' + path;
  }

  return {
    path: path,
    method: method,
    query: query || {},
    body: body,
    initData: initData,
  };
}
