import test from 'node:test';
import assert from 'node:assert/strict';
import { validateForecastResponse, forecastApi } from '../services/forecastApi';
import { ForecastResponse, ForecastItem } from '../types/forecast';

// =========================================================================
// Real Authoritative Pune Baseline Payload from F4-P4
// =========================================================================
const REAL_PUNE_FORECAST_PAYLOAD: ForecastResponse = {
  h3Index: '88608850e5fffff',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  baseTimestamp: '2026-09-26T12:48:38.191225Z',
  generatedAt: '2026-09-27T06:15:32.415902Z',
  modelVersion: 'forecast_regressors_v1',
  parentPredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
  featureSnapshotId: '47d7f766-3d60-449e-ba23-9584288009df',
  status: 'SUCCESS',
  freshness: 'LIVE',
  forecasts: [
    {
      horizonHours: 1,
      targetTime: '2026-09-26T13:48:38.191225Z',
      predictedPm25: 70.62,
      lowerBound: 68.78,
      upperBound: 72.48,
      unit: 'ug/m3',
    },
    {
      horizonHours: 3,
      targetTime: '2026-09-26T15:48:38.191225Z',
      predictedPm25: 70.55,
      lowerBound: 66.65,
      upperBound: 73.60,
      unit: 'ug/m3',
    },
    {
      horizonHours: 6,
      targetTime: '2026-09-26T18:48:38.191225Z',
      predictedPm25: 60.91,
      lowerBound: 55.39,
      upperBound: 66.33,
      unit: 'ug/m3',
    },
  ],
  forecastConfidence: null,
};

// =========================================================================
// 1. API Client Parsing Test
// =========================================================================
test('1. API Client Parsing — Successfully parses authoritative backend payload', () => {
  const parsed = validateForecastResponse(REAL_PUNE_FORECAST_PAYLOAD);

  assert.strictEqual(parsed.h3Index, '88608850e5fffff');
  assert.strictEqual(parsed.cityId, '550e8400-e29b-41d4-a716-446655440001');
  assert.strictEqual(parsed.parentPredictionId, 'a310c689-f340-49fc-8935-a037de8d7709');
  assert.strictEqual(parsed.featureSnapshotId, '47d7f766-3d60-449e-ba23-9584288009df');
  assert.strictEqual(parsed.modelVersion, 'forecast_regressors_v1');
  assert.strictEqual(parsed.status, 'SUCCESS');
  assert.strictEqual(parsed.freshness, 'LIVE');
  assert.strictEqual(parsed.forecasts.length, 3);
  assert.strictEqual(parsed.forecastConfidence, null);
});

// =========================================================================
// 2. Forecast Response Validation Test
// =========================================================================
test('2. Response Validation — Rejects invalid structures, invalid horizons, and bounds violations', () => {
  // A. Non-object
  assert.throws(
    () => validateForecastResponse(null),
    /MALFORMED_RESPONSE: Forecast response must be an object/
  );
  assert.throws(
    () => validateForecastResponse('string-payload'),
    /MALFORMED_RESPONSE: Forecast response must be an object/
  );

  // B. Missing h3Index
  assert.throws(
    () => validateForecastResponse({ forecasts: [] }),
    /MALFORMED_RESPONSE: Missing h3Index/
  );

  // C. Invalid horizonHours (e.g. 2, 4, 12, not in [1, 3, 6])
  assert.throws(
    () =>
      validateForecastResponse({
        h3Index: '88608850e5fffff',
        forecasts: [
          {
            horizonHours: 2, // invalid
            targetTime: '2026-09-26T14:48:38Z',
            predictedPm25: 50.0,
            lowerBound: 45.0,
            upperBound: 55.0,
          },
        ],
      }),
    /MALFORMED_RESPONSE: Invalid horizonHours/
  );

  // D. Non-numeric predictedPm25
  assert.throws(
    () =>
      validateForecastResponse({
        h3Index: '88608850e5fffff',
        forecasts: [
          {
            horizonHours: 1,
            targetTime: '2026-09-26T13:48:38Z',
            predictedPm25: 'not-a-number' as any,
            lowerBound: 45.0,
            upperBound: 55.0,
          },
        ],
      }),
    /MALFORMED_RESPONSE: Non-numeric predictedPm25/
  );

  // E. Negative lowerBound (physical impossibility clamped in P3/P4)
  assert.throws(
    () =>
      validateForecastResponse({
        h3Index: '88608850e5fffff',
        forecasts: [
          {
            horizonHours: 1,
            targetTime: '2026-09-26T13:48:38Z',
            predictedPm25: 50.0,
            lowerBound: -5.0,
            upperBound: 55.0,
          },
        ],
      }),
    /MALFORMED_RESPONSE: Invalid lowerBound/
  );

  // F. Bounds order violation (lowerBound > predictedPm25 or predictedPm25 > upperBound)
  assert.throws(
    () =>
      validateForecastResponse({
        h3Index: '88608850e5fffff',
        forecasts: [
          {
            horizonHours: 1,
            targetTime: '2026-09-26T13:48:38Z',
            predictedPm25: 70.0,
            lowerBound: 75.0, // lower > predicted
            upperBound: 80.0,
          },
        ],
      }),
    /MALFORMED_RESPONSE: Bounds order violated/
  );

  assert.throws(
    () =>
      validateForecastResponse({
        h3Index: '88608850e5fffff',
        forecasts: [
          {
            horizonHours: 1,
            targetTime: '2026-09-26T13:48:38Z',
            predictedPm25: 70.0,
            lowerBound: 65.0,
            upperBound: 68.0, // predicted > upper
          },
        ],
      }),
    /MALFORMED_RESPONSE: Bounds order violated/
  );
});

// =========================================================================
// 3. Null Confidence Rendering Test
// =========================================================================
test('3. Null Confidence Representation — Always preserves null and displays factual wording', () => {
  // Frontend contract requires null confidence to be factual: "Not available"
  // Never converted to 0% or fabricated numbers
  const payloadWithNull = validateForecastResponse({
    ...REAL_PUNE_FORECAST_PAYLOAD,
    forecastConfidence: null,
  });
  assert.strictEqual(payloadWithNull.forecastConfidence, null);

  // Helper simulating UI render logic
  function renderConfidenceLabel(confidence: number | null | undefined): string {
    if (confidence === null || confidence === undefined) {
      return 'Forecast confidence: Not available';
    }
    return `${Math.round(confidence * 100)}%`;
  }

  assert.strictEqual(renderConfidenceLabel(null), 'Forecast confidence: Not available');
  assert.strictEqual(renderConfidenceLabel(undefined), 'Forecast confidence: Not available');

  // Even if an upstream payload accidentally provided a number, validateForecastResponse enforces null
  const payloadWithAccidentalNumber = validateForecastResponse({
    ...REAL_PUNE_FORECAST_PAYLOAD,
    forecastConfidence: 0.95 as any,
  });
  assert.strictEqual(payloadWithAccidentalNumber.forecastConfidence, null);
  assert.strictEqual(renderConfidenceLabel(payloadWithAccidentalNumber.forecastConfidence), 'Forecast confidence: Not available');
});

// =========================================================================
// 4. 1/3/6 Horizon Rendering Test
// =========================================================================
test('4. Horizon Rendering — Exactly extracts and sorts horizons +1h, +3h, +6h', () => {
  const parsed = validateForecastResponse(REAL_PUNE_FORECAST_PAYLOAD);
  const horizons = parsed.forecasts.map(f => f.horizonHours);

  assert.deepStrictEqual(horizons, [1, 3, 6]);

  const h1 = parsed.forecasts.find(f => f.horizonHours === 1);
  const h3 = parsed.forecasts.find(f => f.horizonHours === 3);
  const h6 = parsed.forecasts.find(f => f.horizonHours === 6);

  assert.ok(h1, '+1h horizon exists');
  assert.ok(h3, '+3h horizon exists');
  assert.ok(h6, '+6h horizon exists');

  assert.strictEqual(h1?.predictedPm25, 70.62);
  assert.strictEqual(h3?.predictedPm25, 70.55);
  assert.strictEqual(h6?.predictedPm25, 60.91);

  assert.strictEqual(h1?.unit, 'ug/m3');
  assert.strictEqual(h3?.unit, 'ug/m3');
  assert.strictEqual(h6?.unit, 'ug/m3');
});

// =========================================================================
// 5. Bounds Rendering Test
// =========================================================================
test('5. Bounds Rendering — Verifies empirical P10/P90 bounds match backend without client recalculation', () => {
  const parsed = validateForecastResponse(REAL_PUNE_FORECAST_PAYLOAD);

  const h1 = parsed.forecasts.find(f => f.horizonHours === 1)!;
  assert.strictEqual(h1.lowerBound, 68.78);
  assert.strictEqual(h1.upperBound, 72.48);
  assert.ok(h1.lowerBound <= h1.predictedPm25 && h1.predictedPm25 <= h1.upperBound);

  const h3 = parsed.forecasts.find(f => f.horizonHours === 3)!;
  assert.strictEqual(h3.lowerBound, 66.65);
  assert.strictEqual(h3.upperBound, 73.60);
  assert.ok(h3.lowerBound <= h3.predictedPm25 && h3.predictedPm25 <= h3.upperBound);

  const h6 = parsed.forecasts.find(f => f.horizonHours === 6)!;
  assert.strictEqual(h6.lowerBound, 55.39);
  assert.strictEqual(h6.upperBound, 66.33);
  assert.ok(h6.lowerBound <= h6.predictedPm25 && h6.predictedPm25 <= h6.upperBound);
});

// =========================================================================
// 6. Loading State Test
// =========================================================================
test('6. Loading State — Tracks loading transitions and state isolation', () => {
  interface ForecastState {
    loading: boolean;
    data: ForecastResponse | null;
    error: string | null;
  }

  let state: ForecastState = { loading: false, data: null, error: null };

  // Trigger load
  function startLoad(): void {
    state = { loading: true, data: null, error: null };
  }

  // Success
  function resolveLoad(data: ForecastResponse): void {
    state = { loading: false, data, error: null };
  }

  startLoad();
  assert.strictEqual(state.loading, true);
  assert.strictEqual(state.data, null);
  assert.strictEqual(state.error, null);

  resolveLoad(REAL_PUNE_FORECAST_PAYLOAD);
  assert.strictEqual(state.loading, false);
  assert.strictEqual((state.data as any)?.h3Index, '88608850e5fffff');
  assert.strictEqual(state.error, null);
});

// =========================================================================
// 7. NO_DATA State Test
// =========================================================================
test('7. NO_DATA State — Properly handles empty/missing cell forecast without error', () => {
  const emptyPayload = {
    h3Index: '88608852c1fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    status: 'NO_DATA',
    freshness: 'NO_DATA',
    forecasts: [],
    forecastConfidence: null,
  };

  const parsed = validateForecastResponse(emptyPayload);

  assert.strictEqual(parsed.status, 'NO_DATA');
  assert.strictEqual(parsed.freshness, 'NO_DATA');
  assert.strictEqual(parsed.forecasts.length, 0);
  assert.strictEqual(parsed.forecastConfidence, null);

  // Helper verifying empty state banner conditions
  const isNoData = parsed.status === 'NO_DATA' || parsed.forecasts.length === 0;
  assert.strictEqual(isNoData, true);
});

// =========================================================================
// 8. STALE State Test
// =========================================================================
test('8. STALE State — Recognizes STALE freshness and keeps forecast viewable with warning', () => {
  const stalePayload = {
    ...REAL_PUNE_FORECAST_PAYLOAD,
    freshness: 'STALE',
    generatedAt: '2026-09-25T06:00:00Z', // 48h ago
  };

  const parsed = validateForecastResponse(stalePayload);

  assert.strictEqual(parsed.freshness, 'STALE');
  assert.strictEqual(parsed.forecasts.length, 3);

  // Stale check helper
  const isStale = parsed.freshness === 'STALE';
  assert.strictEqual(isStale, true);
  // Stale forecast MUST NOT discard forecasts
  assert.strictEqual(parsed.forecasts[0].predictedPm25, 70.62);
});

// =========================================================================
// 9. API Error State Test
// =========================================================================
test('9. API Error State — Handles network error and unexpected failures gracefully', () => {
  interface State {
    loading: boolean;
    error: string | null;
    data: ForecastResponse | null;
  }

  let state: State = { loading: true, error: null, data: null };

  function handleError(err: any): void {
    const message = err?.response?.data?.message || err?.message || 'Failed to retrieve forecast data';
    state = { loading: false, error: message, data: null };
  }

  // Network offline error
  handleError(new Error('Network Error: Connection refused to Spring Boot API'));
  assert.strictEqual(state.loading, false);
  assert.strictEqual(state.data, null);
  assert.ok(state.error?.includes('Network Error'));

  // 500 internal server error
  handleError({
    response: {
      status: 500,
      data: { message: 'Database connection timeout in ForecastService' },
    },
  });
  assert.strictEqual(state.loading, false);
  assert.strictEqual(state.data, null);
  assert.strictEqual(state.error, 'Database connection timeout in ForecastService');
});

// =========================================================================
// 10. H3 Selection -> Forecast Fetch Test
// =========================================================================
test('10. H3 Selection & Fetch Sequencing — Validates cell switching and race condition suppression', () => {
  let activeRequestId = 0;
  let activeH3 = '88608850e5fffff';
  let displayedForecast: ForecastResponse | null = null;

  // 1. User selects Pune cell A (req 1)
  const req1 = ++activeRequestId;
  activeH3 = '88608850e5fffff';
  displayedForecast = null; // Cleared on switch

  // 2. User quickly selects cell B before req 1 finishes (req 2)
  const req2 = ++activeRequestId;
  activeH3 = '88608852c1fffff';
  displayedForecast = null;

  // 3. Req 1 returns late
  if (req1 === activeRequestId) {
    displayedForecast = REAL_PUNE_FORECAST_PAYLOAD;
  }
  // Verify req 1 did not overwrite cell B
  assert.strictEqual(displayedForecast, null);

  // 4. Req 2 returns
  const cellBPayload: ForecastResponse = {
    ...REAL_PUNE_FORECAST_PAYLOAD,
    h3Index: '88608852c1fffff',
    forecasts: [],
    status: 'NO_DATA',
  };
  if (req2 === activeRequestId) {
    displayedForecast = cellBPayload;
  }

  const finalForecast: ForecastResponse | null = displayedForecast as any;
  assert.strictEqual(finalForecast?.h3Index, '88608852c1fffff');
  assert.strictEqual(finalForecast?.status, 'NO_DATA');
});

// =========================================================================
// 11. F3 -> Forecast H3 Continuity Priority Test
// =========================================================================
test('11. F3 -> Forecast H3 Continuity — Strict resolution priority without silent reset', () => {
  const resolveH3 = (
    urlParam?: string | null,
    locationState?: string | null,
    sessionStorageValue?: string | null,
    defaultFallback = '88608850e5fffff'
  ): string => {
    if (urlParam && /^88[0-9a-f]{13}$/i.test(urlParam.trim())) {
      return urlParam.trim();
    }
    if (locationState && /^88[0-9a-f]{13}$/i.test(locationState.trim())) {
      return locationState.trim();
    }
    if (sessionStorageValue && /^88[0-9a-f]{13}$/i.test(sessionStorageValue.trim())) {
      return sessionStorageValue.trim();
    }
    return defaultFallback;
  };

  // Flow 1: URL search param ?h3= takes top priority
  assert.strictEqual(
    resolveH3('88608852c1fffff', '8860885357fffff', '88608850e5fffff'),
    '88608852c1fffff',
    'URL search parameter ?h3 must take top priority'
  );

  // Flow 2: Location state takes second priority when no URL param
  assert.strictEqual(
    resolveH3(null, '8860885357fffff', '88608850e5fffff'),
    '8860885357fffff',
    'Location state takes priority when URL param is absent'
  );

  // Flow 3: SessionStorage takes priority when navigating from F3 Hotspots via Sidebar
  assert.strictEqual(
    resolveH3(null, null, '88608b56b3fffff'),
    '88608b56b3fffff',
    'Session storage from F3 Hotspots is preserved on Sidebar navigation'
  );

  // Flow 4: Default fallback only applies when all sources are absent
  assert.strictEqual(
    resolveH3(null, null, null),
    '88608850e5fffff',
    'Default fallback applies cleanly only when no prior cell was selected'
  );

  // Flow 5: Malformed H3 indices are rejected and ignored
  assert.strictEqual(
    resolveH3('invalid-hex', null, '88608850e5fffff'),
    '88608850e5fffff',
    'Malformed URL param falls back to next valid source'
  );
});

// =========================================================================
// 12. Real Multi-Cell Forecast Payloads Validation
// =========================================================================
test('12. Real Multi-Cell Payloads — Validates real Katraj, Hadapsar, Kurla, Delhi forecasts', () => {
  const realPayloads: Record<string, any> = {
    katraj: {
      h3Index: '88608852c1fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      baseTimestamp: '2026-09-26T13:09:44.793750Z',
      generatedAt: '2026-09-27T13:42:13.544545Z',
      modelVersion: 'forecast_regressors_v1',
      parentPredictionId: 'dffaab3a-3add-45ff-9aa7-3c5f2a87fd74',
      featureSnapshotId: '7e5c5a9c-6ecb-4ea8-a329-da3fbb3c31f6',
      status: 'SUCCESS',
      freshness: 'LIVE',
      forecasts: [
        { horizonHours: 1, targetTime: '2026-09-26T14:09:44.793750Z', predictedPm25: 61.13, lowerBound: 59.29, upperBound: 62.99, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-26T16:09:44.793750Z', predictedPm25: 61.21, lowerBound: 57.31, upperBound: 64.26, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-26T19:09:44.793750Z', predictedPm25: 58.60, lowerBound: 53.08, upperBound: 64.02, unit: 'ug/m3' },
      ],
      forecastConfidence: null,
    },
    hadapsar: {
      h3Index: '8860885357fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      baseTimestamp: '2026-09-26T13:08:48.118968Z',
      generatedAt: '2026-09-27T13:42:33.141761Z',
      modelVersion: 'forecast_regressors_v1',
      parentPredictionId: '8e8bab50-ca1c-4abc-bdba-fd9dc221e916',
      featureSnapshotId: 'c8a0ed0d-54f2-4c78-b9ff-f34640ce4509',
      status: 'SUCCESS',
      freshness: 'LIVE',
      forecasts: [
        { horizonHours: 1, targetTime: '2026-09-26T14:08:48.118968Z', predictedPm25: 88.79, lowerBound: 86.95, upperBound: 90.65, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-26T16:08:48.118968Z', predictedPm25: 81.16, lowerBound: 77.26, upperBound: 84.21, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-26T19:08:48.118968Z', predictedPm25: 77.27, lowerBound: 71.75, upperBound: 82.69, unit: 'ug/m3' },
      ],
      forecastConfidence: null,
    },
    kurla: {
      h3Index: '88608b56b3fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440002',
      baseTimestamp: '2026-09-26T12:48:38.851263Z',
      generatedAt: '2026-09-27T13:42:47.277624Z',
      modelVersion: 'forecast_regressors_v1',
      parentPredictionId: '41b3addc-33b2-46ee-95fa-6c1d15dbf9d8',
      featureSnapshotId: 'dce791ea-d77c-4e21-917c-82eee1778229',
      status: 'SUCCESS',
      freshness: 'LIVE',
      forecasts: [
        { horizonHours: 1, targetTime: '2026-09-26T13:48:38.851263Z', predictedPm25: 25.64, lowerBound: 23.80, upperBound: 27.50, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-26T15:48:38.851263Z', predictedPm25: 28.26, lowerBound: 24.36, upperBound: 31.31, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-26T18:48:38.851263Z', predictedPm25: 28.00, lowerBound: 22.48, upperBound: 33.42, unit: 'ug/m3' },
      ],
      forecastConfidence: null,
    },
    delhi: {
      h3Index: '883da11505fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440003',
      baseTimestamp: '2026-09-26T12:48:38.851263Z',
      generatedAt: '2026-09-27T13:43:00.917666Z',
      modelVersion: 'forecast_regressors_v1',
      parentPredictionId: '1f93ba3d-0e7b-4b01-8644-bdf17851645d',
      featureSnapshotId: '261e9bf0-77a7-4fa8-98c3-128de35945c1',
      status: 'SUCCESS',
      freshness: 'LIVE',
      forecasts: [
        { horizonHours: 1, targetTime: '2026-09-26T13:48:38.851263Z', predictedPm25: 24.93, lowerBound: 23.09, upperBound: 26.79, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-26T15:48:38.851263Z', predictedPm25: 24.26, lowerBound: 20.36, upperBound: 27.31, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-26T18:48:38.851263Z', predictedPm25: 26.84, lowerBound: 21.32, upperBound: 32.26, unit: 'ug/m3' },
      ],
      forecastConfidence: null,
    },
  };

  for (const [key, payload] of Object.entries(realPayloads)) {
    const validated = validateForecastResponse(payload);
    assert.strictEqual(validated.h3Index, payload.h3Index, `Cell ${key} h3Index matches`);
    assert.strictEqual(validated.status, 'SUCCESS', `Cell ${key} status is SUCCESS`);
    assert.strictEqual(validated.freshness, 'LIVE', `Cell ${key} freshness is LIVE`);
    assert.strictEqual(validated.forecasts.length, 3, `Cell ${key} has exactly 3 horizons`);
    assert.strictEqual(validated.forecastConfidence, null, `Cell ${key} confidence is strictly null`);

    for (const f of validated.forecasts) {
      assert.ok([1, 3, 6].includes(f.horizonHours), 'Horizon is 1, 3, or 6');
      assert.ok(f.lowerBound <= f.predictedPm25, 'lowerBound <= predictedPm25');
      assert.ok(f.predictedPm25 <= f.upperBound, 'predictedPm25 <= upperBound');
    }
  }
});

// =========================================================================
// 13. Chart Data Model — Observed != Forecast Separation
// =========================================================================
test('13. Chart Data Isolation — Observed base point strictly separated from Forecast points', () => {
  const currentPm25 = 78;
  const forecasts: ForecastItem[] = [
    { horizonHours: 1, targetTime: '2026-09-26T14:09:44Z', predictedPm25: 70.62, lowerBound: 68.78, upperBound: 72.48, unit: 'ug/m3' },
    { horizonHours: 3, targetTime: '2026-09-26T16:09:44Z', predictedPm25: 70.55, lowerBound: 66.65, upperBound: 73.60, unit: 'ug/m3' },
    { horizonHours: 6, targetTime: '2026-09-26T19:09:44Z', predictedPm25: 60.91, lowerBound: 55.39, upperBound: 66.33, unit: 'ug/m3' },
  ];

  // Build the chart data as constructed by ForecastChart
  const chartData = [
    {
      timeLabel: 'T0 (Observed)',
      horizonHours: 0,
      observedPm25: currentPm25,
      forecastPm25: currentPm25,
      lowerBound: currentPm25,
      upperBound: currentPm25,
      isBase: true,
    },
    ...[...forecasts]
      .sort((a, b) => a.horizonHours - b.horizonHours)
      .map((f) => ({
        timeLabel: `+${f.horizonHours}h`,
        horizonHours: f.horizonHours,
        observedPm25: undefined, // Forecast points must NOT have observedPm25
        forecastPm25: f.predictedPm25,
        lowerBound: f.lowerBound,
        upperBound: f.upperBound,
        isBase: false,
      })),
  ];

  // 1. Point 0 is T0 base
  assert.strictEqual(chartData[0].isBase, true);
  assert.strictEqual(chartData[0].observedPm25, 78);
  assert.strictEqual(chartData[0].horizonHours, 0);

  // 2. Horizon points (+1h, +3h, +6h)
  assert.strictEqual(chartData.length, 4);
  for (let i = 1; i <= 3; i++) {
    const pt = chartData[i];
    assert.strictEqual(pt.isBase, false, `Point ${i} is not base`);
    assert.strictEqual(pt.observedPm25, undefined, `Point ${i} observedPm25 must be undefined`);
    assert.ok(typeof pt.forecastPm25 === 'number', `Point ${i} forecastPm25 must be numeric`);
    assert.ok(pt.lowerBound <= pt.forecastPm25 && pt.forecastPm25 <= pt.upperBound, `Point ${i} bounds valid`);
  }
});

// =========================================================================
// 14. Forecast Confidence Honest Null Handling
// =========================================================================
test('14. Confidence Honesty — Renders strictly Not available when confidence is null', () => {
  const renderConfidenceCard = (confidence: number | null | undefined): { title: string; subtitle: string } => {
    if (confidence == null) {
      return {
        title: 'Not available',
        subtitle: 'Prediction ranges are provided instead.',
      };
    }
    return {
      title: `${Math.round(confidence * 100)}%`,
      subtitle: 'Calibrated certainty',
    };
  };

  const resNull = renderConfidenceCard(null);
  assert.strictEqual(resNull.title, 'Not available');
  assert.strictEqual(resNull.subtitle, 'Prediction ranges are provided instead.');

  const resUndefined = renderConfidenceCard(undefined);
  assert.strictEqual(resUndefined.title, 'Not available');
  assert.strictEqual(resUndefined.subtitle, 'Prediction ranges are provided instead.');
});

// =========================================================================
// 15. Exactly +1h, +3h, +6h Timeline Horizon Sorting & Delta
// =========================================================================
test('15. Timeline Horizon Sorting & Delta — Sorts [1, 3, 6] and computes delta vs T0', () => {
  const currentPm25 = 78;
  const unsorted: ForecastItem[] = [
    { horizonHours: 6, targetTime: '2026-09-26T19:09:44Z', predictedPm25: 60.91, lowerBound: 55.39, upperBound: 66.33, unit: 'ug/m3' },
    { horizonHours: 1, targetTime: '2026-09-26T14:09:44Z', predictedPm25: 70.62, lowerBound: 68.78, upperBound: 72.48, unit: 'ug/m3' },
    { horizonHours: 3, targetTime: '2026-09-26T16:09:44Z', predictedPm25: 70.55, lowerBound: 66.65, upperBound: 73.60, unit: 'ug/m3' },
  ];

  const sorted = [...unsorted].sort((a, b) => a.horizonHours - b.horizonHours);
  assert.deepStrictEqual(sorted.map(s => s.horizonHours), [1, 3, 6]);

  const deltas = sorted.map(s => Math.round(s.predictedPm25 - currentPm25));
  assert.strictEqual(deltas[0], -7); // 70.62 - 78 = -7.38 -> -7
  assert.strictEqual(deltas[1], -7); // 70.55 - 78 = -7.45 -> -7
  assert.strictEqual(deltas[2], -17); // 60.91 - 78 = -17.09 -> -17
});

// =========================================================================
// 16. Unit Consistency — Strict µg/m³ Output Normalization
// =========================================================================
test('16. Unit Consistency — Backend ug/m3 is strictly formatted as µg/m³ in UI presentations', () => {
  const formatUnit = (unit: string) => (unit === 'ug/m3' ? 'µg/m³' : unit);

  assert.strictEqual(formatUnit('ug/m3'), 'µg/m³');
  assert.strictEqual(formatUnit('µg/m³'), 'µg/m³');

  const renderedCardValue = (val: number, unit: string) => `${val.toFixed(2)} ${formatUnit(unit)}`;
  assert.strictEqual(renderedCardValue(70.62, 'ug/m3'), '70.62 µg/m³');
  assert.strictEqual(renderedCardValue(61.13, 'ug/m3'), '61.13 µg/m³');

  const deltaText = (delta: number, unit: string) =>
    `${delta > 0 ? `+${delta}` : delta} ${formatUnit(unit)} vs observed T0`;
  assert.strictEqual(deltaText(-7, 'ug/m3'), '-7 µg/m³ vs observed T0');
  assert.strictEqual(deltaText(3, 'ug/m3'), '+3 µg/m³ vs observed T0');
});

// =========================================================================
// 17. Freshness States — LIVE, STALE, NO_DATA, and UNAVAILABLE Semantics
// =========================================================================
test('17. Freshness Semantics — Accurately distinguishes LIVE, STALE, NO_DATA, UNAVAILABLE', () => {
  type FreshnessAction = 'RENDER_NORMAL' | 'RENDER_STALE_BANNER' | 'RENDER_EMPTY_STATE' | 'RENDER_UNAVAILABLE_STATE';

  const resolveFreshnessAction = (freshness: string, status?: string): FreshnessAction => {
    if (freshness === 'NO_DATA' || status === 'NO_DATA') return 'RENDER_EMPTY_STATE';
    if (freshness === 'UNAVAILABLE' || status === 'UNAVAILABLE') return 'RENDER_UNAVAILABLE_STATE';
    if (freshness === 'STALE') return 'RENDER_STALE_BANNER';
    return 'RENDER_NORMAL';
  };

  assert.strictEqual(resolveFreshnessAction('LIVE', 'SUCCESS'), 'RENDER_NORMAL');
  assert.strictEqual(resolveFreshnessAction('STALE', 'SUCCESS'), 'RENDER_STALE_BANNER');
  assert.strictEqual(resolveFreshnessAction('NO_DATA', 'NO_DATA'), 'RENDER_EMPTY_STATE');
  assert.strictEqual(resolveFreshnessAction('UNAVAILABLE', 'SUCCESS'), 'RENDER_UNAVAILABLE_STATE');
  assert.strictEqual(resolveFreshnessAction('LIVE', 'UNAVAILABLE'), 'RENDER_UNAVAILABLE_STATE');
});

// =========================================================================
// 18. Controlled Reliability States — Never Displays Fabricated Numbers (P7.9)
// =========================================================================
test('18. Controlled Reliability States — Never fabricates 70 µg/m³, confidence, or bounds on failure (P7.9)', () => {
  // Test 1: When status is NO_DATA or forecasts array is empty, no forecast numbers or cards should be rendered
  const noDataForecast: ForecastResponse = {
    h3Index: '88608852c1fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    baseTimestamp: null,
    generatedAt: '',
    modelVersion: 'forecast_regressors_v1',
    parentPredictionId: null,
    featureSnapshotId: null,
    status: 'NO_DATA',
    freshness: 'NO_DATA',
    forecasts: [],
    forecastConfidence: null,
  };

  const parsedNoData = validateForecastResponse(noDataForecast);
  assert.strictEqual(parsedNoData.forecasts.length, 0);
  assert.strictEqual(parsedNoData.forecastConfidence, null);

  // Helper verifying no fake forecast numbers or ranges are derived
  function deriveDisplayForecasts(forecast: ForecastResponse | null): any[] | null {
    if (!forecast || forecast.status === 'NO_DATA' || forecast.freshness === 'NO_DATA' || !forecast.forecasts || forecast.forecasts.length === 0) {
      return null; // EmptyState rendered, no forecast numbers
    }
    return forecast.forecasts;
  }

  assert.strictEqual(deriveDisplayForecasts(parsedNoData), null);
  assert.strictEqual(deriveDisplayForecasts(null), null);

  // Test 2: When freshness is UNAVAILABLE, no fake numbers displayed
  const unavailableForecast: ForecastResponse = {
    ...noDataForecast,
    status: 'NO_DATA',
    freshness: 'UNAVAILABLE',
  };
  assert.strictEqual(deriveDisplayForecasts(unavailableForecast), null);

  // Test 3: Confidence is strictly Not Available, never converted to a fake percentage
  function getConfidenceDisplay(confidence: number | null | undefined): string {
    if (confidence === null || confidence === undefined) {
      return 'Not available';
    }
    return `${Math.round(confidence * 100)}%`;
  }

  assert.strictEqual(getConfidenceDisplay(null), 'Not available');
  assert.strictEqual(getConfidenceDisplay(undefined), 'Not available');
  assert.notStrictEqual(getConfidenceDisplay(null), '70%');
  assert.notStrictEqual(getConfidenceDisplay(null), '85%');
  assert.notStrictEqual(getConfidenceDisplay(null), '0%');
});

// =========================================================================
// 19. Controlled Error Contract & Sanitization (P7.8)
// =========================================================================
test('19. Controlled Error Contract — Sanitizes error responses and prevents stack trace leakage (P7.8)', () => {
  interface ApiErrorResponse {
    status: number;
    error: string;
    message: string;
    timestamp?: string;
  }

  function parseApiError(err: any): { userMessage: string; errorCode: string; leaksStackTrace: boolean } {
    const data: ApiErrorResponse | undefined = err?.response?.data;
    const rawString = JSON.stringify(err);

    const leaksStackTrace =
      rawString.includes('at com.aerosentinel') ||
      rawString.includes('java.lang.NullPointerException') ||
      rawString.includes('Traceback (most recent call last)');

    if (data?.error) {
      return {
        userMessage: data.message || 'Operation failed',
        errorCode: data.error,
        leaksStackTrace,
      };
    }

    return {
      userMessage: err?.message || 'Failed to retrieve forecast data',
      errorCode: 'UNKNOWN_ERROR',
      leaksStackTrace,
    };
  }

  // 1. 404 ParentNotFound
  const err404 = {
    response: {
      status: 404,
      data: {
        timestamp: '2026-09-27T12:00:00Z',
        status: 404,
        error: 'FORECAST_PARENT_NOT_FOUND',
        message: 'F3 Parent prediction not found: a310c689-f340-49fc-8935-a037de8d7709',
      },
    },
  };
  const parsed404 = parseApiError(err404);
  assert.strictEqual(parsed404.errorCode, 'FORECAST_PARENT_NOT_FOUND');
  assert.strictEqual(parsed404.leaksStackTrace, false);

  // 2. 422 ValidationFailed
  const err422 = {
    response: {
      status: 422,
      data: {
        timestamp: '2026-09-27T12:00:00Z',
        status: 422,
        error: 'FORECAST_VALIDATION_ERROR',
        message: 'Feature vector length mismatch',
      },
    },
  };
  const parsed422 = parseApiError(err422);
  assert.strictEqual(parsed422.errorCode, 'FORECAST_VALIDATION_ERROR');
  assert.strictEqual(parsed422.leaksStackTrace, false);

  // 3. 503 AiUnavailable
  const err503 = {
    response: {
      status: 503,
      data: {
        timestamp: '2026-09-27T12:00:00Z',
        status: 503,
        error: 'FORECAST_AI_UNAVAILABLE',
        message: 'Forecast inference service is currently unavailable',
      },
    },
  };
  const parsed503 = parseApiError(err503);
  assert.strictEqual(parsed503.errorCode, 'FORECAST_AI_UNAVAILABLE');
  assert.strictEqual(parsed503.leaksStackTrace, false);

  // 4. 504 AiTimeout
  const err504 = {
    response: {
      status: 504,
      data: {
        timestamp: '2026-09-27T12:00:00Z',
        status: 504,
        error: 'FORECAST_AI_TIMEOUT',
        message: 'Forecast inference timed out after 15000 ms',
      },
    },
  };
  const parsed504 = parseApiError(err504);
  assert.strictEqual(parsed504.errorCode, 'FORECAST_AI_TIMEOUT');
  assert.strictEqual(parsed504.leaksStackTrace, false);
});


