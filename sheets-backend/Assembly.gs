/**
 * Assembly: unique tools/plates/jaws required for a part.
 */

function assemblyRequiredItems_(partId) {
  requirePart_(partId);
  var tp = findTechProcessByPartId_(partId);
  if (!tp) {
    return okResponse_({ tools: [], plates: [], jaws: [] });
  }

  var toolIds = {};
  var plateIds = {};
  var jawIds = {};

  var setups = listSetupRows_(tp.id);
  for (var i = 0; i < setups.length; i++) {
    jawIds[Number(setups[i].jaw_id)] = true;
    var ops = listOperationRows_(setups[i].id);
    for (var j = 0; j < ops.length; j++) {
      toolIds[Number(ops[j].tool_id)] = true;
      plateIds[Number(ops[j].plate_id)] = true;
    }
  }

  return okResponse_({
    tools: resolveCatalogIds_(Object.keys(toolIds), 'tool'),
    plates: resolveCatalogIds_(Object.keys(plateIds), 'plate'),
    jaws: resolveCatalogIds_(Object.keys(jawIds), 'jaw'),
  });
}

function resolveCatalogIds_(ids, expectedType) {
  var items = [];
  for (var i = 0; i < ids.length; i++) {
    var item = getCatalogItemByIdAndType_(Number(ids[i]), expectedType);
    if (item) {
      items.push({ id: item.id, type: item.type, name: item.name });
    }
  }
  items.sort(function (a, b) {
    return a.id - b.id;
  });
  return items;
}
