/**
 * JSON envelope helpers.
 * GAS Web App always returns HTTP 200; app status is in httpStatus.
 */

function HttpError_(status, detail) {
  this.status = status;
  this.detail = detail;
  this.name = 'HttpError';
}

function okResponse_(data, httpStatus) {
  return {
    ok: true,
    httpStatus: httpStatus === undefined ? 200 : httpStatus,
    data: data === undefined ? null : data,
  };
}

function errResponse_(httpStatus, detail) {
  return {
    ok: false,
    httpStatus: httpStatus,
    detail: detail,
  };
}

function jsonOutput_(payload) {
  return ContentService.createTextOutput(JSON.stringify(payload)).setMimeType(
    ContentService.MimeType.JSON,
  );
}
