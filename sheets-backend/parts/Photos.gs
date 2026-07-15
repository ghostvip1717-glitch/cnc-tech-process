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
