/**
 * Tech process: one TP per part, setups, operations, reorder.
 */

var ROMAN_ORDERS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];

function setupOrderLabel_(order) {
  var n = Number(order);
  if (n >= 0 && n < ROMAN_ORDERS.length) {
    return ROMAN_ORDERS[n];
  }
  return String(n + 1);
}

function findTechProcessByPartId_(partId) {
  var rows = rowsToObjects_(SHEET_NAMES.TECH_PROCESSES);
  for (var i = 0; i < rows.length; i++) {
    if (Number(rows[i].part_id) === Number(partId)) {
      return rows[i];
    }
  }
  return null;
}

function requirePart_(partId) {
  var part = findById_(SHEET_NAMES.PARTS, partId);
  if (!part) {
    throw new HttpError_(404, 'Part not found');
  }
  return part;
}

function techProcessGet_(partId) {
  requirePart_(partId);
  var tp = findTechProcessByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  return okResponse_(serializeTechProcess_(tp));
}

function techProcessCreate_(partId) {
  requirePart_(partId);
  var existing = findTechProcessByPartId_(partId);
  if (existing) {
    throw new HttpError_(409, 'Tech process already exists for this part');
  }
  var id = nextId_(SHEET_NAMES.TECH_PROCESSES);
  var row = { id: id, part_id: partId };
  appendObject_(SHEET_NAMES.TECH_PROCESSES, row);
  return okResponse_(serializeTechProcess_(row));
}

function getOrCreateTechProcess_(partId) {
  var tp = findTechProcessByPartId_(partId);
  if (tp) {
    return tp;
  }
  var id = nextId_(SHEET_NAMES.TECH_PROCESSES);
  var row = { id: id, part_id: partId };
  appendObject_(SHEET_NAMES.TECH_PROCESSES, row);
  return findTechProcessByPartId_(partId);
}

function setupCreate_(partId, body) {
  requirePart_(partId);
  if (!body || body.jaw_id === undefined || body.jaw_id === null) {
    throw new HttpError_(422, 'jaw_id is required');
  }
  if (!getCatalogItemByIdAndType_(body.jaw_id, 'jaw')) {
    throw new HttpError_(422, 'Invalid jaw_id');
  }

  var tp = getOrCreateTechProcess_(partId);
  var setups = listSetupRows_(tp.id);
  var nextOrder = 0;
  for (var i = 0; i < setups.length; i++) {
    var o = Number(setups[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }

  var id = nextId_(SHEET_NAMES.SETUPS);
  var row = {
    id: id,
    tech_process_id: toInt_(tp.id),
    order: nextOrder,
    jaw_id: toInt_(body.jaw_id),
  };
  appendObject_(SHEET_NAMES.SETUPS, row);
  return okResponse_(serializeSetup_(row), 201);
}

function setupUpdate_(partId, setupId, body) {
  var setup = requireSetupForPart_(partId, setupId);
  if (!body || body.jaw_id === undefined || body.jaw_id === null) {
    throw new HttpError_(422, 'jaw_id is required');
  }
  if (!getCatalogItemByIdAndType_(body.jaw_id, 'jaw')) {
    throw new HttpError_(422, 'Invalid jaw_id');
  }
  var updated = {
    id: toInt_(setup.id),
    tech_process_id: toInt_(setup.tech_process_id),
    order: toInt_(setup.order),
    jaw_id: toInt_(body.jaw_id),
  };
  updateObject_(SHEET_NAMES.SETUPS, setup.__row, updated);
  return okResponse_(serializeSetup_(updated));
}

function setupDelete_(partId, setupId) {
  var setup = requireSetupForPart_(partId, setupId);
  var ops = listOperationRows_(setupId).slice().sort(function (a, b) {
    return b.__row - a.__row;
  });
  for (var i = 0; i < ops.length; i++) {
    deleteRow_(SHEET_NAMES.OPERATIONS, ops[i].__row);
  }
  deleteRow_(SHEET_NAMES.SETUPS, setup.__row);
  return okResponse_(null, 204);
}

function operationCreate_(partId, setupId, body) {
  requireSetupForPart_(partId, setupId);
  if (!body || !body.op_number || !body.title || !body.tool_id || !body.plate_id) {
    throw new HttpError_(422, 'op_number, title, tool_id, plate_id are required');
  }
  if (!getCatalogItemByIdAndType_(body.tool_id, 'tool')) {
    throw new HttpError_(422, 'Invalid tool_id');
  }
  if (!getCatalogItemByIdAndType_(body.plate_id, 'plate')) {
    throw new HttpError_(422, 'Invalid plate_id');
  }

  var ops = listOperationRows_(setupId);
  var nextOrder = 0;
  for (var i = 0; i < ops.length; i++) {
    var o = Number(ops[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }

  var id = nextId_(SHEET_NAMES.OPERATIONS);
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
  appendObject_(SHEET_NAMES.OPERATIONS, row);
  return okResponse_(serializeOperation_(row), 201);
}

function operationUpdate_(partId, operationId, body) {
  var op = requireOperationForPart_(partId, operationId);
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
  if (body.tool_id !== undefined && !getCatalogItemByIdAndType_(toolId, 'tool')) {
    throw new HttpError_(422, 'Invalid tool_id');
  }
  if (body.plate_id !== undefined && !getCatalogItemByIdAndType_(plateId, 'plate')) {
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
  updateObject_(SHEET_NAMES.OPERATIONS, op.__row, updated);
  return okResponse_(serializeOperation_(updated));
}

function operationDelete_(partId, operationId) {
  var op = requireOperationForPart_(partId, operationId);
  deleteRow_(SHEET_NAMES.OPERATIONS, op.__row);
  return okResponse_(null, 204);
}

function operationsReorder_(partId, setupId, body) {
  requireSetupForPart_(partId, setupId);
  if (!body || !body.operation_ids || !(body.operation_ids instanceof Array)) {
    throw new HttpError_(422, 'operation_ids is required');
  }
  var ops = listOperationRows_(setupId);
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
    updateObject_(SHEET_NAMES.OPERATIONS, row.__row, updated);
    result.push(serializeOperation_(updated));
  }
  return okResponse_(result);
}

function requireSetupForPart_(partId, setupId) {
  requirePart_(partId);
  var tp = findTechProcessByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  var setup = findById_(SHEET_NAMES.SETUPS, setupId);
  if (!setup || Number(setup.tech_process_id) !== Number(tp.id)) {
    throw new HttpError_(404, 'Setup not found');
  }
  return setup;
}

function requireOperationForPart_(partId, operationId) {
  requirePart_(partId);
  var tp = findTechProcessByPartId_(partId);
  if (!tp) {
    throw new HttpError_(404, 'Tech process not found');
  }
  var op = findById_(SHEET_NAMES.OPERATIONS, operationId);
  if (!op) {
    throw new HttpError_(404, 'Operation not found');
  }
  var setup = findById_(SHEET_NAMES.SETUPS, op.setup_id);
  if (!setup || Number(setup.tech_process_id) !== Number(tp.id)) {
    throw new HttpError_(404, 'Operation not found');
  }
  return op;
}

function listSetupRows_(techProcessId) {
  return rowsToObjects_(SHEET_NAMES.SETUPS)
    .filter(function (row) {
      return Number(row.tech_process_id) === Number(techProcessId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function listOperationRows_(setupId) {
  return rowsToObjects_(SHEET_NAMES.OPERATIONS)
    .filter(function (row) {
      return Number(row.setup_id) === Number(setupId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function deleteTechProcessCascade_(tp) {
  var setups = listSetupRows_(tp.id);
  var opRows = [];
  for (var i = 0; i < setups.length; i++) {
    opRows = opRows.concat(listOperationRows_(setups[i].id));
  }
  opRows.sort(function (a, b) {
    return b.__row - a.__row;
  });
  for (var j = 0; j < opRows.length; j++) {
    deleteRow_(SHEET_NAMES.OPERATIONS, opRows[j].__row);
  }

  setups
    .slice()
    .sort(function (a, b) {
      return b.__row - a.__row;
    })
    .forEach(function (setup) {
      deleteRow_(SHEET_NAMES.SETUPS, setup.__row);
    });

  deleteRow_(SHEET_NAMES.TECH_PROCESSES, tp.__row);
}

function serializeOperation_(row) {
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

function serializeSetup_(row) {
  return {
    id: toInt_(row.id),
    tech_process_id: toInt_(row.tech_process_id),
    order: toInt_(row.order),
    order_label: setupOrderLabel_(row.order),
    jaw_id: toInt_(row.jaw_id),
    operations: listOperationRows_(row.id).map(serializeOperation_),
  };
}

function serializeTechProcess_(row) {
  return {
    id: toInt_(row.id),
    part_id: toInt_(row.part_id),
    setups: listSetupRows_(row.id).map(serializeSetup_),
  };
}
