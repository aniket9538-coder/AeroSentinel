export type HotspotRiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export type HotspotFreshness = 'LIVE' | 'STALE' | 'NO_DATA' | 'UNAVAILABLE';

export type DataQualityStatus = 'VALID' | 'MISSING' | 'UNAVAILABLE';

export interface AirContext {
  dataQuality: DataQualityStatus;
  pm25?: number | null;
  pm10?: number | null;
  no2?: number | null;
  so2?: number | null;
  co?: number | null;
  o3?: number | null;
  observedAt?: string | null;
  stationId?: string | null;
  recentPm25Mean24h?: number | null;
}

export interface WeatherContext {
  dataQuality: DataQualityStatus;
  temperature?: number | null;
  humidity?: number | null;
  windSpeedKmh?: number | null;
  windSpeedMps?: number | null;
  windDirection?: number | null;
  surfacePressure?: number | null;
  precipitation?: number | null;
  observedAt?: string | null;
}

export interface MonitoringCoverageContext {
  dataQuality: DataQualityStatus;
  nearestStationDistanceKm?: number | null;
  stationsWithin5kmCount?: number | null;
  monitoringCoverageGapFlag?: number | null;
  spatialCoverageConfidence?: number | null;
}

export interface SpatialDispersionContext {
  dataQuality: DataQualityStatus;
  pm25SpatialLagMean?: number | null;
  windU?: number | null;
  windV?: number | null;
}

export interface EnvironmentalGisContext {
  dataQuality: DataQualityStatus;
  distToNearestIndustrialKm?: number | null;
  distToNearestMajorRoadKm?: number | null;
  sensitiveReceptorsCount2km?: number | null;
  industrialZoneWithin2kmFlag?: number | null;
  fireCount24h25km?: number | null;
  fireFrpSum24h25km?: number | null;
  fireFrpMean24h25km?: number | null;
  nearestFireDistanceKm?: number | null;
  fireFrpDistanceDecay?: number | null;
  fireUpwindAlignmentScore?: number | null;
}

export interface ConfidenceBreakdown {
  overallConfidence: number;
  dataQualityScore: number;
  spatialCoverageConfidence: number;
  modelCertainty: number;
  nearestStationDistanceKm: number;
  epistemicUncertaintyFlag: number;
}

export interface HotspotSpatialContext {
  predictionId: string;
  h3Index: string;
  cityId: string;
  cityName: string;
  featureSnapshotId?: string | null;
  predictedAt: string;
  riskScore: number;
  riskLevel: HotspotRiskLevel;
  confidence: number;
  engineType: string;
  modelVersion: string;
  freshness: HotspotFreshness;
  airContext?: AirContext;
  weatherContext?: WeatherContext;
  monitoringCoverage?: MonitoringCoverageContext;
  spatialDispersion?: SpatialDispersionContext;
  environmentalGis?: EnvironmentalGisContext;
  isHotspot?: boolean;
  operationalThreshold?: number;
  confidenceBreakdown?: ConfidenceBreakdown;
}

export interface HotspotCell {
  h3Index: string;
  gridCellId: string;
  riskScore: number;
  riskLevel: HotspotRiskLevel;
  confidence: number;
  predictedAt: string;
  freshness: HotspotFreshness;
  modelVersion: string;
  predictionId?: string;
  cityId?: string;
  cityName?: string;
  engineType?: string;
  featureSnapshotId?: string;
  spatialContext?: HotspotSpatialContext;
  isHotspot?: boolean;
  operationalThreshold?: number;
}

export interface HotspotOverviewResponse {
  cityId: string;
  cityName: string;
  generatedAt: string;
  modelVersion: string;
  engineType: string;
  freshness: HotspotFreshness;
  totalCells: number;
  highRiskCells: number;
  cells: HotspotCell[];
  operationalThreshold?: number;
}
