/**
 * Web App entry: doGet / doPost + router.
 *
 * HTTP contract (единственный формат):
 *   GET  .../exec?path=/health
 *   POST .../exec  Content-Type: text/plain
 *   {
 *     "path": "/api/v1/catalog",
 *     "method": "GET",
 *     "query": { "type": "tool" },
 *     "body": null,
 *     "initData": "..."
 *   }
 *
 * Response envelope:
 *   { "ok": true, "httpStatus": 200, "data": ... }
 *   { "ok": false, "httpStatus": 401, "detail": "..." }
 */

function doGet(e) {
  return handleRequest_(e || {});
}

function doPost(e) {
  return handleRequest_(e || {});
}

function handleRequest_(e) {
  try {
    var req = parseRequestEnvelope_(e);
    requireAuth_(req.path, req.initData);
    var result = routeRequest_(req);
    return jsonOutput_(result);
  } catch (err) {
    if (err && err.name === 'HttpError') {
      return jsonOutput_(errResponse_(err.status, err.detail));
    }
    var message = err && err.message ? err.message : String(err);
    return jsonOutput_(errResponse_(500, message));
  }
}

function routeRequest_(req) {
  var path = req.path;
  var method = req.method;
  var query = req.query || {};
  var body = req.body;
  var params;

  if (path === '/health' && method === 'GET') {
    return okResponse_({ status: 'OK' });
  }

  if (path === '/api/v1/catalog' && method === 'GET') {
    return catalogList_(query);
  }
  if (path === '/api/v1/catalog' && method === 'POST') {
    return catalogCreate_(body);
  }
  params = matchPath_('/api/v1/catalog/{itemId}', path);
  if (params) {
    if (method === 'GET') {
      return catalogGet_(params.itemId);
    }
    if (method === 'PATCH') {
      return catalogUpdate_(params.itemId, body);
    }
    if (method === 'DELETE') {
      return catalogDelete_(params.itemId);
    }
  }

  if (path === '/api/v1/parts' && method === 'GET') {
    return partsList_(query);
  }
  if (path === '/api/v1/parts' && method === 'POST') {
    return partsCreate_(body);
  }
  params = matchPath_('/api/v1/parts/{partId}', path);
  if (params) {
    if (method === 'GET') {
      return partsGet_(params.partId);
    }
    if (method === 'PATCH') {
      return partsUpdate_(params.partId, body);
    }
    if (method === 'DELETE') {
      return partsDelete_(params.partId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/photos', path);
  if (params && method === 'POST') {
    return photosUpload_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/photos/reorder', path);
  if (params && method === 'PATCH') {
    return photosReorder_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/photos/{photoId}', path);
  if (params && method === 'DELETE') {
    return photosDelete_(params.partId, params.photoId);
  }

  params = matchPath_('/api/v1/parts/{partId}/tech-process', path);
  if (params) {
    if (method === 'GET') {
      return techProcessGet_(params.partId);
    }
    if (method === 'PUT') {
      return techProcessCreate_(params.partId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/tech-process/setups', path);
  if (params && method === 'POST') {
    return setupCreate_(params.partId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/tech-process/setups/{setupId}', path);
  if (params) {
    if (method === 'PATCH') {
      return setupUpdate_(params.partId, params.setupId, body);
    }
    if (method === 'DELETE') {
      return setupDelete_(params.partId, params.setupId);
    }
  }

  params = matchPath_(
    '/api/v1/parts/{partId}/tech-process/setups/{setupId}/operations',
    path,
  );
  if (params && method === 'POST') {
    return operationCreate_(params.partId, params.setupId, body);
  }
  params = matchPath_(
    '/api/v1/parts/{partId}/tech-process/setups/{setupId}/operations/reorder',
    path,
  );
  if (params && method === 'PATCH') {
    return operationsReorder_(params.partId, params.setupId, body);
  }
  params = matchPath_('/api/v1/parts/{partId}/tech-process/operations/{operationId}', path);
  if (params) {
    if (method === 'PATCH') {
      return operationUpdate_(params.partId, params.operationId, body);
    }
    if (method === 'DELETE') {
      return operationDelete_(params.partId, params.operationId);
    }
  }

  params = matchPath_('/api/v1/parts/{partId}/required-items', path);
  if (params && method === 'GET') {
    return assemblyRequiredItems_(params.partId);
  }

  throw new HttpError_(404, 'Not found: ' + method + ' ' + path);
}
