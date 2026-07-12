export interface Setup {
  id: number;
  tech_process_id: number;
  order: number;
  order_label: string;
  jaw_id: number;
}

export interface TechProcess {
  id: number;
  part_id: number;
  setups: Setup[];
}

const apiBase = (partId: number) => `/api/v1/parts/${partId}/tech-process`;

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

export async function getTechProcess(partId: number): Promise<TechProcess> {
  return requestJson<TechProcess>(apiBase(partId));
}

export async function createTechProcess(partId: number): Promise<TechProcess> {
  return requestJson<TechProcess>(apiBase(partId), { method: "PUT" });
}

export async function createSetup(partId: number, jawId: number): Promise<Setup> {
  return requestJson<Setup>(`${apiBase(partId)}/setups`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jaw_id: jawId }),
  });
}

export async function updateSetup(
  partId: number,
  setupId: number,
  jawId: number,
): Promise<Setup> {
  return requestJson<Setup>(`${apiBase(partId)}/setups/${setupId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jaw_id: jawId }),
  });
}

export async function deleteSetup(partId: number, setupId: number): Promise<void> {
  await requestJson<void>(`${apiBase(partId)}/setups/${setupId}`, { method: "DELETE" });
}
