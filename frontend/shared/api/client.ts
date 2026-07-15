import { getTelegramInitData } from "../../telegram/init";
import { cacheGet, cacheInvalidateAll, cacheKey, cacheSet } from "./cache";
import { apiOrigin } from "./config";

interface ApiEnvelopeSuccess<T> {
  ok: true;
  httpStatus: number;
  data: T;
}

interface ApiEnvelopeError {
  ok: false;
  httpStatus: number;
  detail: string | unknown;
}

type ApiEnvelope<T> = ApiEnvelopeSuccess<T> | ApiEnvelopeError;

function splitUrl(url: string): { path: string; query: Record<string, string> } {
  const [pathPart, queryString] = url.split("?");
  const query: Record<string, string> = {};
  if (queryString) {
    new URLSearchParams(queryString).forEach((value, key) => {
      query[key] = value;
    });
  }
  return { path: pathPart, query };
}

async function fileToBase64(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(i, i + chunkSize);
    binary += String.fromCharCode(...chunk);
  }
  return btoa(binary);
}

async function serializeBody(body: BodyInit | null | undefined): Promise<unknown> {
  if (body === undefined || body === null) {
    return null;
  }
  if (body instanceof FormData) {
    const file = body.get("file");
    if (!(file instanceof File)) {
      throw new Error("Unsupported FormData payload");
    }
    return {
      fileName: file.name,
      mimeType: file.type,
      contentBase64: await fileToBase64(file),
    };
  }
  if (typeof body === "string") {
    if (!body) {
      return null;
    }
    return JSON.parse(body) as unknown;
  }
  throw new Error("Unsupported request body type");
}

async function postEnvelope<T>(envelope: {
  path: string;
  method: string;
  query: Record<string, string>;
  body: unknown;
  initData: string | null;
}): Promise<ApiEnvelope<T>> {
  const response = await fetch(apiOrigin, {
    method: "POST",
    headers: {
      "Content-Type": "text/plain;charset=utf-8",
    },
    body: JSON.stringify(envelope),
    redirect: "follow",
  });

  try {
    return (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new Error(`Request failed with status ${response.status}`);
  }
}

/**
 * Единый транспорт к Apps Script Web App (/exec).
 * GET читает из кэша (60с); мутации сбрасывают кэш.
 */
export async function apiRequest<T>(url: string, init?: RequestInit): Promise<T> {
  if (!apiOrigin) {
    throw new Error("VITE_API_URL is not configured");
  }

  const method = (init?.method ?? "GET").toUpperCase();
  const { path, query } = splitUrl(url);
  const body = await serializeBody(init?.body ?? null);
  const initData = getTelegramInitData() || null;
  const key = cacheKey(path, query);

  if (method === "GET") {
    const cached = cacheGet<T>(key);
    if (cached !== undefined) {
      return cached;
    }
  }

  const envelope = {
    path,
    method,
    query,
    body,
    initData,
  };

  const payload = await postEnvelope<T>(envelope);

  if (!payload.ok) {
    const message =
      typeof payload.detail === "string"
        ? payload.detail
        : `Request failed with status ${payload.httpStatus}`;
    throw new Error(message);
  }

  if (payload.httpStatus === 204) {
    cacheInvalidateAll();
    return undefined as T;
  }

  if (method === "GET") {
    cacheSet(key, payload.data);
  } else {
    cacheInvalidateAll();
  }

  return payload.data;
}

/** Прогрев Apps Script + кэш списков при старте (фоном). */
export function warmUpApi(): void {
  if (!apiOrigin) {
    return;
  }
  void (async () => {
    try {
      await postEnvelope({
        path: "/health",
        method: "GET",
        query: {},
        body: null,
        initData: null,
      });
      await Promise.all([
        apiRequest("/api/v1/parts"),
        apiRequest("/api/v1/catalog"),
      ]);
    } catch {
      // ignore warmup errors
    }
  })();
}
