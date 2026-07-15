/**
 * Unique tools / plates / jaws required for a part.
 */

function assemblyRequiredItems_(partId) {
  partsRequire_(partId);
  var tp = techProcessRepoFindByPartId_(partId);
  if (!tp) {
    return okResponse_({ tools: [], plates: [], jaws: [] });
  }

  var toolIds = {};
  var plateIds = {};
  var jawIds = {};

  var setups = setupsRepoListByTp_(tp.id);
  for (var i = 0; i < setups.length; i++) {
    jawIds[Number(setups[i].jaw_id)] = true;
    var ops = operationsRepoListBySetup_(setups[i].id);
    for (var j = 0; j < ops.length; j++) {
      toolIds[Number(ops[j].tool_id)] = true;
      plateIds[Number(ops[j].plate_id)] = true;
    }
  }

  return okResponse_({
    tools: assemblyResolveIds_(Object.keys(toolIds), 'tool'),
    plates: assemblyResolveIds_(Object.keys(plateIds), 'plate'),
    jaws: assemblyResolveIds_(Object.keys(jawIds), 'jaw'),
  });
}

function assemblyResolveIds_(ids, expectedType) {
  var items = [];
  for (var i = 0; i < ids.length; i++) {
    var item = catalogGetByIdAndType_(Number(ids[i]), expectedType);
    if (item) {
      items.push({ id: item.id, type: item.type, name: item.name });
    }
  }
  items.sort(function (a, b) {
    return a.id - b.id;
  });
  return items;
}
