/**
 * Authoritative types for F8 Monitoring Gap & Recommendation UI (F8-P4 / F8-P5).
 */

export type MonitoringPriorityLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type MonitoringRecommendationType =
  | 'ROUTINE_MONITORING'
  | 'TARGETED_MONITORING'
  | 'MOBILE_SENSOR_RECOMMENDED'
  | 'FIELD_VERIFICATION_RECOMMENDED';

export interface MonitoringRecommendation {
  h3Index: string;
  latitude: number;
  longitude: number;

  // F3 Risk Lineage
  riskScore: number;
  riskLevel: string;
  f3Confidence?: number | null;
  predictionId?: string;
  predictionTimestamp?: string;

  // F4 Forecast Lineage
  forecastHorizonHours?: number;
  predictedPm25?: number;
  lowerBound?: number;
  upperBound?: number;
  uncertaintyIntervalWidth?: number;
  normalizedUncertainty?: number;
  forecastGeneratedAt?: string;

  // F8-P2 Coverage Lineage
  nearestStationId?: string | null;
  nearestStationCode?: string | null;
  nearestStationName?: string | null;
  nearestStationDistanceKm?: number | null;
  stationsWithin5kmCount?: number;
  monitoringCoverageGapFlag?: number;

  // F8-P3 Priority Lineage
  normalizedRisk?: number;
  normalizedDistance?: number;
  priorityScore: number;
  priorityScorePercent: number;
  priorityLevel: MonitoringPriorityLevel;

  // F8-P4 Recommendation Lineage
  recommendationType: MonitoringRecommendationType;
  recommendation: string;
  rationale: string;

  // Frontend Backward Compatibility Aliases
  uncertainty: number;
  stationDistanceKm: number;
}

export interface MonitoringSummaryMetrics {
  totalCells: number;
  highPriorityCount: number;
  mediumPriorityCount: number;
  lowPriorityCount: number;
  coverageGapsCount: number;
  maxDistanceKm: number;
  avgDistanceKm: number;
}
