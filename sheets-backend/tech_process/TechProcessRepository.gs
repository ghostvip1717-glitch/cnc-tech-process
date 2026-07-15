/**
 * tech_processes / setups / operations sheet access.
 */

function techProcessRepoFindByPartId_(partId) {
  var rows = sheetRows_(SHEET_NAMES.TECH_PROCESSES);
  for (var i = 0; i < rows.length; i++) {
    if (Number(rows[i].part_id) === Number(partId)) {
      return rows[i];
    }
  }
  return null;
}

function techProcessRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.TECH_PROCESSES, row);
}

function techProcessRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.TECH_PROCESSES, rowNumber);
}

function setupsRepoListByTp_(techProcessId) {
  return sheetRows_(SHEET_NAMES.SETUPS)
    .filter(function (row) {
      return Number(row.tech_process_id) === Number(techProcessId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function setupsRepoFindById_(setupId) {
  return sheetFindById_(SHEET_NAMES.SETUPS, setupId);
}

function setupsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.SETUPS, row);
}

function setupsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.SETUPS, rowNumber, row);
}

function setupsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.SETUPS, rowNumber);
}

function operationsRepoListBySetup_(setupId) {
  return sheetRows_(SHEET_NAMES.OPERATIONS)
    .filter(function (row) {
      return Number(row.setup_id) === Number(setupId);
    })
    .sort(function (a, b) {
      return Number(a.order) - Number(b.order);
    });
}

function operationsRepoFindById_(operationId) {
  return sheetFindById_(SHEET_NAMES.OPERATIONS, operationId);
}

function operationsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.OPERATIONS, row);
}

function operationsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.OPERATIONS, rowNumber, row);
}

function operationsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.OPERATIONS, rowNumber);
}

function techProcessDeleteCascade_(tp) {
  var setups = setupsRepoListByTp_(tp.id);
  var opRows = [];
  for (var i = 0; i < setups.length; i++) {
    opRows = opRows.concat(operationsRepoListBySetup_(setups[i].id));
  }
  opRows.sort(function (a, b) {
    return b.__row - a.__row;
  });
  for (var j = 0; j < opRows.length; j++) {
    operationsRepoDelete_(opRows[j].__row);
  }

  setups
    .slice()
    .sort(function (a, b) {
      return b.__row - a.__row;
    })
    .forEach(function (setup) {
      setupsRepoDelete_(setup.__row);
    });

  techProcessRepoDelete_(tp.__row);
}
