import test from 'node:test';
import assert from 'node:assert/strict';
import {
  getMapTileConfig,
  MAPTILER_ATTRIBUTION,
  FALLBACK_ATTRIBUTION,
  _resetWarningState,
} from './mapTileConfig.js';

test('MapTileConfig: returns valid MapTiler config when API key is set', () => {
  // If VITE_MAPTILER_API_KEY is present in the current environment
  const currentKey =
    typeof import.meta !== 'undefined' &&
    typeof import.meta.env !== 'undefined'
      ? import.meta.env.VITE_MAPTILER_API_KEY
      : undefined;

  const config = getMapTileConfig('light');

  if (currentKey && currentKey.trim().length > 0) {
    assert.strictEqual(config.isMapTiler, true);
    assert.ok(
      config.url.startsWith('https://api.maptiler.com/maps/streets-v4/{z}/{x}/{y}.png?key='),
      'URL should use MapTiler streets-v4 endpoint'
    );
    assert.strictEqual(config.attribution, MAPTILER_ATTRIBUTION);
    assert.strictEqual(config.maxZoom, 19);
    assert.ok(
      config.attribution.includes('MapTiler') && config.attribution.includes('OpenStreetMap'),
      'Attribution must credit MapTiler and OpenStreetMap'
    );
  } else {
    // If running in environment without key
    assert.strictEqual(config.isMapTiler, false);
    assert.strictEqual(config.attribution, FALLBACK_ATTRIBUTION);
  }
});

test('MapTileConfig: attribution constants are compliant', () => {
  assert.ok(MAPTILER_ATTRIBUTION.includes('https://www.maptiler.com/'));
  assert.ok(MAPTILER_ATTRIBUTION.includes('https://www.openstreetmap.org/copyright'));
  assert.ok(FALLBACK_ATTRIBUTION.includes('https://carto.com/'));
  assert.ok(FALLBACK_ATTRIBUTION.includes('https://www.openstreetmap.org/copyright'));
});

test('MapTileConfig: graceful fallback when key is empty', () => {
  _resetWarningState();

  const originalKey =
    typeof import.meta !== 'undefined' &&
    typeof import.meta.env !== 'undefined'
      ? import.meta.env.VITE_MAPTILER_API_KEY
      : undefined;

  let warningCount = 0;
  let loggedMessage = '';
  const originalWarn = console.warn;
  console.warn = (msg: string) => {
    warningCount++;
    loggedMessage = msg;
  };

  try {
    // Temporarily unset key in import.meta.env
    if (typeof import.meta !== 'undefined' && import.meta.env) {
      (import.meta.env as any).VITE_MAPTILER_API_KEY = '';
    }

    const lightFallback = getMapTileConfig('light');
    assert.strictEqual(lightFallback.isMapTiler, false);
    assert.ok(lightFallback.url.includes('voyager'));
    assert.strictEqual(lightFallback.attribution, FALLBACK_ATTRIBUTION);

    // Call a second time - warning should only log once
    const darkFallback = getMapTileConfig('dark');
    assert.strictEqual(darkFallback.isMapTiler, false);
    assert.ok(darkFallback.url.includes('dark_all'));
    assert.strictEqual(warningCount, 1, 'Warning should only be logged once');
    assert.ok(
      loggedMessage.includes('VITE_MAPTILER_API_KEY is not configured'),
      'Warning message should be informative'
    );
    // Crucial security check: Ensure no key or token leaked in warning
    assert.ok(
      !loggedMessage.includes('key='),
      'Warning message must never contain key parameters'
    );
  } finally {
    console.warn = originalWarn;
    if (typeof import.meta !== 'undefined' && import.meta.env) {
      (import.meta.env as any).VITE_MAPTILER_API_KEY = originalKey;
    }
    _resetWarningState();
  }
});
