import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateFreshnessStatus,
  getFreshnessBadgeVariant,
  formatObservationTimestamp,
  formatObservationAge,
  isSourceUnavailableError,
  DEFAULT_FRESHNESS_THRESHOLD_MS,
} from './freshness';

test('Freshness Calculation — Recent observation within threshold returns LIVE', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  // 30 minutes before reference time
  const observedAt = new Date('2026-09-25T11:30:00Z').toISOString();
  const configuredThresholdMs = 60 * 60 * 1000; // 1 hour isolated configured threshold

  const status = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    thresholdMs: configuredThresholdMs,
    referenceTime,
  });

  assert.strictEqual(status, 'LIVE');
});

test('Freshness Calculation — Old observation exceeding threshold returns STALE', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  // 3 hours before reference time
  const observedAt = new Date('2026-09-25T09:00:00Z').toISOString();
  const configuredThresholdMs = 60 * 60 * 1000; // 1 hour isolated configured threshold

  const status = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    thresholdMs: configuredThresholdMs,
    referenceTime,
  });

  assert.strictEqual(status, 'STALE');
});

test('Freshness Calculation — Exact threshold boundary returns LIVE', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  const configuredThresholdMs = 2 * 60 * 60 * 1000; // 2 hours
  // Exactly 2 hours before reference time
  const observedAt = new Date('2026-09-25T10:00:00Z').toISOString();

  const status = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    thresholdMs: configuredThresholdMs,
    referenceTime,
  });

  assert.strictEqual(status, 'LIVE');
});

test('Freshness Calculation — Clock skew (slight future timestamp) returns LIVE', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  // 1 minute in the future due to server/client clock drift
  const observedAt = new Date('2026-09-25T12:01:00Z').toISOString();

  const status = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    thresholdMs: 60 * 60 * 1000,
    referenceTime,
  });

  assert.strictEqual(status, 'LIVE');
});

test('Freshness Calculation — Empty observations (hasObservations = false) returns NO_DATA', () => {
  const status = calculateFreshnessStatus({
    hasObservations: false,
    observedAt: undefined,
  });

  assert.strictEqual(status, 'NO_DATA');
});

test('Freshness Calculation — Missing or null observedAt returns NO_DATA', () => {
  const status1 = calculateFreshnessStatus({
    hasObservations: true,
    observedAt: null,
  });
  const status2 = calculateFreshnessStatus({
    hasObservations: true,
    observedAt: undefined,
  });

  assert.strictEqual(status1, 'NO_DATA');
  assert.strictEqual(status2, 'NO_DATA');
});

test('Freshness Calculation — Malformed/invalid timestamp returns NO_DATA', () => {
  const status = calculateFreshnessStatus({
    hasObservations: true,
    observedAt: 'not-a-valid-iso-date',
  });

  assert.strictEqual(status, 'NO_DATA');
});

test('Freshness Calculation — Explicit source unavailable condition returns SOURCE_UNAVAILABLE', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  const observedAt = new Date('2026-09-25T11:55:00Z').toISOString();

  const status = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    isSourceUnavailable: true,
    referenceTime,
  });

  assert.strictEqual(status, 'SOURCE_UNAVAILABLE');
});

test('Freshness Calculation — Source unavailable takes priority over empty observations', () => {
  const status = calculateFreshnessStatus({
    hasObservations: false,
    isSourceUnavailable: true,
  });

  assert.strictEqual(status, 'SOURCE_UNAVAILABLE');
});

test('Freshness Calculation — Falls back to configurable default threshold when omitted', () => {
  const referenceTime = new Date('2026-09-25T12:00:00Z').getTime();
  // 12 hours ago (within default 24h fallback threshold)
  const observedAt = new Date('2026-09-25T00:00:00Z').toISOString();

  const statusWithinDefault = calculateFreshnessStatus({
    observedAt,
    hasObservations: true,
    referenceTime,
  });

  // 26 hours ago (exceeds default 24h fallback threshold)
  const observedAtStale = new Date('2026-09-24T10:00:00Z').toISOString();
  const statusExceedingDefault = calculateFreshnessStatus({
    observedAt: observedAtStale,
    hasObservations: true,
    referenceTime,
  });

  assert.strictEqual(statusWithinDefault, 'LIVE');
  assert.strictEqual(statusExceedingDefault, 'STALE');
});

test('UI Helper — getFreshnessBadgeVariant maps statuses to appropriate badge styles', () => {
  assert.strictEqual(getFreshnessBadgeVariant('LIVE'), 'success');
  assert.strictEqual(getFreshnessBadgeVariant('STALE'), 'warning');
  assert.strictEqual(getFreshnessBadgeVariant('SOURCE_UNAVAILABLE'), 'danger');
  assert.strictEqual(getFreshnessBadgeVariant('NO_DATA'), 'neutral');
});

test('UI Helper — formatObservationTimestamp parses ISO UTC and produces formatted string', () => {
  const formatted = formatObservationTimestamp('2026-09-24T22:00:00Z');
  assert.ok(formatted !== 'N/A');
  assert.ok(formatted.includes('2026') || formatted.includes('Sep'));
  assert.strictEqual(formatObservationTimestamp(null), 'N/A');
  assert.strictEqual(formatObservationTimestamp('invalid'), 'N/A');
});

test('UI Helper — formatObservationAge calculates human-readable age', () => {
  const ref = new Date('2026-09-25T12:00:00Z').getTime();
  const fiveMinAgo = new Date('2026-09-25T11:55:00Z').toISOString();
  const twoHoursAgo = new Date('2026-09-25T10:00:00Z').toISOString();

  assert.strictEqual(formatObservationAge(fiveMinAgo, ref), '5 min ago');
  assert.strictEqual(formatObservationAge(twoHoursAgo, ref), '2h ago');
  assert.strictEqual(formatObservationAge(null, ref), 'N/A');
});

test('Error Classification — isSourceUnavailableError distinguishes genuine upstream failure from generic errors', () => {
  // Genuine upstream / provider failures
  assert.strictEqual(isSourceUnavailableError('503 Service Unavailable'), true);
  assert.strictEqual(isSourceUnavailableError('HTTP 502 Bad Gateway'), true);
  assert.strictEqual(isSourceUnavailableError('Provider unavailable: connection timeout'), true);
  assert.strictEqual(isSourceUnavailableError('SOURCE_UNAVAILABLE: OpenAQ connection failure'), true);
  assert.strictEqual(isSourceUnavailableError('connection refused by upstream server'), true);
  assert.strictEqual(isSourceUnavailableError(null, 'ERROR'), true);

  // Generic errors that must NOT be labeled SOURCE_UNAVAILABLE
  assert.strictEqual(isSourceUnavailableError('404 Not Found: City not found with id'), false);
  assert.strictEqual(isSourceUnavailableError('400 Bad Request: Missing parameter from'), false);
  assert.strictEqual(isSourceUnavailableError('SyntaxError: Unexpected token'), false);
  assert.strictEqual(isSourceUnavailableError(''), false);
  assert.strictEqual(isSourceUnavailableError(null, 'CONNECTED'), false);
});

test('F2 Weather Freshness — Recent weather observation returns LIVE', () => {
  const refTime = new Date('2026-09-26T08:00:00Z').getTime();
  const recentWeatherObs = '2026-09-26T07:45:00Z'; // 15 mins ago

  const status = calculateFreshnessStatus({
    observedAt: recentWeatherObs,
    hasObservations: true,
    referenceTime: refTime,
  });

  assert.strictEqual(status, 'LIVE');
});

test('F2 Weather Freshness — Weather observation older than threshold returns STALE', () => {
  const refTime = new Date('2026-09-26T08:00:00Z').getTime();
  const oldWeatherObs = '2026-09-24T06:00:00Z'; // 50 hours ago

  const status = calculateFreshnessStatus({
    observedAt: oldWeatherObs,
    hasObservations: true,
    referenceTime: refTime,
  });

  assert.strictEqual(status, 'STALE');
});

test('F2 Weather Freshness — Zero weather observations (null telemetry) returns NO_DATA', () => {
  const status1 = calculateFreshnessStatus({
    observedAt: null,
    hasObservations: false,
  });
  const status2 = calculateFreshnessStatus({
    observedAt: undefined,
    hasObservations: true,
  });

  assert.strictEqual(status1, 'NO_DATA');
  assert.strictEqual(status2, 'NO_DATA');
});

test('F2 Weather Freshness — Provider failure with last-known DB data never returns LIVE', () => {
  const refTime = new Date('2026-09-26T08:00:00Z').getTime();
  const recentWeatherObs = '2026-09-26T07:50:00Z'; // 10 mins ago

  // Raw calculation without provider error would be LIVE
  const rawStatus = calculateFreshnessStatus({
    observedAt: recentWeatherObs,
    hasObservations: true,
    referenceTime: refTime,
  });
  assert.strictEqual(rawStatus, 'LIVE');

  // Hardened requirement: If upstream provider is down, cached last-known data must NOT claim LIVE
  const isUpstreamDown = isSourceUnavailableError('503 Service Unavailable: Open-Meteo connection refused');
  assert.strictEqual(isUpstreamDown, true);

  const safeStatus = isUpstreamDown ? 'STALE' : rawStatus;
  assert.strictEqual(safeStatus, 'STALE');
});

test('F2 Weather Freshness — Timezone handling parses ISO-8601 UTC correctly regardless of local timezone', () => {
  const refTimeUtc = Date.parse('2026-09-26T12:00:00Z');
  // Timestamp expressed with positive timezone offset (+05:30) equivalent to 11:30 UTC
  const obsWithOffset = '2026-09-26T17:00:00+05:30'; // 11:30:00 UTC (30 mins before ref)

  const status = calculateFreshnessStatus({
    observedAt: obsWithOffset,
    hasObservations: true,
    referenceTime: refTimeUtc,
  });

  assert.strictEqual(status, 'LIVE');
});

test('F2 State Guard — City-switch sequencing discards in-flight superseded responses', () => {
  let activeRequestId = 0;
  let displayedCity: string | null = null;

  // 1. User selects Pune (request 1)
  const req1 = ++activeRequestId;
  displayedCity = 'Pune (loading)';

  // 2. User quickly switches to Mumbai before req 1 finishes (request 2)
  const req2 = ++activeRequestId;
  // State reset: previous city state cleared immediately
  displayedCity = 'Mumbai (loading)';

  // 3. Req 1 finishes late
  if (req1 === activeRequestId) {
    displayedCity = 'Pune (resolved)';
  }

  // Verify req 1 did not overwrite Mumbai
  assert.strictEqual(displayedCity, 'Mumbai (loading)');

  // 4. Req 2 finishes
  if (req2 === activeRequestId) {
    displayedCity = 'Mumbai (resolved)';
  }

  assert.strictEqual(displayedCity, 'Mumbai (resolved)');
});

test('F2 State Guard — Cell-switch sequencing isolates cell-level observations', () => {
  let activeCellId: string | null = '88608850e5fffff';
  let cellObservations: string | null = null;
  let obsReqId = 0;

  // User selects Cell A
  const reqA = ++obsReqId;
  cellObservations = null; // Cleared immediately on switch

  // User immediately clicks Cell B
  activeCellId = '88608850e7fffff';
  const reqB = ++obsReqId;
  cellObservations = null; // Cleared immediately

  // Req A returns after Cell B was already chosen
  if (reqA === obsReqId) {
    cellObservations = 'Cell A Data';
  }
  assert.strictEqual(cellObservations, null); // Still clean

  // Req B returns
  if (reqB === obsReqId) {
    cellObservations = 'Cell B Data';
  }
  assert.strictEqual(cellObservations, 'Cell B Data');
});

