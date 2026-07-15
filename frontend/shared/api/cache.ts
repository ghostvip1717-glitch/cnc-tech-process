const memoryCache = new Map<string, { expiresAt: number; data: unknown }>();

const DEFAULT_TTL_MS = 60_000;

export function cacheKey(path: string, query: Record<string, string>): string {
  const q = Object.keys(query)
    .sort()
    .map((key) => `${key}=${query[key]}`)
    .join("&");
  return q ? `${path}?${q}` : path;
}

export function cacheGet<T>(key: string): T | undefined {
  const entry = memoryCache.get(key);
  if (!entry) {
    return undefined;
  }
  if (Date.now() > entry.expiresAt) {
    memoryCache.delete(key);
    return undefined;
  }
  return entry.data as T;
}

export function cacheSet(key: string, data: unknown, ttlMs = DEFAULT_TTL_MS): void {
  memoryCache.set(key, { data, expiresAt: Date.now() + ttlMs });
}

/** После создания/изменения/удаления — списки и карточки устарели. */
export function cacheInvalidateAll(): void {
  memoryCache.clear();
}
