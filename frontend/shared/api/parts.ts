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

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);

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

export async function listParts(q?: string): Promise<Part[]> {
  const searchParams = new URLSearchParams();
  if (q) {
    searchParams.set("q", q);
  }
  const query = searchParams.toString();
  const url = query ? `${API_BASE}?${query}` : API_BASE;
  return requestJson<Part[]>(url);
}

export async function getPart(id: number): Promise<Part> {
  return requestJson<Part>(`${API_BASE}/${id}`);
}

export async function createPart(payload: PartCreate): Promise<Part> {
  return requestJson<Part>(API_BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function updatePart(id: number, payload: PartUpdate): Promise<Part> {
  return requestJson<Part>(`${API_BASE}/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function deletePart(id: number): Promise<void> {
  await requestJson<void>(`${API_BASE}/${id}`, { method: "DELETE" });
}

export async function uploadPartPhoto(partId: number, file: File): Promise<PartPhoto> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE}/${partId}/photos`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const message =
      typeof errorBody?.detail === "string"
        ? errorBody.detail
        : `Upload failed with status ${response.status}`;
    throw new Error(message);
  }

  return response.json() as Promise<PartPhoto>;
}

export async function deletePartPhoto(partId: number, photoId: number): Promise<void> {
  await requestJson<void>(`${API_BASE}/${partId}/photos/${photoId}`, { method: "DELETE" });
}

export async function reorderPartPhotos(partId: number, photoIds: number[]): Promise<PartPhoto[]> {
  return requestJson<PartPhoto[]>(`${API_BASE}/${partId}/photos/reorder`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ photo_ids: photoIds }),
  });
}
