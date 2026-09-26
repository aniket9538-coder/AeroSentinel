/**
 * Canonical Weather Types for Feature 2 (F2).
 * Strictly mirrors backend WeatherLatestResponse DTO.
 */
export interface WeatherLatestResponse {
  cityId: string;
  cityName?: string;
  temperature: number | null;
  humidity: number | null;
  windSpeed: number | null;
  windDirection: number | null;
  rainfall: number | null;
  observedAt: string | null;
  source: string | null;
  h3Index: string | null;
}

export type WeatherObservation = WeatherLatestResponse;
