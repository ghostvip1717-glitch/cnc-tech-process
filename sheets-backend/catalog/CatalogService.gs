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
