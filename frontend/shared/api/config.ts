const DEFAULT_API_URL =
  "https://script.google.com/macros/s/AKfycbxD1AjO9kD26CNbEm_SyJoMjm1UkNYdh3kKleOFbc4WGnkQbLbB8oS_LLQ5AMOg1CzeUA/exec";

const rawApiUrl = (import.meta.env.VITE_API_URL || DEFAULT_API_URL).trim();

export const apiOrigin = rawApiUrl.replace(/\/$/, "");

export function resolveApiUrl(path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  if (!apiOrigin) {
    return path;
  }
  return `${apiOrigin}${path}`;
}
