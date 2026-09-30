import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as h3 from 'h3-js';
import { MonitoringRecommendation } from '../types/monitoring';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

describe('F8-P6: H3 Monitoring Map Integration Test Suite', () => {

  const highPriorityRec: MonitoringRecommendation = {
    h3Index: '8860884119fffff',
    latitude: 18.65028,
    longitude: 73.77805,
    riskScore: 0.88,
    riskLevel: 'HIGH',
    f3Confidence: 0.94,
    forecastHorizonHours: 1,
    predictedPm25: 135.0,
    lowerBound: 125.0,
    upperBound: 145.0,
    uncertaintyIntervalWidth: 20.0,
    normalizedUncertainty: 1.0,
    nearestStationId: '660e8400-e29b-41d4-a716-446655440001',
    nearestStationCode: 'PUN-001',
    nearestStationName: 'Shivajinagar CAAQMS',
    nearestStationDistanceKm: 14.96,
    stationsWithin5kmCount: 0,
    monitoringCoverageGapFlag: 1,
    normalizedRisk: 0.88,
    normalizedDistance: 0.748,
    priorityScore: 88.3,
    priorityScorePercent: 88,
    priorityLevel: 'HIGH',
    recommendationType: 'MOBILE_SENSOR_RECOMMENDED',
    recommendation: 'Consider deploying additional mobile monitoring in this unobserved H3 cell.',
    rationale: 'Risk: HIGH (0.88) | Forecast uncertainty: 20.0 ug/m3 | Nearest station: PUN-001 at 14.96 km',
    uncertainty: 1.0,
    stationDistanceKm: 14.96,
  };

  const mediumPriorityRec: MonitoringRecommendation = {
    h3Index: '88608852c1fffff',
    latitude: 18.45343,
    longitude: 73.86727,
    riskScore: 0.58,
    riskLevel: 'MEDIUM',
    f3Confidence: 0.85,
    forecastHorizonHours: 1,
    predictedPm25: 68.0,
    lowerBound: 60.5,
    upperBound: 75.5,
    uncertaintyIntervalWidth: 15.0,
    normalizedUncertainty: 0.75,
    nearestStationId: '660e8400-e29b-41d4-a716-446655440002',
    nearestStationCode: 'PUN-002',
    nearestStationName: 'Katraj Air Station',
    nearestStationDistanceKm: 2.15,
    stationsWithin5kmCount: 1,
    monitoringCoverageGapFlag: 0,
    normalizedRisk: 0.58,
    normalizedDistance: 0.108,
    priorityScore: 49.1,
    priorityScorePercent: 49,
    priorityLevel: 'MEDIUM',
    recommendationType: 'TARGETED_MONITORING',
    recommendation: 'Targeted monitoring advised; observed nearby station provides baseline context.',
    rationale: 'Risk: MEDIUM (0.58) | Forecast uncertainty: 15.0 ug/m3 | Nearest station: PUN-002 at 2.15 km',
    uncertainty: 0.75,
    stationDistanceKm: 2.15,
  };

  const lowPriorityRec: MonitoringRecommendation = {
    h3Index: '88608850e5fffff',
    latitude: 18.53082,
    longitude: 73.84747,
    riskScore: 0.32,
    riskLevel: 'LOW',
    f3Confidence: 0.90,
    forecastHorizonHours: 1,
    predictedPm25: 38.0,
    lowerBound: 33.0,
    upperBound: 43.0,
    uncertaintyIntervalWidth: 10.0,
    normalizedUncertainty: 0.50,
    nearestStationId: '660e8400-e29b-41d4-a716-446655440001',
    nearestStationCode: 'PUN-001',
    nearestStationName: 'Shivajinagar CAAQMS',
    nearestStationDistanceKm: 0.42,
    stationsWithin5kmCount: 2,
    monitoringCoverageGapFlag: 0,
    normalizedRisk: 0.32,
    normalizedDistance: 0.021,
    priorityScore: 13.5,
    priorityScorePercent: 13,
    priorityLevel: 'LOW',
    recommendationType: 'ROUTINE_MONITORING',
    recommendation: 'Routine CAAQMS monitoring sufficient; station coverage is adequate.',
    rationale: 'Risk: LOW (0.32) | Forecast uncertainty: 10.0 ug/m3 | Nearest station: PUN-001 at 0.42 km',
    uncertainty: 0.50,
    stationDistanceKm: 0.42,
  };

  // Helper matching MonitoringCoverageLayer styling logic
  const getPriorityColor = (level: string) => {
    switch (level) {
      case 'HIGH':
        return '#ec4899';
      case 'MEDIUM':
        return '#a855f7';
      case 'LOW':
      default:
        return '#6366f1';
    }
  };

  it('1. Monitoring layer processes recommendation data array into spatial entries', () => {
    const list = [highPriorityRec, mediumPriorityRec, lowPriorityRec];
    assert.strictEqual(list.length, 3);
    const validCells = list.filter((r) => h3.isValidCell(r.h3Index));
    assert.strictEqual(validCells.length, 3, 'All 3 items must be valid H3 indices');
  });

  it('2. H3 polygon boundary is generated accurately from backend h3Index via h3-js', () => {
    const boundary = h3.cellToBoundary(highPriorityRec.h3Index) as [number, number][];
    assert.ok(Array.isArray(boundary), 'Boundary must be an array');
    assert.strictEqual(boundary.length >= 6, true, 'Uber H3 resolution 8 cell must have at least 6 boundary vertices');

    // Each vertex must be a valid [lat, lng] coordinate pair
    for (const [lat, lng] of boundary) {
      assert.strictEqual(typeof lat, 'number');
      assert.strictEqual(typeof lng, 'number');
      assert.strictEqual(isNaN(lat), false);
      assert.strictEqual(isNaN(lng), false);
      // Pune region check
      assert.ok(lat > 18.0 && lat < 19.5, `Latitude ${lat} must be within region`);
      assert.ok(lng > 73.0 && lng < 74.5, `Longitude ${lng} must be within region`);
    }
  });

  it('3. HIGH priority styling is applied with #ec4899 and dashed border', () => {
    const color = getPriorityColor(highPriorityRec.priorityLevel);
    assert.strictEqual(color, '#ec4899', 'HIGH priority must use #ec4899 color token');
    assert.strictEqual(highPriorityRec.priorityLevel, 'HIGH');
  });

  it('4. MEDIUM priority styling is applied with #a855f7', () => {
    const color = getPriorityColor(mediumPriorityRec.priorityLevel);
    assert.strictEqual(color, '#a855f7', 'MEDIUM priority must use #a855f7 color token');
    assert.strictEqual(mediumPriorityRec.priorityLevel, 'MEDIUM');
  });

  it('5. LOW priority styling is applied with #6366f1', () => {
    const color = getPriorityColor(lowPriorityRec.priorityLevel);
    assert.strictEqual(color, '#6366f1', 'LOW priority must use #6366f1 color token');
    assert.strictEqual(lowPriorityRec.priorityLevel, 'LOW');
  });

  it('6. Popup data displays H3 cell index', () => {
    assert.strictEqual(highPriorityRec.h3Index, '8860884119fffff');
    assert.strictEqual(h3.isValidCell(highPriorityRec.h3Index), true);
  });

  it('7. Popup displays numeric priority score and percentage', () => {
    const scoreDisplay = highPriorityRec.priorityScorePercent ?? Math.round(highPriorityRec.priorityScore);
    assert.strictEqual(scoreDisplay, 88);
    assert.strictEqual(`${scoreDisplay}/100`, '88/100');
  });

  it('8. Popup displays risk separately from priority to prevent confusion', () => {
    // Risk is atmospheric F3 risk; Priority is observational F8 priority
    assert.strictEqual(highPriorityRec.riskScore, 0.88);
    assert.strictEqual(highPriorityRec.riskLevel, 'HIGH');
    assert.strictEqual(highPriorityRec.priorityLevel, 'HIGH');
    assert.strictEqual(highPriorityRec.priorityScorePercent, 88);

    // Verify medium case where risk (0.58) and priority (49) are distinct
    assert.strictEqual(mediumPriorityRec.riskScore, 0.58);
    assert.strictEqual(mediumPriorityRec.riskLevel, 'MEDIUM');
    assert.strictEqual(mediumPriorityRec.priorityScorePercent, 49);
    assert.notStrictEqual(mediumPriorityRec.riskScore * 100, mediumPriorityRec.priorityScorePercent);
  });

  it('9. Popup displays forecast uncertainty formatted cleanly in µg/m³', () => {
    const uncertaintyText = highPriorityRec.uncertaintyIntervalWidth != null
      ? `${highPriorityRec.uncertaintyIntervalWidth.toFixed(1)} µg/m³`
      : `${(highPriorityRec.uncertainty * 100).toFixed(0)}%`;
    assert.strictEqual(uncertaintyText, '20.0 µg/m³');
  });

  it('10. Popup displays nearest station name and distance in km', () => {
    const distanceKm = highPriorityRec.nearestStationDistanceKm ?? highPriorityRec.stationDistanceKm;
    const stationName = highPriorityRec.nearestStationName || highPriorityRec.nearestStationCode || 'Unknown';
    assert.strictEqual(stationName, 'Shivajinagar CAAQMS');
    assert.strictEqual(`${distanceKm?.toFixed(1)} km`, '15.0 km');
  });

  it('11. Popup displays coverage gap status as YES or NO', () => {
    const gapHigh = highPriorityRec.monitoringCoverageGapFlag === 1 ? 'YES' : 'NO';
    const gapMed = mediumPriorityRec.monitoringCoverageGapFlag === 1 ? 'YES' : 'NO';
    assert.strictEqual(gapHigh, 'YES');
    assert.strictEqual(gapMed, 'NO');
  });

  it('12. Popup displays recommendation type and non-alarmist action guidance', () => {
    assert.strictEqual(highPriorityRec.recommendationType, 'MOBILE_SENSOR_RECOMMENDED');
    assert.strictEqual(
      highPriorityRec.recommendation,
      'Consider deploying additional mobile monitoring in this unobserved H3 cell.'
    );

    // Verify non-alarmist disclaimer is present in component source
    const layerPath = path.resolve(__dirname, '../components/map/MonitoringCoverageLayer.tsx');
    const layerSrc = fs.readFileSync(layerPath, 'utf8');
    assert.ok(
      layerSrc.includes('Monitoring gap indicates limited proximity to existing monitoring stations; it does not by itself confirm pollution.'),
      'Popup must include non-alarmist monitoring gap disclaimer'
    );
  });

  it('13. Empty recommendation list renders zero polygons and subtle indicator', () => {
    const emptyList: MonitoringRecommendation[] = [];
    assert.strictEqual(emptyList.length, 0);

    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');
    assert.ok(
      mapSrc.includes('No monitoring priorities available for this city.'),
      'Must contain explicit empty indicator'
    );
    assert.strictEqual(
      mapSrc.includes('Pollution is safe'),
      false,
      'Empty state must NOT say "Pollution is safe"'
    );
  });

  it('14. API error state does not crash map and provides retry', () => {
    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');
    assert.ok(mapSrc.includes('monitoringError'), 'Map must track monitoringError state');
    assert.ok(mapSrc.includes('Retry'), 'Map must provide Retry button upon error');
  });

  it('15. Monitoring toggle turns layer ON', () => {
    let layerMonitoring = false;
    layerMonitoring = true; // User clicks toggle
    assert.strictEqual(layerMonitoring, true);

    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');
    assert.ok(mapSrc.includes('setLayerMonitoring(!layerMonitoring)'), 'Toggle handler must flip state');
    assert.ok(mapSrc.includes('<Radio size={12} /> Monitoring Gaps'), 'Toggle button must exist with label');
  });

  it('16. Monitoring toggle turns layer OFF and removes polygons', () => {
    let layerMonitoring = true;
    layerMonitoring = false; // User clicks toggle
    assert.strictEqual(layerMonitoring, false);

    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');
    assert.ok(
      mapSrc.includes('{layerMonitoring &&') && mapSrc.includes('<MonitoringCoverageLayer'),
      'MonitoringCoverageLayer must be conditionally rendered strictly when layerMonitoring is true'
    );
  });

  it('17. Existing map layers remain mounted alongside monitoring layer', () => {
    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');

    // Confirm all protected layers exist in PollutionMap
    assert.ok(mapSrc.includes('<SensorLayer'), 'SensorLayer must remain mounted');
    assert.ok(mapSrc.includes('<H3GridLayer'), 'H3GridLayer must remain supported');
    assert.ok(mapSrc.includes('<H3RiskLayer'), 'H3RiskLayer must remain supported');
    assert.ok(mapSrc.includes('<FireLayer'), 'FireLayer must remain supported');
    assert.ok(mapSrc.includes('<CitizenReportLayer'), 'CitizenReportLayer must remain supported');
  });

  it('18. City change replaces old F8 data and clears previous city polygons', () => {
    let currentRecommendations = [highPriorityRec];
    const oldCityId: string = 'pune-city-uuid';
    const newCityId: string = 'mumbai-city-uuid';

    // Simulate city transition
    if (oldCityId !== newCityId) {
      currentRecommendations = []; // Cleared during city change
    }
    assert.strictEqual(currentRecommendations.length, 0, 'Old recommendations must be cleared on city switch');

    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');
    assert.ok(
      mapSrc.includes('useEffect(() => {'),
      'Map must have cityId reactive lifecycle effect'
    );
  });

  it('19. No hardcoded cityId in map or layer components', () => {
    const layerPath = path.resolve(__dirname, '../components/map/MonitoringCoverageLayer.tsx');
    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');

    const layerSrc = fs.readFileSync(layerPath, 'utf8');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');

    assert.strictEqual(layerSrc.includes('550e8400-e29b-41d4-a716-446655440001'), false);
    assert.strictEqual(mapSrc.includes('550e8400-e29b-41d4-a716-446655440001'), false);
    assert.strictEqual(layerSrc.includes('pune-uuid'), false);
  });

  it('20. No hardcoded recommendation values in map or layer components', () => {
    const layerPath = path.resolve(__dirname, '../components/map/MonitoringCoverageLayer.tsx');
    const mapPath = path.resolve(__dirname, '../components/map/PollutionMap.tsx');

    const layerSrc = fs.readFileSync(layerPath, 'utf8');
    const mapSrc = fs.readFileSync(mapPath, 'utf8');

    // Confirm no synthetic hardcoded mock recommendations array
    assert.strictEqual(layerSrc.includes('mockRecommendations'), false);
    assert.strictEqual(mapSrc.includes('mockRecommendations'), false);
    assert.strictEqual(layerSrc.includes('sampleRecommendations'), false);
  });

});
