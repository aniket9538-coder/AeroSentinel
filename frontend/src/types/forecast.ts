/**
 * AeroSentinel - Authoritative Forecast Frontend Contract (F4-P5)
 * Matches the locked Spring Boot P4 ForecastResponse API contract.
 */

export interface ForecastItem {
  horizonHours: number;      // Strictly in {1, 3, 6}
  targetTime: string;        // ISO-8601 target timestamp (T0 + horizonHours)
  predictedPm25: number;     // Multi-horizon regressor prediction (ug/m3)
  lowerBound: number;        // Physical lower bound clamped at >= 0.0
  upperBound: number;        // Upper empirical residual bound
  unit: string;              // Strictly "ug/m3"
}

export type ForecastStatus = 'SUCCESS' | 'NO_DATA' | 'ERROR';
export type ForecastFreshness = 'LIVE' | 'STALE' | 'UNAVAILABLE' | 'NO_DATA';

export interface ForecastResponse {
  h3Index: string;
  cityId: string;
  baseTimestamp: string | null;       // T0: Parent prediction / feature observation time
  generatedAt: string;               // Wall-clock ML inference execution time
  modelVersion: string;              // "forecast_regressors_v1"
  parentPredictionId: string | null; // F3 parent hotspot prediction UUID
  featureSnapshotId: string | null;  // F2/F3 feature snapshot UUID
  status: ForecastStatus;
  freshness: ForecastFreshness;
  forecasts: ForecastItem[];
  forecastConfidence: number | null; // STRICTLY null by design (no synthetic confidence)
}

export interface ForecastGenerateRequest {
  parentPredictionId: string;
  cityId?: string;
  h3Index?: string;
}
