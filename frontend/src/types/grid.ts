/**
 * Canonical H3 Spatial Grid Types for Feature 2 (F2).
 * Strictly mirrors backend DTOs:
 * - LatLngPoint
 * - GridCellResponse
 * - GridAirObservationResponse
 * - GridWeatherObservationResponse
 * - GridCellObservationResponse
 */

export interface LatLngPoint {
  lat: number;
  lng: number;
}

export interface GridCellResponse {
  h3Index: string;
  cityId: string;
  resolution: number;
  center: LatLngPoint;
  boundary: LatLngPoint[];
}

export interface GridAirObservationResponse {
  id: string;
  stationId: string;
  stationName?: string;
  pm25: number;
  observedAt: string;
  source: string;
  quality: string;
  h3Index: string;
}

export interface GridWeatherObservationResponse {
  id: string;
  temperature: number;
  humidity: number;
  windSpeed: number;
  windDirection: number;
  rainfall: number;
  observedAt: string;
  source: string;
  h3Index: string;
}

export interface GridCellObservationResponse {
  h3Index: string;
  cityId: string;
  airObservations: GridAirObservationResponse[];
  weatherObservations: GridWeatherObservationResponse[];
}
