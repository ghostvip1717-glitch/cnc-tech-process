export type CatalogItemType = "tool" | "plate" | "jaw";

export interface CatalogItem {
  id: number;
  type: CatalogItemType;
  name: string;
  note: string | null;
}

export interface CatalogItemCreate {
  type: CatalogItemType;
  name: string;
  note?: string | null;
}

export interface CatalogItemUpdate {
  name?: string;
  note?: string | null;
}

const API_BASE = "/api/v1/catalog";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const message =
      typeof errorBody?.detail === "string"
        ? errorBody.detail
        : `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export async function listCatalogItems(params?: {
  type?: CatalogItemType;
  q?: string;
}): Promise<CatalogItem[]> {
  const searchParams = new URLSearchParams();
  if (params?.type) {
    searchParams.set("type", params.type);
  }
  if (params?.q) {
    searchParams.set("q", params.q);
  }

  const query = searchParams.toString();
  const url = query ? `${API_BASE}?${query}` : API_BASE;
  return request<CatalogItem[]>(url);
}

export async function createCatalogItem(
  payload: CatalogItemCreate,
): Promise<CatalogItem> {
  return request<CatalogItem>(API_BASE, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateCatalogItem(
  id: number,
  payload: CatalogItemUpdate,
): Promise<CatalogItem> {
  return request<CatalogItem>(`${API_BASE}/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deleteCatalogItem(id: number): Promise<void> {
  await request<void>(`${API_BASE}/${id}`, { method: "DELETE" });
}
