import { apiRequest } from "./client";

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
  return apiRequest<CatalogItem[]>(url);
}

export async function createCatalogItem(
  payload: CatalogItemCreate,
): Promise<CatalogItem> {
  return apiRequest<CatalogItem>(API_BASE, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateCatalogItem(
  id: number,
  payload: CatalogItemUpdate,
): Promise<CatalogItem> {
  return apiRequest<CatalogItem>(`${API_BASE}/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deleteCatalogItem(id: number): Promise<void> {
  await apiRequest<void>(`${API_BASE}/${id}`, { method: "DELETE" });
}
