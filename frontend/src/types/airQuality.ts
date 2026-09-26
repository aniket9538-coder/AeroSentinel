/**
 * Data quality flags matching backend semantics:
 * - VALID: observation passed physical checks and validation rules
 * - SUSPECT: observation flagged for secondary verification
 * - INVALID: observation failed physical boundaries or QA/QC checks
 */
export type DataQuality = 'VALID' | 'SUSPECT' | 'INVALID';

/**
 * Freshness status representing data recency and source availability.
 * Conforms to F1 Phase 7 requirements:
 * - LIVE: Observation timestamp is within verified freshness threshold
 * - STALE: Observation timestamp age exceeds verified freshness threshold
 * - NO_DATA: Canonical API successfully returned zero observations
 * - SOURCE_UNAVAILABLE: Telemetry source / provider is explicitly unavailable or unreachable
 */
export type FreshnessStatus = 'LIVE' | 'STALE' | 'NO_DATA' | 'SOURCE_UNAVAILABLE';

/**
 * Telemetry provider provenance sources.
 */
export type TelemetrySource = 'CPCB' | 'MPCB' | 'OPENAQ' | string;

/**
 * Single station observation returned in LatestAirQualityResponse.
 * Maps to backend: com.aerosentinel.dto.air.AirQualityObservationResponse
 */
export interface AirQualityObservationResponse {
  stationId: string;
  stationName: string;
  pm25: number;
  observedAt: string;
  source: TelemetrySource;
  quality: DataQuality | string;
  latitude?: number;
  longitude?: number;
}

/**
 * Response contract for GET /api/v1/cities/{cityId}/air-quality/latest.
 * Maps to backend: com.aerosentinel.dto.air.LatestAirQualityResponse
 */
export interface LatestAirQualityResponse {
  cityId: string;
  cityName?: string;
  observations: AirQualityObservationResponse[];
}

/**
 * Single historical reading point in time.
 * Maps to backend: com.aerosentinel.dto.air.HistoricalObservationResponse
 */
export interface HistoricalObservationResponse {
  pm25: number;
  observedAt: string;
  source?: TelemetrySource;
  quality?: DataQuality | string;
}

/**
 * Response contract for GET /api/v1/stations/{stationId}/air-quality?from=...&to=...
 * Maps to backend: com.aerosentinel.dto.air.AirQualityHistoryResponse
 * Provides both canonical 'observations' array and 'readings' alias.
 */
export interface AirQualityHistoryResponse {
  stationId: string;
  stationName?: string;
  observations: HistoricalObservationResponse[];
  /** Alias for observations to support flexible consumer naming */
  readings?: HistoricalObservationResponse[];
}
