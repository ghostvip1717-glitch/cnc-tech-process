import { apiRequest } from "./client";

export interface PartPhoto {
  id: number;
  part_id: number;
  file_path: string;
  url: string;
  sort_order: number;
}

export interface Part {
  id: number;
  number: string;
  title: string;
  created_at: string;
  photos: PartPhoto[];
}

export interface PartCreate {
  number: string;
  title: string;
}

export interface PartUpdate {
  number?: string;
  title?: string;
}

const API_BASE = "/api/v1/parts";

export async function listParts(q?: string): Promise<Part[]> {
  const searchParams = new URLSearchParams();
  if (q) {
    searchParams.set("q", q);
  }
  const query = searchParams.toString();
  const url = query ? `${API_BASE}?${query}` : API_BASE;
  return apiRequest<Part[]>(url);
}

export async function getPart(id: number): Promise<Part> {
  return apiRequest<Part>(`${API_BASE}/${id}`);
}

export async function createPart(payload: PartCreate): Promise<Part> {
  return apiRequest<Part>(API_BASE, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updatePart(id: number, payload: PartUpdate): Promise<Part> {
  return apiRequest<Part>(`${API_BASE}/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deletePart(id: number): Promise<void> {
  await apiRequest<void>(`${API_BASE}/${id}`, { method: "DELETE" });
}

export async function uploadPartPhoto(partId: number, file: File): Promise<PartPhoto> {
  const formData = new FormData();
  formData.append("file", file);

  return apiRequest<PartPhoto>(`${API_BASE}/${partId}/photos`, {
    method: "POST",
    body: formData,
  });
}

export async function deletePartPhoto(partId: number, photoId: number): Promise<void> {
  await apiRequest<void>(`${API_BASE}/${partId}/photos/${photoId}`, { method: "DELETE" });
}

export async function reorderPartPhotos(partId: number, photoIds: number[]): Promise<PartPhoto[]> {
  return apiRequest<PartPhoto[]>(`${API_BASE}/${partId}/photos/reorder`, {
    method: "PATCH",
    body: JSON.stringify({ photo_ids: photoIds }),
  });
}
