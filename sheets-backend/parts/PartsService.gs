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
