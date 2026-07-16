/**
 * Tech process: one per part, setups, operations, reorder.
 * Labels / reorder / cascade planning: TechProcessRules.gs (pure).
 */

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
  var nextOrder = nextSetupOrder_(setups);

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
  var nextOrder = nextOperationOrder_(ops);

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
  var ops = operationsRepoListBySetup_(setupId);
  var existingIds = ops.map(function (row) {
    return Number(row.id);
  });
  var check = validateReorderIds_(body && body.operation_ids, existingIds);
  if (!check.ok) {
    throw new HttpError_(422, check.detail);
  }

  var byId = {};
  for (var i = 0; i < ops.length; i++) {
    byId[Number(ops[i].id)] = ops[i];
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
