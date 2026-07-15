/**
 * Drive photo upload/delete via Drive API + OAuth token.
 * Обход Access denied: DriveApp в Web App.
 * Script Properties: DRIVE_PHOTOS_FOLDER_ID
 * Нужны scopes: drive + script.external_request
 */

function getDrivePhotosFolderId_() {
  return getScriptProp_('DRIVE_PHOTOS_FOLDER_ID', '');
}

function getDriveOAuthToken_() {
  return ScriptApp.getOAuthToken();
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

/**
 * Создаёт файл в папке Drive через multipart upload (Drive API v3).
 * @return {{id:string}}
 */
function driveApiCreateFile_(folderId, fileName, mimeType, bytes) {
  var boundary = 'cnc_boundary_' + Date.now();
  var delimiter = '--' + boundary + '\r\n';
  var closeDelim = '\r\n--' + boundary + '--';
  var metadata = {
    name: fileName,
    parents: [folderId],
  };
  var body =
    delimiter +
    'Content-Type: application/json; charset=UTF-8\r\n\r\n' +
    JSON.stringify(metadata) +
    '\r\n' +
    delimiter +
    'Content-Type: ' +
    mimeType +
    '\r\n' +
    'Content-Transfer-Encoding: base64\r\n\r\n' +
    Utilities.base64Encode(bytes) +
    closeDelim;

  var response = UrlFetchApp.fetch(
    'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name',
    {
      method: 'post',
      contentType: 'multipart/related; boundary=' + boundary,
      headers: {
        Authorization: 'Bearer ' + getDriveOAuthToken_(),
      },
      payload: body,
      muteHttpExceptions: true,
    },
  );

  var code = response.getResponseCode();
  var text = response.getContentText();
  if (code < 200 || code >= 300) {
    throw new HttpError_(500, 'Drive upload failed (' + code + '): ' + text);
  }
  return JSON.parse(text);
}

function driveApiAnyoneWithLink_(fileId) {
  var response = UrlFetchApp.fetch(
    'https://www.googleapis.com/drive/v3/files/' +
      encodeURIComponent(fileId) +
      '/permissions',
    {
      method: 'post',
      contentType: 'application/json',
      headers: {
        Authorization: 'Bearer ' + getDriveOAuthToken_(),
      },
      payload: JSON.stringify({
        role: 'reader',
        type: 'anyone',
      }),
      muteHttpExceptions: true,
    },
  );
  var code = response.getResponseCode();
  if (code < 200 || code >= 300) {
    // не блокируем upload, если шаринг не прошёл
    Logger.log('Drive permission warn: ' + response.getContentText());
  }
}

function driveApiTrashFile_(fileId) {
  var response = UrlFetchApp.fetch(
    'https://www.googleapis.com/drive/v3/files/' + encodeURIComponent(fileId),
    {
      method: 'patch',
      contentType: 'application/json',
      headers: {
        Authorization: 'Bearer ' + getDriveOAuthToken_(),
      },
      payload: JSON.stringify({ trashed: true }),
      muteHttpExceptions: true,
    },
  );
  var code = response.getResponseCode();
  if (code < 200 || code >= 300) {
    Logger.log('Drive trash warn: ' + response.getContentText());
  }
}

function driveApiGetFolder_(folderId) {
  var response = UrlFetchApp.fetch(
    'https://www.googleapis.com/drive/v3/files/' +
      encodeURIComponent(folderId) +
      '?fields=id,name,mimeType',
    {
      method: 'get',
      headers: {
        Authorization: 'Bearer ' + getDriveOAuthToken_(),
      },
      muteHttpExceptions: true,
    },
  );
  var code = response.getResponseCode();
  var text = response.getContentText();
  if (code < 200 || code >= 300) {
    throw new HttpError_(500, 'Drive folder access failed (' + code + '): ' + text);
  }
  return JSON.parse(text);
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

  // проверка доступа к папке через Drive API (не DriveApp)
  driveApiGetFolder_(folderId);

  var bytes = Utilities.base64Decode(body.contentBase64);
  var fileName = body.fileName
    ? String(body.fileName)
    : 'part-' + partId + '-' + Date.now() + ext;
  var created = driveApiCreateFile_(folderId, fileName, mimeType, bytes);
  driveApiAnyoneWithLink_(created.id);
  var fileUrl = 'https://drive.google.com/uc?export=view&id=' + created.id;

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
    driveApiTrashFile_(fileId);
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

/**
 * Запустить один раз: authorizeDrive → Выполнить → Allow.
 * Потом веб-приложение → Новая версия.
 */
function authorizeDrive() {
  var folderId = getDrivePhotosFolderId_() || '1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC';
  driveApiGetFolder_(folderId);
  var created = driveApiCreateFile_(
    folderId,
    'auth-test.txt',
    'text/plain',
    Utilities.newBlob('ok', 'text/plain').getBytes(),
  );
  driveApiTrashFile_(created.id);
}
