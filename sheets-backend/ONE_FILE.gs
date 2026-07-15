/**
 * CNC Tech Process — весь backend в ОДНОМ файле для Apps Script.
 *
 * КАК УСТАНОВИТЬ:
 * 1. Таблица → Расширения → Apps Script
 * 2. Удали весь старый код во всех файлах (оставь один файл Code)
 * 3. Вставь этот файл целиком → Сохранить (Ctrl+S)
 * 4. Project Settings → показать файл манифеста appsscript.json → вставить oauthScopes
 *    (spreadsheets + drive + script.container.ui) из sheets-backend/appsscript.json
 * 5. При запросе прав → Разрешить (включая Google Drive)
 * 6. Параметры проекта → Свойства скрипта:
 *      SPREADSHEET_ID = 1kn8B0t_WTImI5At6YKlJqsr_KHS6m2IlRFT0CDnDuUU
 *      DRIVE_PHOTOS_FOLDER_ID = 1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC
 *      TELEGRAM_AUTH_ENABLED = false
 * 7. Deploy → Управление развёртываниями → карандаш → Новая версия →
 *      от моего имени / доступ: Все → Развернуть
 * 8. Проверка:
 *    .../exec?path=/health  →  {"ok":true,"httpStatus":200,"data":{"status":"OK"}}
 *    POST /api/v1/parts/{id}/photos → 201 + файл в Drive
 *
 * Web App URL:
 * https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec
 */

// ===== core/Response.gs =====

/**
 * JSON envelope helpers.
 * GAS Web App always returns HTTP 200; app status is in httpStatus.
 */

function HttpError_(status, detail) {
  this.status = status;
  this.detail = detail;
  this.name = 'HttpError';
}

function okResponse_(data, httpStatus) {
  return {
    ok: true,
    httpStatus: httpStatus === undefined ? 200 : httpStatus,
    data: data === undefined ? null : data,
  };
}

function errResponse_(httpStatus, detail) {
  return {
    ok: false,
    httpStatus: httpStatus,
    detail: detail,
  };
}

function jsonOutput_(payload) {
  return ContentService.createTextOutput(JSON.stringify(payload)).setMimeType(
    ContentService.MimeType.JSON,
  );
}

// ===== core/Auth.gs =====

/**
 * Telegram WebApp initData validation.
 * Script Properties: BOT_TOKEN, TELEGRAM_AUTH_ENABLED, TELEGRAM_ALLOWED_USER_IDS
 */

var INIT_DATA_HEADER = 'X-Telegram-Init-Data';
var MAX_AUTH_AGE_SECONDS = 86400;
var API_V1_PREFIX = '/api/v1';

function getScriptProp_(key, fallback) {
  var value = PropertiesService.getScriptProperties().getProperty(key);
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function getAuthConfig_() {
  return {
    botToken: getScriptProp_('BOT_TOKEN', ''),
    telegramAuthEnabled:
      String(getScriptProp_('TELEGRAM_AUTH_ENABLED', 'false')).toLowerCase() === 'true',
    telegramAllowedUserIds: parseAllowedUserIds_(
      getScriptProp_('TELEGRAM_ALLOWED_USER_IDS', ''),
    ),
  };
}

function parseAllowedUserIds_(raw) {
  if (!raw) {
    return [];
  }
  return String(raw)
    .split(',')
    .map(function (part) {
      return part.trim();
    })
    .filter(Boolean)
    .map(Number)
    .filter(function (id) {
      return !isNaN(id);
    });
}

function requireAuth_(path, initData) {
  var config = getAuthConfig_();
  if (!config.telegramAuthEnabled) {
    return null;
  }
  if (path === '/health') {
    return null;
  }
  if (path.indexOf(API_V1_PREFIX) !== 0) {
    return null;
  }
  if (!initData) {
    throw new HttpError_(401, 'Missing Telegram init data');
  }
  var user = validateTelegramInitData_(initData, config.botToken);
  if (config.telegramAllowedUserIds.length > 0) {
    var userId = Number(user.id);
    if (config.telegramAllowedUserIds.indexOf(userId) === -1) {
      throw new HttpError_(403, 'User not allowed');
    }
  }
  return user;
}

function validateTelegramInitData_(initData, botToken) {
  if (!initData) {
    throw new HttpError_(401, 'init data is empty');
  }
  if (!botToken) {
    throw new HttpError_(401, 'bot token is not configured');
  }

  var parsed = parseInitData_(initData);
  if (!parsed.hash) {
    throw new HttpError_(401, 'hash is missing');
  }

  var keys = Object.keys(parsed.data).sort();
  var lines = [];
  for (var i = 0; i < keys.length; i++) {
    lines.push(keys[i] + '=' + parsed.data[keys[i]]);
  }
  var dataCheckString = lines.join('\n');

  var secretKey = Utilities.computeHmacSha256Signature(botToken, 'WebAppData');
  var calculatedHash = bytesToHex_(
    Utilities.computeHmacSha256Signature(dataCheckString, secretKey),
  );

  if (calculatedHash !== parsed.hash) {
    throw new HttpError_(401, 'invalid init data signature');
  }

  if (parsed.data.auth_date) {
    var authDate = Number(parsed.data.auth_date);
    var nowSec = Math.floor(Date.now() / 1000);
    if (nowSec - authDate > MAX_AUTH_AGE_SECONDS) {
      throw new HttpError_(401, 'init data is expired');
    }
  }

  if (!parsed.data.user) {
    throw new HttpError_(401, 'user is missing');
  }

  var user;
  try {
    user = JSON.parse(parsed.data.user);
  } catch (e) {
    throw new HttpError_(401, 'user is missing');
  }
  if (!user || user.id === undefined || user.id === null) {
    throw new HttpError_(401, 'user id is missing');
  }
  return user;
}

function parseInitData_(initData) {
  var parts = String(initData).split('&');
  var data = {};
  var hash = null;
  for (var i = 0; i < parts.length; i++) {
    var eq = parts[i].indexOf('=');
    if (eq === -1) {
      continue;
    }
    var key = decodeURIComponent(parts[i].substring(0, eq));
    var value = decodeURIComponent(parts[i].substring(eq + 1).replace(/\+/g, ' '));
    if (key === 'hash') {
      hash = value;
    } else {
      data[key] = value;
    }
  }
  return { data: data, hash: hash };
}

function bytesToHex_(bytes) {
  var hex = [];
  for (var i = 0; i < bytes.length; i++) {
    var b = bytes[i];
    if (b < 0) {
      b += 256;
    }
    var h = b.toString(16);
    hex.push(h.length === 1 ? '0' + h : h);
  }
  return hex.join('');
}

// ===== core/SheetStore.gs =====

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
      return;
    }
  }
  sheet.appendRow([key, value]);
}

/**
 * Allocate next integer id from meta (keys like next_catalog_id).
 * Seed from prepareSpreadsheet starts at 1.
 */
function sheetNextId_(sheetName) {
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
}

function sheetRows_(sheetName) {
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
}

function sheetUpdate_(sheetName, rowNumber, obj) {
  var headers = SHEET_HEADERS[sheetName];
  var values = headers.map(function (key) {
    return obj[key] === undefined ? '' : obj[key];
  });
  getSheetByName_(sheetName).getRange(rowNumber, 1, 1, headers.length).setValues([values]);
}

function sheetDeleteRow_(sheetName, rowNumber) {
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

// ===== catalog/CatalogRepository.gs =====

/**
 * catalog_items sheet access.
 */

function catalogRepoList_() {
  return sheetRows_(SHEET_NAMES.CATALOG);
}

function catalogRepoFindById_(id) {
  return sheetFindById_(SHEET_NAMES.CATALOG, id);
}

function catalogRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.CATALOG, row);
}

function catalogRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.CATALOG, rowNumber, row);
}

function catalogRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.CATALOG, rowNumber);
}

function catalogRepoIsReferenced_(itemId) {
  var setups = sheetRows_(SHEET_NAMES.SETUPS);
  for (var i = 0; i < setups.length; i++) {
    if (Number(setups[i].jaw_id) === Number(itemId)) {
      return true;
    }
  }
  var operations = sheetRows_(SHEET_NAMES.OPERATIONS);
  for (var j = 0; j < operations.length; j++) {
    if (
      Number(operations[j].tool_id) === Number(itemId) ||
      Number(operations[j].plate_id) === Number(itemId)
    ) {
      return true;
    }
  }
  return false;
}

function catalogRepoFindByTypeAndName_(type, name) {
  var rows = catalogRepoList_();
  for (var i = 0; i < rows.length; i++) {
    if (String(rows[i].type) === type && String(rows[i].name) === name) {
      return rows[i];
    }
  }
  return null;
}

// ===== catalog/CatalogService.gs =====

/**
 * Catalog business rules and HTTP handlers.
 */

function catalogSerialize_(row) {
  return {
    id: toInt_(row.id),
    type: String(row.type),
    name: String(row.name),
    note: emptyToNull_(row.note),
  };
}

function catalogList_(query) {
  var typeFilter = query && query.type ? String(query.type) : null;
  var q = query && query.q ? String(query.q).toLowerCase() : null;
  var items = catalogRepoList_()
    .map(catalogSerialize_)
    .filter(function (item) {
      if (typeFilter && item.type !== typeFilter) {
        return false;
      }
      if (q && item.name.toLowerCase().indexOf(q) === -1) {
        return false;
      }
      return true;
    });
  items.sort(function (a, b) {
    return a.id - b.id;
  });
  return okResponse_(items);
}

function catalogGet_(itemId) {
  var item = catalogRepoFindById_(itemId);
  if (!item) {
    throw new HttpError_(404, 'Catalog item not found');
  }
  return okResponse_(catalogSerialize_(item));
}

function catalogCreate_(body) {
  if (!body || !body.type || !body.name) {
    throw new HttpError_(422, 'type and name are required');
  }
  var type = String(body.type);
  if (['tool', 'plate', 'jaw'].indexOf(type) === -1) {
    throw new HttpError_(422, 'Invalid type');
  }
  var name = String(body.name).trim();
  if (!name || name.length > 255) {
    throw new HttpError_(422, 'Invalid name');
  }
  var note = body.note === undefined || body.note === null ? null : String(body.note);
  if (note && note.length > 1000) {
    throw new HttpError_(422, 'note is too long');
  }
  if (catalogRepoFindByTypeAndName_(type, name)) {
    throw new HttpError_(409, 'Item with this name already exists for type ' + type);
  }

  var id = sheetNextId_(SHEET_NAMES.CATALOG);
  var row = { id: id, type: type, name: name, note: note === null ? '' : note };
  catalogRepoInsert_(row);
  return okResponse_(catalogSerialize_(row), 201);
}

function catalogUpdate_(itemId, body) {
  var item = catalogRepoFindById_(itemId);
  if (!item) {
    throw new HttpError_(404, 'Catalog item not found');
  }
  if (!body || (body.name === undefined && body.note === undefined)) {
    throw new HttpError_(422, 'No fields to update');
  }

  var name = body.name === undefined ? String(item.name) : String(body.name).trim();
  if (!name || name.length > 255) {
    throw new HttpError_(422, 'Invalid name');
  }

  var note;
  if (body.note === undefined) {
    note = emptyToNull_(item.note);
  } else if (body.note === null) {
    note = null;
  } else {
    note = String(body.note);
    if (note.length > 1000) {
      throw new HttpError_(422, 'note is too long');
    }
  }

  var conflict = catalogRepoFindByTypeAndName_(String(item.type), name);
  if (conflict && Number(conflict.id) !== Number(itemId)) {
    throw new HttpError_(409, 'Item with this name already exists for type ' + item.type);
  }

  var updated = {
    id: toInt_(item.id),
    type: String(item.type),
    name: name,
    note: note === null ? '' : note,
  };
  catalogRepoUpdate_(item.__row, updated);
  return okResponse_(catalogSerialize_(updated));
}

function catalogDelete_(itemId) {
  var item = catalogRepoFindById_(itemId);
  if (!item) {
    throw new HttpError_(404, 'Catalog item not found');
  }
  if (catalogRepoIsReferenced_(itemId)) {
    throw new HttpError_(409, 'Catalog item is used in tech process and cannot be deleted');
  }
  catalogRepoDelete_(item.__row);
  return okResponse_(null, 204);
}

function catalogGetByIdAndType_(itemId, expectedType) {
  var item = catalogRepoFindById_(itemId);
  if (!item || String(item.type) !== expectedType) {
    return null;
  }
  return catalogSerialize_(item);
}

// ===== parts/PartsRepository.gs =====

/**
 * parts sheet access.
 */

function partsRepoList_() {
  return sheetRows_(SHEET_NAMES.PARTS);
}

function partsRepoFindById_(id) {
  return sheetFindById_(SHEET_NAMES.PARTS, id);
}

function partsRepoFindByNumber_(number) {
  var rows = partsRepoList_();
  for (var i = 0; i < rows.length; i++) {
    if (String(rows[i].number) === number) {
      return rows[i];
    }
  }
  return null;
}

function partsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.PARTS, row);
}

function partsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.PARTS, rowNumber, row);
}

function partsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.PARTS, rowNumber);
}

function photosRepoListByPart_(partId) {
  return sheetRows_(SHEET_NAMES.PART_PHOTOS)
    .filter(function (row) {
      return Number(row.part_id) === Number(partId);
    })
    .sort(function (a, b) {
      return Number(a.sort_order) - Number(b.sort_order);
    });
}

function photosRepoFind_(partId, photoId) {
  var photos = photosRepoListByPart_(partId);
  for (var i = 0; i < photos.length; i++) {
    if (Number(photos[i].id) === Number(photoId)) {
      return photos[i];
    }
  }
  return null;
}

function photosRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.PART_PHOTOS, row);
}

function photosRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.PART_PHOTOS, rowNumber, row);
}

function photosRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.PART_PHOTOS, rowNumber);
}

// ===== parts/PartsService.gs =====

/**
 * Parts CRUD + photo orchestration.
 */

function partsSerialize_(row) {
  var partId = toInt_(row.id);
  return {
    id: partId,
    number: String(row.number),
    title: String(row.title),
    created_at: normalizeIsoDate_(row.created_at),
    photos: photosRepoListByPart_(partId).map(photosSerialize_),
  };
}

function normalizeIsoDate_(value) {
  if (Object.prototype.toString.call(value) === '[object Date]') {
    return value.toISOString();
  }
  return String(value);
}

function partsList_(query) {
  var q = query && query.q ? String(query.q).toLowerCase() : null;
  var parts = partsRepoList_().map(partsSerialize_);
  if (q) {
    parts = parts.filter(function (part) {
      return (
        part.number.toLowerCase().indexOf(q) !== -1 ||
        part.title.toLowerCase().indexOf(q) !== -1
      );
    });
  }
  parts.sort(function (a, b) {
    return b.id - a.id;
  });
  return okResponse_(parts);
}

function partsGet_(partId) {
  var part = partsRepoFindById_(partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  return okResponse_(partsSerialize_(part));
}

function partsCreate_(body) {
  if (!body || !body.number || !body.title) {
    throw new HttpError_(422, 'number and title are required');
  }
  var number = String(body.number).trim();
  var title = String(body.title).trim();
  if (!number || !title) {
    throw new HttpError_(422, 'number and title are required');
  }
  if (partsRepoFindByNumber_(number)) {
    throw new HttpError_(409, 'Part with this number already exists');
  }

  var id = sheetNextId_(SHEET_NAMES.PARTS);
  var row = {
    id: id,
    number: number,
    title: title,
    created_at: new Date().toISOString(),
  };
  partsRepoInsert_(row);
  return okResponse_(partsSerialize_(row), 201);
}

function partsUpdate_(partId, body) {
  var part = partsRepoFindById_(partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  if (!body || (body.number === undefined && body.title === undefined)) {
    throw new HttpError_(422, 'No fields to update');
  }

  var number = body.number === undefined ? String(part.number) : String(body.number).trim();
  var title = body.title === undefined ? String(part.title) : String(body.title).trim();
  if (!number || !title) {
    throw new HttpError_(422, 'number and title are required');
  }

  var conflict = partsRepoFindByNumber_(number);
  if (conflict && Number(conflict.id) !== Number(partId)) {
    throw new HttpError_(409, 'Part with this number already exists');
  }

  var updated = {
    id: toInt_(part.id),
    number: number,
    title: title,
    created_at: String(part.created_at),
  };
  partsRepoUpdate_(part.__row, updated);
  return okResponse_(partsSerialize_(updated));
}

function partsDelete_(partId) {
  var part = partsRepoFindById_(partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  photosDeleteAllForPart_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (tp) {
    techProcessDeleteCascade_(tp);
  }
  partsRepoDelete_(part.__row);
  return okResponse_(null, 204);
}

function partsRequire_(partId) {
  var part = partsRepoFindById_(partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  return part;
}

// ===== parts/Photos.gs =====

/**
 * Drive photo upload/delete.
 * Script Properties: DRIVE_PHOTOS_FOLDER_ID
 */

function getDrivePhotosFolderId_() {
  return getScriptProp_('DRIVE_PHOTOS_FOLDER_ID', '');
}

function photosSerialize_(row) {
  var fileUrl = String(row.file_url || '');
  var driveId = extractDriveFileId_(fileUrl);
  return {
    id: toInt_(row.id),
    part_id: toInt_(row.part_id),
    file_path: driveId ? 'drive/' + driveId : fileUrl,
    url: fileUrl,
    sort_order: toInt_(row.sort_order),
  };
}

function extractDriveFileId_(fileUrl) {
  if (!fileUrl) {
    return '';
  }
  var match = String(fileUrl).match(/[?&]id=([a-zA-Z0-9_-]+)/);
  if (match) {
    return match[1];
  }
  match = String(fileUrl).match(/\/d\/([a-zA-Z0-9_-]+)/);
  return match ? match[1] : '';
}

function photosUpload_(partId, body) {
  if (!partsRepoFindById_(partId)) {
    throw new HttpError_(404, 'Part not found');
  }
  if (!body || !body.contentBase64) {
    throw new HttpError_(422, 'Empty file');
  }

  var mimeType = body.mimeType ? String(body.mimeType) : 'image/jpeg';
  var allowed = {
    'image/jpeg': '.jpg',
    'image/png': '.png',
    'image/webp': '.webp',
    'image/gif': '.gif',
  };
  var ext = allowed[mimeType];
  if (!ext) {
    throw new HttpError_(422, 'Unsupported image type');
  }

  var folderId = getDrivePhotosFolderId_();
  if (!folderId) {
    throw new HttpError_(500, 'DRIVE_PHOTOS_FOLDER_ID is not configured');
  }

  var folder = DriveApp.getFolderById(folderId);
  var bytes = Utilities.base64Decode(body.contentBase64);
  var fileName = body.fileName ? String(body.fileName) : 'part-' + partId + '-' + Date.now() + ext;
  var blob = Utilities.newBlob(bytes, mimeType, fileName);
  var file = folder.createFile(blob);
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  var fileUrl = 'https://drive.google.com/uc?export=view&id=' + file.getId();

  var photos = photosRepoListByPart_(partId);
  var sortOrder = 0;
  for (var i = 0; i < photos.length; i++) {
    var so = Number(photos[i].sort_order);
    if (so >= sortOrder) {
      sortOrder = so + 1;
    }
  }

  var id = sheetNextId_(SHEET_NAMES.PART_PHOTOS);
  var row = {
    id: id,
    part_id: partId,
    file_url: fileUrl,
    sort_order: sortOrder,
  };
  photosRepoInsert_(row);
  return okResponse_(photosSerialize_(row), 201);
}

function photosDelete_(partId, photoId) {
  var photo = photosRepoFind_(partId, photoId);
  if (!photo) {
    throw new HttpError_(404, 'Photo not found');
  }
  trashDriveFileByUrl_(photo.file_url);
  photosRepoDelete_(photo.__row);
  return okResponse_(null, 204);
}

function photosReorder_(partId, body) {
  if (!partsRepoFindById_(partId)) {
    throw new HttpError_(404, 'Part not found');
  }
  if (!body || !body.photo_ids || !(body.photo_ids instanceof Array)) {
    throw new HttpError_(422, 'photo_ids is required');
  }

  var photos = photosRepoListByPart_(partId);
  if (photos.length !== body.photo_ids.length) {
    throw new HttpError_(422, 'photo_ids must contain all photo ids');
  }
  var byId = {};
  for (var i = 0; i < photos.length; i++) {
    byId[Number(photos[i].id)] = photos[i];
  }
  for (var j = 0; j < body.photo_ids.length; j++) {
    if (!byId[Number(body.photo_ids[j])]) {
      throw new HttpError_(422, 'photo_ids must contain all photo ids');
    }
  }

  var result = [];
  for (var k = 0; k < body.photo_ids.length; k++) {
    var row = byId[Number(body.photo_ids[k])];
    var updated = {
      id: toInt_(row.id),
      part_id: toInt_(row.part_id),
      file_url: String(row.file_url),
      sort_order: k,
    };
    photosRepoUpdate_(row.__row, updated);
    result.push(photosSerialize_(updated));
  }
  return okResponse_(result);
}

function trashDriveFileByUrl_(fileUrl) {
  var fileId = extractDriveFileId_(fileUrl);
  if (!fileId) {
    return;
  }
  try {
    DriveApp.getFileById(fileId).setTrashed(true);
  } catch (e) {
    // ignore missing file
  }
}

function photosDeleteAllForPart_(partId) {
  var photos = photosRepoListByPart_(partId)
    .slice()
    .sort(function (a, b) {
      return b.__row - a.__row;
    });
  for (var i = 0; i < photos.length; i++) {
    trashDriveFileByUrl_(photos[i].file_url);
    photosRepoDelete_(photos[i].__row);
  }
}

// ===== tech_process/TechProcessRepository.gs =====

/**
 * tech_processes / setups / operations sheet access.
 */

function techProcessRepoFindByPartId_(partId) {
  var rows = sheetRows_(SHEET_NAMES.TECH_PROCESSES);
  for (var i = 0; i < rows.length; i++) {
    if (Number(rows[i].part_id) === Number(partId)) {
      return rows[i];
    }
  }
  return null;
}

function techProcessRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.TECH_PROCESSES, row);
}

function techProcessRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.TECH_PROCESSES, rowNumber);
}

function setupsRepoListByTp_(techProcessId) {
  return sheetRows_(SHEET_NAMES.SETUPS)
    .filter(function (row) {
      return Number(row.tech_process_id) === Number(techProcessId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function setupsRepoFindById_(setupId) {
  return sheetFindById_(SHEET_NAMES.SETUPS, setupId);
}

function setupsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.SETUPS, row);
}

function setupsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.SETUPS, rowNumber, row);
}

function setupsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.SETUPS, rowNumber);
}

function operationsRepoListBySetup_(setupId) {
  return sheetRows_(SHEET_NAMES.OPERATIONS)
    .filter(function (row) {
      return Number(row.setup_id) === Number(setupId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function operationsRepoFindById_(operationId) {
  return sheetFindById_(SHEET_NAMES.OPERATIONS, operationId);
}

function operationsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.OPERATIONS, row);
}

function operationsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.OPERATIONS, rowNumber, row);
}

function operationsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.OPERATIONS, rowNumber);
}

function techProcessDeleteCascade_(tp) {
  var setups = setupsRepoListByTp_(tp.id);
  var opRows = [];
  for (var i = 0; i < setups.length; i++) {
    opRows = opRows.concat(operationsRepoListBySetup_(setups[i].id));
  }
  opRows.sort(function (a, b) {
    return b.__row - a.__row;
  });
  for (var j = 0; j < opRows.length; j++) {
    operationsRepoDelete_(opRows[j].__row);
  }

  setups
    .slice()
    .sort(function (a, b) {
      return b.__row - a.__row;
    })
    .forEach(function (setup) {
      setupsRepoDelete_(setup.__row);
    });

  techProcessRepoDelete_(tp.__row);
}

// ===== tech_process/TechProcessService.gs =====

/**
 * Tech process: one per part, setups, operations, reorder.
 */

var ROMAN_ORDERS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];

function setupOrderLabel_(order) {
  var n = Number(order);
  if (n >= 0 && n < ROMAN_ORDERS.length) {
    return ROMAN_ORDERS[n];
  }
  return String(n + 1);
}

function operationSerialize_(row) {
  return {
    id: toInt_(row.id),
    setup_id: toInt_(row.setup_id),
    order: toInt_(row.order),
    op_number: String(row.op_number),
    title: String(row.title),
    tool_id: toInt_(row.tool_id),
    plate_id: toInt_(row.plate_id),
    comment: emptyToNull_(row.comment),
  };
}

function setupSerialize_(row) {
  return {
    id: toInt_(row.id),
    tech_process_id: toInt_(row.tech_process_id),
    order: toInt_(row.order),
    order_label: setupOrderLabel_(row.order),
    jaw_id: toInt_(row.jaw_id),
    operations: operationsRepoListBySetup_(row.id).map(operationSerialize_),
  };
}

function techProcessSerialize_(row) {
  return {
    id: toInt_(row.id),
    part_id: toInt_(row.part_id),
    setups: setupsRepoListByTp_(row.id).map(setupSerialize_),
  };
}

function techProcessGet_(partId) {
  partsRequire_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  return okResponse_(techProcessSerialize_(tp));
}

function techProcessCreate_(partId) {
  partsRequire_(partId);
  if (techProcessRepoFindByPartId_(partId)) {
    throw new HttpError_(409, 'Tech process already exists for this part');
  }
  var id = sheetNextId_(SHEET_NAMES.TECH_PROCESSES);
  var row = { id: id, part_id: partId };
  techProcessRepoInsert_(row);
  return okResponse_(techProcessSerialize_(row));
}

function techProcessGetOrCreate_(partId) {
  var tp = techProcessRepoFindByPartId_(partId);
  if (tp) {
    return tp;
  }
  var id = sheetNextId_(SHEET_NAMES.TECH_PROCESSES);
  techProcessRepoInsert_({ id: id, part_id: partId });
  return techProcessRepoFindByPartId_(partId);
}

function setupCreate_(partId, body) {
  partsRequire_(partId);
  if (!body || body.jaw_id === undefined || body.jaw_id === null) {
    throw new HttpError_(422, 'jaw_id is required');
  }
  if (!catalogGetByIdAndType_(body.jaw_id, 'jaw')) {
    throw new HttpError_(422, 'Invalid jaw_id');
  }

  var tp = techProcessGetOrCreate_(partId);
  var setups = setupsRepoListByTp_(tp.id);
  var nextOrder = 0;
  for (var i = 0; i < setups.length; i++) {
    var o = Number(setups[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }

  var id = sheetNextId_(SHEET_NAMES.SETUPS);
  var row = {
    id: id,
    tech_process_id: toInt_(tp.id),
    order: nextOrder,
    jaw_id: toInt_(body.jaw_id),
  };
  setupsRepoInsert_(row);
  return okResponse_(setupSerialize_(row), 201);
}

function setupUpdate_(partId, setupId, body) {
  var setup = setupRequireForPart_(partId, setupId);
  if (!body || body.jaw_id === undefined || body.jaw_id === null) {
    throw new HttpError_(422, 'jaw_id is required');
  }
  if (!catalogGetByIdAndType_(body.jaw_id, 'jaw')) {
    throw new HttpError_(422, 'Invalid jaw_id');
  }
  var updated = {
    id: toInt_(setup.id),
    tech_process_id: toInt_(setup.tech_process_id),
    order: toInt_(setup.order),
    jaw_id: toInt_(body.jaw_id),
  };
  setupsRepoUpdate_(setup.__row, updated);
  return okResponse_(setupSerialize_(updated));
}

function setupDelete_(partId, setupId) {
  var setup = setupRequireForPart_(partId, setupId);
  var ops = operationsRepoListBySetup_(setupId)
    .slice()
    .sort(function (a, b) {
      return b.__row - a.__row;
    });
  for (var i = 0; i < ops.length; i++) {
    operationsRepoDelete_(ops[i].__row);
  }
  setupsRepoDelete_(setup.__row);
  return okResponse_(null, 204);
}

function operationCreate_(partId, setupId, body) {
  setupRequireForPart_(partId, setupId);
  if (!body || !body.op_number || !body.title || !body.tool_id || !body.plate_id) {
    throw new HttpError_(422, 'op_number, title, tool_id, plate_id are required');
  }
  if (!catalogGetByIdAndType_(body.tool_id, 'tool')) {
    throw new HttpError_(422, 'Invalid tool_id');
  }
  if (!catalogGetByIdAndType_(body.plate_id, 'plate')) {
    throw new HttpError_(422, 'Invalid plate_id');
  }

  var ops = operationsRepoListBySetup_(setupId);
  var nextOrder = 0;
  for (var i = 0; i < ops.length; i++) {
    var o = Number(ops[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }

  var id = sheetNextId_(SHEET_NAMES.OPERATIONS);
  var row = {
    id: id,
    setup_id: setupId,
    order: nextOrder,
    op_number: String(body.op_number),
    title: String(body.title),
    tool_id: toInt_(body.tool_id),
    plate_id: toInt_(body.plate_id),
    comment: body.comment === undefined || body.comment === null ? '' : String(body.comment),
  };
  operationsRepoInsert_(row);
  return okResponse_(operationSerialize_(row), 201);
}

function operationUpdate_(partId, operationId, body) {
  var op = operationRequireForPart_(partId, operationId);
  if (
    !body ||
    (body.op_number === undefined &&
      body.title === undefined &&
      body.tool_id === undefined &&
      body.plate_id === undefined &&
      body.comment === undefined)
  ) {
    throw new HttpError_(422, 'No fields to update');
  }

  var toolId = body.tool_id === undefined ? toInt_(op.tool_id) : toInt_(body.tool_id);
  var plateId = body.plate_id === undefined ? toInt_(op.plate_id) : toInt_(body.plate_id);
  if (body.tool_id !== undefined && !catalogGetByIdAndType_(toolId, 'tool')) {
    throw new HttpError_(422, 'Invalid tool_id');
  }
  if (body.plate_id !== undefined && !catalogGetByIdAndType_(plateId, 'plate')) {
    throw new HttpError_(422, 'Invalid plate_id');
  }

  var comment;
  if (body.comment === undefined) {
    comment = emptyToNull_(op.comment);
  } else if (body.comment === null) {
    comment = null;
  } else {
    comment = String(body.comment);
  }

  var updated = {
    id: toInt_(op.id),
    setup_id: toInt_(op.setup_id),
    order: toInt_(op.order),
    op_number: body.op_number === undefined ? String(op.op_number) : String(body.op_number),
    title: body.title === undefined ? String(op.title) : String(body.title),
    tool_id: toolId,
    plate_id: plateId,
    comment: comment === null ? '' : comment,
  };
  operationsRepoUpdate_(op.__row, updated);
  return okResponse_(operationSerialize_(updated));
}

function operationDelete_(partId, operationId) {
  var op = operationRequireForPart_(partId, operationId);
  operationsRepoDelete_(op.__row);
  return okResponse_(null, 204);
}

function operationsReorder_(partId, setupId, body) {
  setupRequireForPart_(partId, setupId);
  if (!body || !body.operation_ids || !(body.operation_ids instanceof Array)) {
    throw new HttpError_(422, 'operation_ids is required');
  }
  var ops = operationsRepoListBySetup_(setupId);
  if (ops.length !== body.operation_ids.length) {
    throw new HttpError_(422, 'operation_ids must contain all operation ids');
  }
  var byId = {};
  for (var i = 0; i < ops.length; i++) {
    byId[Number(ops[i].id)] = ops[i];
  }
  for (var j = 0; j < body.operation_ids.length; j++) {
    if (!byId[Number(body.operation_ids[j])]) {
      throw new HttpError_(422, 'operation_ids must contain all operation ids');
    }
  }

  var result = [];
  for (var k = 0; k < body.operation_ids.length; k++) {
    var row = byId[Number(body.operation_ids[k])];
    var updated = {
      id: toInt_(row.id),
      setup_id: toInt_(row.setup_id),
      order: k,
      op_number: String(row.op_number),
      title: String(row.title),
      tool_id: toInt_(row.tool_id),
      plate_id: toInt_(row.plate_id),
      comment: emptyToNull_(row.comment) === null ? '' : String(row.comment),
    };
    operationsRepoUpdate_(row.__row, updated);
    result.push(operationSerialize_(updated));
  }
  return okResponse_(result);
}

function setupRequireForPart_(partId, setupId) {
  partsRequire_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  var setup = setupsRepoFindById_(setupId);
  if (!setup || Number(setup.tech_process_id) !== Number(tp.id)) {
    throw new HttpError_(404, 'Setup not found');
  }
  return setup;
}

function operationRequireForPart_(partId, operationId) {
  partsRequire_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  var op = operationsRepoFindById_(operationId);
  if (!op) {
    throw new HttpError_(404, 'Operation not found');
  }
  var setup = setupsRepoFindById_(op.setup_id);
  if (!setup || Number(setup.tech_process_id) !== Number(tp.id)) {
    throw new HttpError_(404, 'Operation not found');
  }
  return op;
}

// ===== assembly/AssemblyService.gs =====

/**
 * Unique tools / plates / jaws required for a part.
 */

function assemblyRequiredItems_(partId) {
  partsRequire_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (!tp) {
    return okResponse_({ tools: [], plates: [], jaws: [] });
  }

  var toolIds = {};
  var plateIds = {};
  var jawIds = {};

  var setups = setupsRepoListByTp_(tp.id);
  for (var i = 0; i < setups.length; i++) {
    jawIds[Number(setups[i].jaw_id)] = true;
    var ops = operationsRepoListBySetup_(setups[i].id);
    for (var j = 0; j < ops.length; j++) {
      toolIds[Number(ops[j].tool_id)] = true;
      plateIds[Number(ops[j].plate_id)] = true;
    }
  }

  return okResponse_({
    tools: assemblyResolveIds_(Object.keys(toolIds), 'tool'),
    plates: assemblyResolveIds_(Object.keys(plateIds), 'plate'),
    jaws: assemblyResolveIds_(Object.keys(jawIds), 'jaw'),
  });
}

function assemblyResolveIds_(ids, expectedType) {
  var items = [];
  for (var i = 0; i < ids.length; i++) {
    var item = catalogGetByIdAndType_(Number(ids[i]), expectedType);
    if (item) {
      items.push({ id: item.id, type: item.type, name: item.name });
    }
  }
  items.sort(function (a, b) {
    return a.id - b.id;
  });
  return items;
}

// ===== Code.gs =====

/**
 * Web App entry: doGet / doPost + router.
 *
 * HTTP contract (единственный формат):
 *   GET  .../exec?path=/health
 *   POST .../exec  Content-Type: text/plain
 *   {
 *     "path": "/api/v1/catalog",
 *     "method": "GET",
 *     "query": { "type": "tool" },
 *     "body": null,
 *     "initData": "..."
 *   }
 *
 * Response envelope:
 *   { "ok": true, "httpStatus": 200, "data": ... }
 *   { "ok": false, "httpStatus": 401, "detail": "..." }
 */

function doGet(e) {
  return handleRequest_(e || {});
}

function doPost(e) {
  return handleRequest_(e || {});
}

function handleRequest_(e) {
  try {
    var req = parseRequestEnvelope_(e);
    requireAuth_(req.path, req.initData);
    var result = routeRequest_(req);
    return jsonOutput_(result);
  } catch (err) {
    if (err && err.name === 'HttpError') {
      return jsonOutput_(errResponse_(err.status, err.detail));
    }
    var message = err && err.message ? err.message : String(err);
    return jsonOutput_(errResponse_(500, message));
  }
}

function routeRequest_(req) {
  var path = req.path;
  var method = req.method;
  var query = req.query || {};
  var body = req.body;
  var params;

  if (path === '/health' && method === 'GET') {
    return okResponse_({ status: 'OK' });
  }

  if (path === '/api/v1/catalog' && method === 'GET') {
    return catalogList_(query);
  }
  if (path === '/api/v1/catalog' && method === 'POST') {
    return catalogCreate_(body);
  }
  params = matchPath_('/api/v1/catalog/{itemId}', path);
  if (params) {
    if (method === 'GET') {
      return catalogGet_(params.itemId);
    }
    if (method === 'PATCH') {
      return catalogUpdate_(params.itemId, body);
    }
    if (method === 'DELETE') {
      return catalogDelete_(params.itemId);
    }
  }

  if (path === '/api/v1/parts' && method === 'GET') {
    return partsList_(query);
  }
  if (path === '/api/v1/parts' && method === 'POST') {
    return partsCreate_(body);
  }
  params = matchPath_('/api/v1/parts/{partId}', path);
  if (params) {
    if (method === 'GET') {
      return partsGet_(params.partId);
    }
    if (method === 'PATCH') {
      return partsUpdate_(params.partId, body);
    }
    if (method === 'DELETE') {
      return partsDelete_(params.partId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/photos', path);
  if (params && method === 'POST') {
    return photosUpload_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/photos/reorder', path);
  if (params && method === 'PATCH') {
    return photosReorder_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/photos/{photoId}', path);
  if (params && method === 'DELETE') {
    return photosDelete_(params.partId, params.photoId);
  }

  params = matchPath_('/api/v1/parts/{partId}/tech-process', path);
  if (params) {
    if (method === 'GET') {
      return techProcessGet_(params.partId);
    }
    if (method === 'PUT') {
      return techProcessCreate_(params.partId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/tech-process/setups', path);
  if (params && method === 'POST') {
    return setupCreate_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/tech-process/setups/{setupId}', path);
  if (params) {
    if (method === 'PATCH') {
      return setupUpdate_(params.partId, params.setupId, body);
    }
    if (method === 'DELETE') {
      return setupDelete_(params.partId, params.setupId);
    }
  }

  params = matchPath_(
    '/api/v1/parts/{partId}/tech-process/setups/{setupId}/operations',
    path,
  );
  if (params && method === 'POST') {
    return operationCreate_(params.partId, params.setupId, body);
  }
  params = matchPath_(
    '/api/v1/parts/{partId}/tech-process/setups/{setupId}/operations/reorder',
    path,
  );
  if (params && method === 'PATCH') {
    return operationsReorder_(params.partId, params.setupId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/tech-process/operations/{operationId}', path);
  if (params) {
    if (method === 'PATCH') {
      return operationUpdate_(params.partId, params.operationId, body);
    }
    if (method === 'DELETE') {
      return operationDelete_(params.partId, params.operationId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/required-items', path);
  if (params && method === 'GET') {
    return assemblyRequiredItems_(params.partId);
  }

  throw new HttpError_(404, 'Not found: ' + method + ' ' + path);
}

// ===== setup/PrepareSpreadsheet.gs =====

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
