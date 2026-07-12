export interface RequiredItem {
  id: number;
  type: "tool" | "plate" | "jaw";
  name: string;
}

export interface RequiredItems {
  tools: RequiredItem[];
  plates: RequiredItem[];
  jaws: RequiredItem[];
}

export async function getRequiredItems(partId: number): Promise<RequiredItems> {
  const response = await fetch(`/api/v1/parts/${partId}/required-items`);
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const message =
      typeof errorBody?.detail === "string"
        ? errorBody.detail
        : `Request failed with status ${response.status}`;
    throw new Error(message);
  }
  return response.json() as Promise<RequiredItems>;
}
