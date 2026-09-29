import * as h3 from 'h3-js';

/**
 * Safely computes the polygon boundary for an H3 cell without throwing.
 * Returns null if the H3 index is null, empty, malformed, or invalid.
 */
export const getH3BoundarySafe = (h3Index?: string | null): [number, number][] | null => {
  if (!h3Index || typeof h3Index !== 'string' || h3Index.trim() === '') {
    return null;
  }
  const clean = h3Index.trim();
  try {
    if (!h3.isValidCell(clean)) {
      return null;
    }
    const boundary = h3.cellToBoundary(clean) as [number, number][];
    if (Array.isArray(boundary) && boundary.length >= 3) {
      return boundary;
    }
    return null;
  } catch {
    return null;
  }
};

/**
 * Safely computes the centroid [lat, lng] for an H3 cell.
 * Returns null if invalid or cannot be computed.
 */
export const getH3CenterSafe = (h3Index?: string | null): [number, number] | null => {
  if (!h3Index || typeof h3Index !== 'string' || h3Index.trim() === '') {
    return null;
  }
  const clean = h3Index.trim();
  try {
    if (!h3.isValidCell(clean)) {
      return null;
    }
    const center = h3.cellToLatLng(clean) as [number, number];
    if (Array.isArray(center) && center.length === 2 && !isNaN(center[0]) && !isNaN(center[1])) {
      return center;
    }
    return null;
  } catch {
    return null;
  }
};

/**
 * Computes an informational, client-side visual H3 cell.
 * Note: Server-side derivation remains the authoritative spatial identity.
 */
export const calculateVisualH3 = (lat: number, lng: number, res = 8): string | null => {
  if (typeof lat !== 'number' || typeof lng !== 'number' || isNaN(lat) || isNaN(lng)) {
    return null;
  }
  try {
    return h3.latLngToCell(lat, lng, res);
  } catch {
    return null;
  }
};
