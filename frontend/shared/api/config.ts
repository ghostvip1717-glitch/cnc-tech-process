const rawApiUrl = import.meta.env.VITE_API_URL ?? "";

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
