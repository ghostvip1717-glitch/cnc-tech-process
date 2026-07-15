/**
 * parts sheet access.
 */

function partsRepoList_() {
  return sheetRows_(SHEET_NAMES.PARTS);
}

function partsRepoFindById_(id) {
  return sheetFindById_(SHEET_NAMES.PARTS, id);
}

function partsRepoFindByNumber_(number) {
  var rows = partsRepoList_();
  for (var i = 0; i < rows.length; i++) {
    if (String(rows[i].number) === number) {
      return rows[i];
    }
  }
  return null;
}

function partsRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.PARTS, row);
}

function partsRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.PARTS, rowNumber, row);
}

function partsRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.PARTS, rowNumber);
}

function photosRepoListByPart_(partId) {
  return sheetRows_(SHEET_NAMES.PART_PHOTOS)
    .filter(function (row) {
      return Number(row.part_id) === Number(partId);
    })
    .sort(function (a, b) {
      return Number(a.sort_order) - Number(b.sort_order);
    });
}

function photosRepoFind_(partId, photoId) {
  var photos = photosRepoListByPart_(partId);
  for (var i = 0; i < photos.length; i++) {
    if (Number(photos[i].id) === Number(photoId)) {
      return photos[i];
    }
  }
  return null;
}

function photosRepoInsert_(row) {
  sheetAppend_(SHEET_NAMES.PART_PHOTOS, row);
}

function photosRepoUpdate_(rowNumber, row) {
  sheetUpdate_(SHEET_NAMES.PART_PHOTOS, rowNumber, row);
}

function photosRepoDelete_(rowNumber) {
  sheetDeleteRow_(SHEET_NAMES.PART_PHOTOS, rowNumber);
}
