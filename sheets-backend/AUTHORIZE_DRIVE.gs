/**
 * Уже есть в ONE_FILE.gs / Photos.gs.
 * Выбери authorizeDrive → Выполнить → Allow → New version Web App.
 */
function authorizeDrive() {
  var folderId = getDrivePhotosFolderId_() || '1fgbnnDIjqVMECUKleD-NPGbwZAUyhuNC';
  driveApiGetFolder_(folderId);
  var created = driveApiCreateFile_(
    folderId,
    'auth-test.txt',
    'text/plain',
    Utilities.newBlob('ok', 'text/plain').getBytes(),
  );
  driveApiTrashFile_(created.id);
}
