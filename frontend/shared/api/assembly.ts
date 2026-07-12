import { apiRequest } from "./client";

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
  return apiRequest<RequiredItems>(`/api/v1/parts/${partId}/required-items`);
}
