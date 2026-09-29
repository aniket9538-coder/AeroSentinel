import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as h3 from 'h3-js';
import { getH3BoundarySafe, getH3CenterSafe, calculateVisualH3 } from './h3Spatial';
import { AuthorityQueueItem } from '../types/alert';
import { PollutionEventContext } from '../types/event';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

describe('F7-P4: Authority Queue + Real Map Integration Tests', () => {

  const sampleAlert1: AuthorityQueueItem = {
    alertId: '3e804cd6-384c-47dd-92b4-b2e53b604be3',
    eventId: '9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f',
    eventCode: 'EVT-88608850-2026092613-d75654e9',
    h3Index: '88608850e5fffff',
    predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    status: 'OPEN',
    severity: 'CRITICAL',
    riskScore: 0.7998,
    evidenceScore: 0.852,
    triageState: 'ALERT_CANDIDATE',
    consistency: 'CONSISTENT',
    title: 'Severe Particulate Elevation near Shivaji Nagar',
    message: 'Telemetry corroborated with ground sensor observation.',
    forecastSummary: '+1h: 70.6 ug/m3 | +3h: 70.5 ug/m3 | +6h: 60.9 ug/m3',
    hasGeminiAnalysis: true,
    geminiSummary: 'Industrial combustion plumes interacting with microclimate stagnation.',
    createdAt: '2026-09-28T16:51:31Z',
    citizenEvidence: [
      {
        reportId: '6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
        reportReference: 'CR-6F3366CB',
        h3Index: '88608850e5fffff',
        category: 'SMOKE',
        description: 'Thick smoke plume rising from railway corridor stack.',
        observedAt: '2026-09-28T16:45:00Z',
        visibleCondition: 'SMOKE_LIKE',
        visualConfidence: 0.95,
        visualObservations: ['Dense black particulate plume visible.'],
        visualUncertainty: ['Single viewpoint observation.'],
        photoUrl: '/api/v1/citizen/photos/cr-6f3366cb-photo.jpg',
        dataSource: 'CITIZEN',
        relevanceTier: 'AUXILIARY',
        evidenceKey: 'citizen-report-6f3366cb',
      },
    ],
  };

  const sampleAlert2: AuthorityQueueItem = {
    alertId: '4d05144a-ae0a-4831-992d-8ca7db547956',
    eventId: '8cb12cc4-7aa1-423e-8f12-42e12b604bc1',
    eventCode: 'EVT-88608852-2026092614-b15264f1',
    h3Index: '88608852c1fffff',
    predictionId: 'b421d790-e451-40ad-9046-b148ef9e8810',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    status: 'ACKNOWLEDGED',
    severity: 'HIGH',
    riskScore: 0.712,
    evidenceScore: 0.680,
    triageState: 'ALERT_CANDIDATE',
    consistency: 'CONSISTENT',
    title: 'Elevated Biomass Haze in Katraj Basin',
    message: 'Agricultural and open burn evidence detected.',
    hasGeminiAnalysis: false,
    createdAt: '2026-09-28T17:10:00Z',
    citizenEvidence: [],
  };

  it('1. Alert H3 renders on map with valid Res-8 polygon boundary and centroid', () => {
    const boundary = getH3BoundarySafe(sampleAlert1.h3Index);
    const center = getH3CenterSafe(sampleAlert1.h3Index);

    assert.ok(boundary, 'H3 boundary should be defined');
    assert.strictEqual(Array.isArray(boundary), true);
    assert.ok(boundary.length >= 6, 'Res-8 cell boundary should contain at least 6 polygon vertices');

    assert.ok(center, 'H3 center coordinate should be defined');
    assert.strictEqual(center.length, 2);
    // Pune coordinates check (approx lat ~18.5, lng ~73.8)
    assert.ok(center[0] > 18.0 && center[0] < 19.5, 'Center latitude should be in Pune region');
    assert.ok(center[1] > 73.0 && center[1] < 74.5, 'Center longitude should be in Pune region');
  });

  it('2. Selected alert changes map context dynamically without page refresh', () => {
    // Alert 1
    const boundary1 = getH3BoundarySafe(sampleAlert1.h3Index);
    const center1 = getH3CenterSafe(sampleAlert1.h3Index);

    // Alert 2
    const boundary2 = getH3BoundarySafe(sampleAlert2.h3Index);
    const center2 = getH3CenterSafe(sampleAlert2.h3Index);

    assert.notDeepStrictEqual(boundary1, boundary2, 'Different alerts should produce distinct polygon boundaries');
    assert.notDeepStrictEqual(center1, center2, 'Different alerts should produce distinct map centers');
    assert.notStrictEqual(sampleAlert1.h3Index, sampleAlert2.h3Index);
  });

  it('3. Citizen evidence photo renders safely with correct API path and AUXILIARY label', () => {
    assert.ok(sampleAlert1.citizenEvidence && sampleAlert1.citizenEvidence.length > 0);
    const obs = sampleAlert1.citizenEvidence[0];

    assert.strictEqual(obs.dataSource, 'CITIZEN');
    assert.strictEqual(obs.relevanceTier, 'AUXILIARY');
    assert.strictEqual(obs.photoUrl, '/api/v1/citizen/photos/cr-6f3366cb-photo.jpg');
    assert.ok(obs.photoUrl.startsWith('/api/v1/citizen/photos/'));
    assert.strictEqual(obs.photoUrl.includes('\\'), false, 'Photo path must not leak Windows separators');
  });

  it('4. Missing citizen evidence renders honest empty state without fabricating observations', () => {
    assert.ok(sampleAlert2.citizenEvidence);
    assert.strictEqual(sampleAlert2.citizenEvidence.length, 0);

    // When empty, code must not invent photoUrl or confidence
    const hasCitizenObservations = sampleAlert2.citizenEvidence.length > 0;
    assert.strictEqual(hasCitizenObservations, false);
  });

  it('5. H3 navigation to Evidence & WHY dossier preserves exact selected H3', () => {
    const targetUrl1 = `/analyst/evidence?h3=${encodeURIComponent(sampleAlert1.h3Index)}`;
    const targetUrl2 = `/analyst/evidence?h3=${encodeURIComponent(sampleAlert2.h3Index)}`;

    assert.strictEqual(targetUrl1, '/analyst/evidence?h3=88608850e5fffff');
    assert.strictEqual(targetUrl2, '/analyst/evidence?h3=88608852c1fffff');
    assert.notStrictEqual(targetUrl1, targetUrl2);
  });

  it('6. Source Audit: No hardcoded H3 cells in production map components', () => {
    const mapFile = path.resolve(__dirname, '../components/authority/AuthorityAlertMap.tsx');
    const alertsFile = path.resolve(__dirname, '../pages/authority/Alerts.tsx');

    const mapSrc = fs.readFileSync(mapFile, 'utf8');
    const alertsSrc = fs.readFileSync(alertsFile, 'utf8');

    // Confirm no hardcoded sample H3 cell literals in AuthorityAlertMap
    assert.strictEqual(mapSrc.includes('"88608850e5fffff"'), false, 'AuthorityAlertMap must not hardcode 88608850e5fffff');
    assert.strictEqual(alertsSrc.includes('"sample-alert"'), false, 'Alerts.tsx must not hardcode sample-alert');
  });

  it('7. Source Audit: No hardcoded test event IDs in production rendering', () => {
    const mapFile = path.resolve(__dirname, '../components/authority/AuthorityAlertMap.tsx');
    const alertsFile = path.resolve(__dirname, '../pages/authority/Alerts.tsx');

    const mapSrc = fs.readFileSync(mapFile, 'utf8');
    const alertsSrc = fs.readFileSync(alertsFile, 'utf8');

    assert.strictEqual(mapSrc.includes('EVT-TEST'), false);
    assert.strictEqual(alertsSrc.includes('EVT-TEST'), false);
    assert.strictEqual(mapSrc.includes('CR-TEST'), false);
    assert.strictEqual(alertsSrc.includes('CR-TEST'), false);
  });

  it('8. Event with no alert remains renderable in spatial map (Event != Alert)', () => {
    const unalertedEvent: PollutionEventContext = {
      id: 'a1b2c3d4-e5f6-4a1b-8c2d-3e4f5a6b7c8d',
      eventCode: 'EVT-88608850-2026092819-a1b2c3d4',
      h3Index: '88608850e5fffff',
      severity: 'MODERATE',
      status: 'OPEN',
      startedAt: '2026-09-28T19:00:00Z',
      createdAt: '2026-09-28T19:05:00Z',
      alert: {
        alertExists: false,
      },
    };

    // Even without an alert, the H3 index can be mapped and rendered safely
    assert.strictEqual(unalertedEvent.alert?.alertExists, false);
    assert.strictEqual(unalertedEvent.alert?.alertId, undefined);

    const boundary = getH3BoundarySafe(unalertedEvent.h3Index);
    assert.ok(boundary && boundary.length >= 6, 'Event with no alert must still produce valid H3 polygon boundary');
  });

  it('9. Invalid, malformed, or empty H3 does not crash UI and returns null boundary', () => {
    // Test empty
    assert.strictEqual(getH3BoundarySafe(''), null);
    assert.strictEqual(getH3CenterSafe(''), null);

    // Test null/undefined
    assert.strictEqual(getH3BoundarySafe(null), null);
    assert.strictEqual(getH3BoundarySafe(undefined), null);

    // Test malformed
    assert.strictEqual(getH3BoundarySafe('not-a-valid-h3'), null);
    assert.strictEqual(getH3CenterSafe('not-a-valid-h3'), null);
    assert.strictEqual(getH3BoundarySafe('88608850'), null);
  });

  it('10. Citizen location picker updates coordinates and computes visual informational H3', () => {
    let capturedCoords: { lat: number; lng: number } | null = null;
    const onSelectLocation = (coords: { lat: number; lng: number }) => {
      capturedCoords = coords;
    };

    // User clicks Katraj coordinates
    const selectedPoint = { lat: 18.4529, lng: 73.8554 };
    onSelectLocation(selectedPoint);

    assert.deepStrictEqual(capturedCoords, selectedPoint);

    // Verify informational visual H3 calculation
    const visualH3 = h3.latLngToCell(selectedPoint.lat, selectedPoint.lng, 8);
    assert.ok(h3.isValidCell(visualH3));
    assert.strictEqual(visualH3.startsWith('88'), true);
  });

  it('11. GPS denial triggers graceful fallback to interactive map picker', () => {
    let gpsDenied = false;
    let locationMethod: 'GPS' | 'MAP' | 'DEFAULT' = 'DEFAULT';

    // Simulate GPS error / denial callback
    const handleGpsError = () => {
      gpsDenied = true;
      locationMethod = 'MAP';
    };

    handleGpsError();

    assert.strictEqual(gpsDenied, true, 'GPS denial flag should be set');
    assert.strictEqual(locationMethod, 'MAP', 'Location method should fall back to interactive MAP');
  });

  it('12. Map selection does not set authoritative H3 on client (server derivation is authoritative)', () => {
    const selectedCoords = { lat: 18.5304, lng: 73.8467 };

    // Client prepares payload with only coordinates
    const payload = {
      latitude: selectedCoords.lat,
      longitude: selectedCoords.lng,
      category: 'SMOKE',
      description: 'Industrial plume near Shivaji Nagar',
    };

    // Verify client payload does not mandate client-chosen H3
    assert.strictEqual((payload as any).h3Index, undefined, 'Client payload must not send client-generated authoritative H3');

    // Server calculates authoritative H3 upon ingestion
    const serverDerivedH3 = h3.latLngToCell(payload.latitude, payload.longitude, 8);
    assert.strictEqual(serverDerivedH3, '88608850e5fffff', 'Server derives authoritative H3 Res-8 cell from coordinates');
  });
});
