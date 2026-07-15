/**
 * One-time spreadsheet bootstrap.
 *
 * In Apps Script editor: run initializeSpreadsheet from the function dropdown.
 * Or open the spreadsheet and use menu CNC Tech Process → Initialize sheets.
 */

function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('CNC Tech Process')
    .addItem('Initialize sheets', 'initializeSpreadsheet')
    .addToUi();
}

function initializeSpreadsheet() {
  var config = getConfig_();
  if (!config.spreadsheetId) {
    var active = SpreadsheetApp.getActiveSpreadsheet();
    if (!active) {
      throw new Error('Set SPREADSHEET_ID in Script Properties or open the spreadsheet and run from there.');
    }
    PropertiesService.getScriptProperties().setProperty('SPREADSHEET_ID', active.getId());
  }

  ensureSheetsInitialized_();

  var ss = getSpreadsheet_();
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (!SHEET_HEADERS[name] && sheets.length > 1) {
      // keep unknown sheets; do not auto-delete
    }
  }

  var message = 'Sheets ready: ' + Object.keys(SHEET_HEADERS).join(', ');
  Logger.log(message);
  try {
    SpreadsheetApp.getUi().alert(message);
  } catch (e) {
    // no UI when run from unbound script editor
  }
}

/**
 * Optional clasp/manual helper: create blank spreadsheet and print ids.
 * Run once, then copy ids into Script Properties.
 */
function createBlankSpreadsheetForProject() {
  var ss = SpreadsheetApp.create('cnc-tech-process-data');
  PropertiesService.getScriptProperties().setProperty('SPREADSHEET_ID', ss.getId());
  ensureSheetsInitialized_();

  var folder = DriveApp.createFolder('cnc-tech-process-photos');
  PropertiesService.getScriptProperties().setProperty('DRIVE_FOLDER_ID', folder.getId());

  Logger.log('SPREADSHEET_ID=' + ss.getId());
  Logger.log('DRIVE_FOLDER_ID=' + folder.getId());
  Logger.log('Spreadsheet URL: ' + ss.getUrl());
  return {
    spreadsheetId: ss.getId(),
    driveFolderId: folder.getId(),
    spreadsheetUrl: ss.getUrl(),
  };
}
