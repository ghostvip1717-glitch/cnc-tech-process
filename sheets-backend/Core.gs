/**
 * Config, auth, HTTP envelope helpers for Apps Script Web App.
 * Script Properties:
 *   SPREADSHEET_ID, DRIVE_FOLDER_ID, BOT_TOKEN,
 *   TELEGRAM_AUTH_ENABLED, TELEGRAM_ALLOWED_USER_IDS
 */

var INIT_DATA_HEADER = 'X-Telegram-Init-Data';
var MAX_AUTH_AGE_SECONDS = 86400;
var API_V1_PREFIX = '/api/v1';

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
  part_photos: ['id', 'part_id', 'file_url', 'sort_order', 'drive_file_id'],
  tech_processes: ['id', 'part_id'],
  setups: ['id', 'tech_process_id', 'order', 'jaw_id'],
  operations: ['id', 'setup_id', 'order', 'op_number', 'title', 'tool_id', 'plate_id', 'comment'],
  meta: ['key', 'value'],
};

function getProp_(key, fallback) {
  var value = PropertiesService.getScriptProperties().getProperty(key);
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function getConfig_() {
  return {
    spreadsheetId: getProp_('SPREADSHEET_ID', ''),
    driveFolderId: getProp_('DRIVE_FOLDER_ID', ''),
    botToken: getProp_('BOT_TOKEN', ''),
    telegramAuthEnabled: String(getProp_('TELEGRAM_AUTH_ENABLED', 'false')).toLowerCase() === 'true',
    telegramAllowedUserIds: parseAllowedUserIds_(getProp_('TELEGRAM_ALLOWED_USER_IDS', '')),
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
    .filter(function (part) {
      return part !== '';
    })
    .map(function (part) {
      return Number(part);
    })
    .filter(function (id) {
      return !isNaN(id);
    });
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

function HttpError_(status, detail) {
  this.status = status;
  this.detail = detail;
  this.name = 'HttpError';
}

function requireAuth_(path, telegramInitData) {
  var config = getConfig_();
  if (!config.telegramAuthEnabled) {
    return null;
  }
  if (path === '/health') {
    return null;
  }
  if (path.indexOf(API_V1_PREFIX) !== 0) {
    return null;
  }
  if (!telegramInitData) {
    throw new HttpError_(401, 'Missing Telegram init data');
  }
  var user = validateTelegramInitData_(telegramInitData, config.botToken);
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
  var data = parsed.data;
  var receivedHash = parsed.hash;
  if (!receivedHash) {
    throw new HttpError_(401, 'hash is missing');
  }

  var keys = Object.keys(data).sort();
  var lines = [];
  for (var i = 0; i < keys.length; i++) {
    lines.push(keys[i] + '=' + data[keys[i]]);
  }
  var dataCheckString = lines.join('\n');

  var secretKey = Utilities.computeHmacSha256Signature(botToken, 'WebAppData');
  var calculatedBytes = Utilities.computeHmacSha256Signature(dataCheckString, secretKey);
  var calculatedHash = bytesToHex_(calculatedBytes);

  if (calculatedHash !== receivedHash) {
    throw new HttpError_(401, 'invalid init data signature');
  }

  if (data.auth_date) {
    var authDate = Number(data.auth_date);
    var nowSec = Math.floor(Date.now() / 1000);
    if (nowSec - authDate > MAX_AUTH_AGE_SECONDS) {
      throw new HttpError_(401, 'init data is expired');
    }
  }

  if (!data.user) {
    throw new HttpError_(401, 'user is missing');
  }

  var user;
  try {
    user = JSON.parse(data.user);
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
    var pair = parts[i];
    var eq = pair.indexOf('=');
    if (eq === -1) {
      continue;
    }
    var key = decodeURIComponent(pair.substring(0, eq));
    var value = decodeURIComponent(pair.substring(eq + 1).replace(/\+/g, ' '));
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

function parseRequestEnvelope_(e) {
  var path = '/health';
  var method = 'GET';
  var query = {};
  var body = null;
  var telegramInitData = null;

  if (e && e.parameter) {
    if (e.parameter.path) {
      path = String(e.parameter.path);
    }
    if (e.parameter.method) {
      method = String(e.parameter.method).toUpperCase();
    }
    if (e.parameter.telegramInitData) {
      telegramInitData = String(e.parameter.telegramInitData);
    }
    Object.keys(e.parameter).forEach(function (key) {
      if (key !== 'path' && key !== 'method' && key !== 'telegramInitData') {
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
    if (parsed.telegramInitData) {
      telegramInitData = parsed.telegramInitData;
    }
    if (parsed.headers && parsed.headers[INIT_DATA_HEADER]) {
      telegramInitData = parsed.headers[INIT_DATA_HEADER];
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
    telegramInitData: telegramInitData,
  };
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
