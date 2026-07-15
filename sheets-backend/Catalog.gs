/**
 * Catalog module: tools, plates, jaws.
 */

function catalogList_(query) {
  var typeFilter = query && query.type ? String(query.type) : null;
  var q = query && query.q ? String(query.q).toLowerCase() : null;
  var items = rowsToObjects_(SHEET_NAMES.CATALOG)
    .map(serializeCatalogItem_)
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
  var item = findById_(SHEET_NAMES.CATALOG, itemId);
  if (!item) {
    throw new HttpError_(404, 'Catalog item not found');
  }
  return okResponse_(serializeCatalogItem_(item));
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

  var existing = rowsToObjects_(SHEET_NAMES.CATALOG);
  for (var i = 0; i < existing.length; i++) {
    if (String(existing[i].type) === type && String(existing[i].name) === name) {
      throw new HttpError_(409, 'Item with this name already exists for type ' + type);
    }
  }

  var id = nextId_(SHEET_NAMES.CATALOG);
  var row = { id: id, type: type, name: name, note: note === null ? '' : note };
  appendObject_(SHEET_NAMES.CATALOG, row);
  return okResponse_(serializeCatalogItem_(row), 201);
}

function catalogUpdate_(itemId, body) {
  var item = findById_(SHEET_NAMES.CATALOG, itemId);
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

  var existing = rowsToObjects_(SHEET_NAMES.CATALOG);
  for (var i = 0; i < existing.length; i++) {
    if (
      Number(existing[i].id) !== Number(itemId) &&
      String(existing[i].type) === String(item.type) &&
      String(existing[i].name) === name
    ) {
      throw new HttpError_(409, 'Item with this name already exists for type ' + item.type);
    }
  }

  var updated = {
    id: toInt_(item.id),
    type: String(item.type),
    name: name,
    note: note === null ? '' : note,
  };
  updateObject_(SHEET_NAMES.CATALOG, item.__row, updated);
  return okResponse_(serializeCatalogItem_(updated));
}

function catalogDelete_(itemId) {
  var item = findById_(SHEET_NAMES.CATALOG, itemId);
  if (!item) {
    throw new HttpError_(404, 'Catalog item not found');
  }
  if (isCatalogItemReferenced_(itemId)) {
    throw new HttpError_(409, 'Catalog item is used in tech process and cannot be deleted');
  }
  deleteRow_(SHEET_NAMES.CATALOG, item.__row);
  return okResponse_(null, 204);
}

function isCatalogItemReferenced_(itemId) {
  var setups = rowsToObjects_(SHEET_NAMES.SETUPS);
  for (var i = 0; i < setups.length; i++) {
    if (Number(setups[i].jaw_id) === Number(itemId)) {
      return true;
    }
  }
  var operations = rowsToObjects_(SHEET_NAMES.OPERATIONS);
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

function getCatalogItemByIdAndType_(itemId, expectedType) {
  var item = findById_(SHEET_NAMES.CATALOG, itemId);
  if (!item || String(item.type) !== expectedType) {
    return null;
  }
  return serializeCatalogItem_(item);
}

function serializeCatalogItem_(row) {
  return {
    id: toInt_(row.id),
    type: String(row.type),
    name: String(row.name),
    note: emptyToNull_(row.note),
  };
}
