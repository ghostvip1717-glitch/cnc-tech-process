/**
 * catalog_items sheet access.
 */

function catalogRepoList_() {
  return sheetRows_(SHEET_NAMES.CATALOG);
}

function catalogRepoFindById_(id) {
  return sheetFindById_(SHEET_NAMES.CATALOG, id);
}

function catalogRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.CATALOG, row);
}

function catalogRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.CATALOG, rowNumber, row);
}

function catalogRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.CATALOG, rowNumber);
}

function catalogRepoIsReferenced_(itemId) {
  var setups = sheetRows_(SHEET_NAMES.SETUPS);
  for (var i = 0; i < setups.length; i++) {
    if (Number(setups[i].jaw_id) === Number(itemId)) {
      return true;
    }
  }
  var operations = sheetRows_(SHEET_NAMES.OPERATIONS);
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

function catalogRepoFindByTypeAndName_(type, name) {
  var rows = catalogRepoList_();
  for (var i = 0; i < rows.length; i++) {
    if (String(rows[i].type) === type && String(rows[i].name) === name) {
      return rows[i];
    }
  }
  return null;
}
