/**
 * AUTO-GENERATED from pure/techProcessRules.js — do not edit by hand.
 * Run: npm run sync:rules
 */

var ROMAN_ORDERS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];
var CATALOG_TYPES = ['tool', 'plate', 'jaw'];

function setupOrderLabel_(order) {
  var n = Number(order);
  if (n >= 0 && n < ROMAN_ORDERS.length) {
    return ROMAN_ORDERS[n];
  }
  return String(n + 1);
}

function isValidCatalogType_(type) {
  return CATALOG_TYPES.indexOf(String(type)) !== -1;
}

/**
 * @returns {{ok:true}|{ok:false,detail:string}}
 */
function validateCatalogItemType_(actualType, expectedType, fieldName) {
  if (String(actualType) !== String(expectedType)) {
    return { ok: false, detail: 'Invalid ' + fieldName };
  }
  return { ok: true };
}

/**
 * Validate that orderedIds is a permutation of existingIds.
 * @returns {{ok:true, orderById: Object}|{ok:false,detail:string}}
 */
function validateReorderIds_(orderedIds, existingIds) {
  if (!orderedIds || !(orderedIds instanceof Array)) {
    return { ok: false, detail: 'operation_ids is required' };
  }
  if (orderedIds.length !== existingIds.length) {
    return { ok: false, detail: 'operation_ids must contain all operation ids' };
  }
  var existing = {};
  for (var i = 0; i < existingIds.length; i++) {
    existing[Number(existingIds[i])] = true;
  }
  var seen = {};
  var orderById = {};
  for (var j = 0; j < orderedIds.length; j++) {
    var id = Number(orderedIds[j]);
    if (!existing[id] || seen[id]) {
      return { ok: false, detail: 'operation_ids must contain all operation ids' };
    }
    seen[id] = true;
    orderById[id] = j;
  }
  return { ok: true, orderById: orderById };
}

/**
 * Plan cascade delete order: operations (high __row first), then setups, then tp.
 */
function planTechProcessCascadeDelete_(tp, setups, operations) {
  var opRows = (operations || []).slice().sort(function (a, b) {
    return b.__row - a.__row;
  });
  var setupRows = (setups || []).slice().sort(function (a, b) {
    return b.__row - a.__row;
  });
  return {
    operations: opRows,
    setups: setupRows,
    techProcess: tp,
  };
}

function nextSetupOrder_(setups) {
  var nextOrder = 0;
  for (var i = 0; i < (setups || []).length; i++) {
    var o = Number(setups[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }
  return nextOrder;
}

function nextOperationOrder_(operations) {
  var nextOrder = 0;
  for (var i = 0; i < (operations || []).length; i++) {
    var o = Number(operations[i].order);
    if (o >= nextOrder) {
      nextOrder = o + 1;
    }
  }
  return nextOrder;
}
