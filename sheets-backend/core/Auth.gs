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
