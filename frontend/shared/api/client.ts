import { getTelegramInitData } from "../../telegram/init";
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

/**
 * Единый транспорт к Google Apps Script Web App.
 * Все вызовы → POST на VITE_API_URL с envelope { path, method, query, body, telegramInitData }.
 * Content-Type text/plain — без CORS preflight.
 */
export async function apiRequest<T>(url: string, init?: RequestInit): Promise<T> {
  if (!apiOrigin) {
    throw new Error("VITE_API_URL is not configured");
  }

  const method = (init?.method ?? "GET").toUpperCase();
  const { path, query } = splitUrl(url);
  const body = await serializeBody(init?.body ?? null);
  const telegramInitData = getTelegramInitData() || null;

  const envelope = {
    path,
    method,
    query,
    body,
    telegramInitData,
    headers: telegramInitData
      ? { "X-Telegram-Init-Data": telegramInitData }
      : {},
  };

  const response = await fetch(apiOrigin, {
    method: "POST",
    headers: {
      "Content-Type": "text/plain;charset=utf-8",
    },
    body: JSON.stringify(envelope),
    redirect: "follow",
  });

  let payload: ApiEnvelope<T>;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new Error(`Request failed with status ${response.status}`);
  }

  if (!payload.ok) {
    const message =
      typeof payload.detail === "string"
        ? payload.detail
        : `Request failed with status ${payload.httpStatus ?? response.status}`;
    throw new Error(message);
  }

  if (payload.httpStatus === 204) {
    return undefined as T;
  }

  return payload.data;
}
