/**
 * Parts module: CRUD + Drive photo upload/delete/reorder.
 */

function partsList_(query) {
  var q = query && query.q ? String(query.q).toLowerCase() : null;
  var parts = rowsToObjects_(SHEET_NAMES.PARTS).map(function (row) {
    return serializePart_(row);
  });
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
  var part = findById_(SHEET_NAMES.PARTS, partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  return okResponse_(serializePart_(part));
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

  var existing = rowsToObjects_(SHEET_NAMES.PARTS);
  for (var i = 0; i < existing.length; i++) {
    if (String(existing[i].number) === number) {
      throw new HttpError_(409, 'Part with this number already exists');
    }
  }

  var id = nextId_(SHEET_NAMES.PARTS);
  var createdAt = new Date().toISOString();
  var row = { id: id, number: number, title: title, created_at: createdAt };
  appendObject_(SHEET_NAMES.PARTS, row);
  return okResponse_(serializePart_(row), 201);
}

function partsUpdate_(partId, body) {
  var part = findById_(SHEET_NAMES.PARTS, partId);
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

  var existing = rowsToObjects_(SHEET_NAMES.PARTS);
  for (var i = 0; i < existing.length; i++) {
    if (Number(existing[i].id) !== Number(partId) && String(existing[i].number) === number) {
      throw new HttpError_(409, 'Part with this number already exists');
    }
  }

  var updated = {
    id: toInt_(part.id),
    number: number,
    title: title,
    created_at: String(part.created_at),
  };
  updateObject_(SHEET_NAMES.PARTS, part.__row, updated);
  return okResponse_(serializePart_(updated));
}

function partsDelete_(partId) {
  var part = findById_(SHEET_NAMES.PARTS, partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }

  var photos = listPhotoRows_(partId).slice().sort(function (a, b) {
    return b.__row - a.__row;
  });
  for (var i = 0; i < photos.length; i++) {
    deleteDriveFileSafe_(photos[i].drive_file_id);
    deleteRow_(SHEET_NAMES.PART_PHOTOS, photos[i].__row);
  }

  var tp = findTechProcessByPartId_(partId);
  if (tp) {
    deleteTechProcessCascade_(tp);
  }

  deleteRow_(SHEET_NAMES.PARTS, part.__row);
  return okResponse_(null, 204);
}

function partsUploadPhoto_(partId, body) {
  var part = findById_(SHEET_NAMES.PARTS, partId);
  if (!part) {
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

  var config = getConfig_();
  if (!config.driveFolderId) {
    throw new HttpError_(500, 'DRIVE_FOLDER_ID is not configured');
  }

  var folder = DriveApp.getFolderById(config.driveFolderId);
  var bytes = Utilities.base64Decode(body.contentBase64);
  var blob = Utilities.newBlob(bytes, mimeType, (body.fileName || 'photo') + '');
  if (!blob.getName() || blob.getName() === 'photo') {
    blob.setName('part-' + partId + '-' + Date.now() + ext);
  }

  var file = folder.createFile(blob);
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  var fileId = file.getId();
  var fileUrl = 'https://drive.google.com/uc?export=view&id=' + fileId;

  var photos = listPhotoRows_(partId);
  var sortOrder = 0;
  for (var i = 0; i < photos.length; i++) {
    var so = Number(photos[i].sort_order);
    if (so >= sortOrder) {
      sortOrder = so + 1;
    }
  }

  var id = nextId_(SHEET_NAMES.PART_PHOTOS);
  var row = {
    id: id,
    part_id: partId,
    file_url: fileUrl,
    sort_order: sortOrder,
    drive_file_id: fileId,
  };
  appendObject_(SHEET_NAMES.PART_PHOTOS, row);
  return okResponse_(serializePhoto_(row), 201);
}

function partsDeletePhoto_(partId, photoId) {
  var photo = findPhoto_(partId, photoId);
  if (!photo) {
    throw new HttpError_(404, 'Photo not found');
  }
  deleteDriveFileSafe_(photo.drive_file_id);
  deleteRow_(SHEET_NAMES.PART_PHOTOS, photo.__row);
  return okResponse_(null, 204);
}

function partsReorderPhotos_(partId, body) {
  if (!findById_(SHEET_NAMES.PARTS, partId)) {
    throw new HttpError_(404, 'Part not found');
  }
  if (!body || !body.photo_ids || !(body.photo_ids instanceof Array)) {
    throw new HttpError_(422, 'photo_ids is required');
  }

  var photos = listPhotoRows_(partId);
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
      drive_file_id: String(row.drive_file_id || ''),
    };
    updateObject_(SHEET_NAMES.PART_PHOTOS, row.__row, updated);
    result.push(serializePhoto_(updated));
  }
  return okResponse_(result);
}

function listPhotoRows_(partId) {
  return rowsToObjects_(SHEET_NAMES.PART_PHOTOS)
    .filter(function (row) {
      return Number(row.part_id) === Number(partId);
    })
    .sort(function (a, b) {
      return Number(a.sort_order) - Number(b.sort_order);
    });
}

function findPhoto_(partId, photoId) {
  var photos = listPhotoRows_(partId);
  for (var i = 0; i < photos.length; i++) {
    if (Number(photos[i].id) === Number(photoId)) {
      return photos[i];
    }
  }
  return null;
}

function deleteDriveFileSafe_(fileId) {
  if (!fileId) {
    return;
  }
  try {
    DriveApp.getFileById(String(fileId)).setTrashed(true);
  } catch (e) {
    // ignore missing Drive file
  }
}

function serializePhoto_(row) {
  var fileUrl = String(row.file_url || '');
  var driveId = String(row.drive_file_id || '');
  return {
    id: toInt_(row.id),
    part_id: toInt_(row.part_id),
    file_path: driveId ? 'drive/' + driveId : fileUrl,
    url: fileUrl,
    sort_order: toInt_(row.sort_order),
  };
}

function serializePart_(row) {
  var partId = toInt_(row.id);
  return {
    id: partId,
    number: String(row.number),
    title: String(row.title),
    created_at: normalizeIsoDate_(row.created_at),
    photos: listPhotoRows_(partId).map(serializePhoto_),
  };
}

function normalizeIsoDate_(value) {
  if (Object.prototype.toString.call(value) === '[object Date]') {
    return value.toISOString();
  }
  return String(value);
}
