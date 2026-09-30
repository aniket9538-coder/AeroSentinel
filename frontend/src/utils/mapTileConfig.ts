/**
 * Shared Map Tile Provider Configuration for AeroSentinel
 *
 * Implements MapTiler Cloud basemap integration using Leaflet.
 * Safely reads VITE_MAPTILER_API_KEY from environment without exposing or logging it.
 * Provides graceful fallback to CartoDB / OpenStreetMap when key is missing.
 */

export interface MapTileConfig {
  url: string;
  attribution: string;
  maxZoom?: number;
  isMapTiler: boolean;
}

export const MAPTILER_ATTRIBUTION =
  '&copy; <a href="https://www.maptiler.com/">MapTiler</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>';

export const FALLBACK_ATTRIBUTION =
  '&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>';

let _warnedMissingKey = false;

/**
 * Reset warning state (useful for unit testing)
 */
export function _resetWarningState(): void {
  _warnedMissingKey = false;
}

/**
 * Returns the configured tile URL and attribution.
 * Uses MapTiler streets-v4 when VITE_MAPTILER_API_KEY is available.
 * Falls back safely to CartoDB/OpenStreetMap basemap if key is absent.
 */
export function getMapTileConfig(theme?: string): MapTileConfig {
  const apiKey =
    typeof import.meta !== 'undefined' &&
    typeof import.meta.env !== 'undefined'
      ? import.meta.env.VITE_MAPTILER_API_KEY
      : undefined;

  const validKey = typeof apiKey === 'string' ? apiKey.trim() : '';

  if (validKey.length > 0) {
    return {
      url: `https://api.maptiler.com/maps/streets-v4/{z}/{x}/{y}.png?key=${validKey}`,
      attribution: MAPTILER_ATTRIBUTION,
      maxZoom: 19,
      isMapTiler: true,
    };
  }

  // Developer-facing warning without logging key value
  if (!_warnedMissingKey) {
    console.warn(
      '[AeroSentinel Map] VITE_MAPTILER_API_KEY is not configured. Falling back to default basemap provider.'
    );
    _warnedMissingKey = true;
  }

  const customTileUrl =
    typeof import.meta !== 'undefined' &&
    typeof import.meta.env !== 'undefined'
      ? (import.meta.env as any).VITE_MAP_TILE_URL
      : undefined;

  return {
    url:
      customTileUrl ||
      (theme === 'dark'
        ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
        : 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png'),
    attribution: FALLBACK_ATTRIBUTION,
    maxZoom: 19,
    isMapTiler: false,
  };
}
