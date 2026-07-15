/**
 * Вставь в конец Code.gs, выбери authorizeDrive → Выполнить → Разрешить (Диск).
 * Затем веб-приложение → Новая версия.
 */
function authorizeDrive() {
  var folderId = '1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC';
  var folder = DriveApp.getFolderById(folderId);
  var file = folder.createFile('auth-test.txt', 'ok', MimeType.PLAIN_TEXT);
  file.setTrashed(true);
}
