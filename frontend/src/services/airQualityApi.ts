import apiClient from './api';
import {
  LatestAirQualityResponse,
  AirQualityHistoryResponse,
} from '../types/airQuality';

/**
 * Air Quality API Service
 * Consumes canonical Phase 5 backend endpoints:
 * - GET /api/v1/cities/{cityId}/air-quality/latest
 * - GET /api/v1/stations/{stationId}/air-quality?from=...&to=...
 */
export const airQualityApi = {
  /**
   * Fetches the latest available PM2.5 observation for each active station in a city.
   * Path: GET /api/v1/cities/{cityId}/air-quality/latest
   */
  getLatestAirQuality: async (cityId: string): Promise<LatestAirQualityResponse> => {
    if (!cityId || !cityId.trim()) {
      throw new Error("Parameter 'cityId' is required");
    }
    const response = await apiClient.get<LatestAirQualityResponse>(
      `/cities/${encodeURIComponent(cityId.trim())}/air-quality/latest`
    );
    return response.data;
  },

  /**
   * Fetches historical PM2.5 observations for a monitoring station within an ISO UTC date range.
   * Path: GET /api/v1/stations/{stationId}/air-quality?from=...&to=...
   *
   * @param stationId Unique station code (e.g. "PUN-001")
   * @param from ISO UTC start timestamp (e.g. "2026-09-24T00:00:00Z")
   * @param to ISO UTC end timestamp (e.g. "2026-09-24T23:59:59Z")
   */
  getStationHistory: async (
    stationId: string,
    from: string,
    to: string
  ): Promise<AirQualityHistoryResponse> => {
    if (!stationId || !stationId.trim()) {
      throw new Error("Parameter 'stationId' is required");
    }
    if (!from || !from.trim()) {
      throw new Error("Parameter 'from' is required");
    }
    if (!to || !to.trim()) {
      throw new Error("Parameter 'to' is required");
    }

    const response = await apiClient.get<AirQualityHistoryResponse>(
      `/stations/${encodeURIComponent(stationId.trim())}/air-quality`,
      {
        params: {
          from: from.trim(),
          to: to.trim(),
        },
      }
    );

    const data = response.data;
    // Guarantee readings alias is populated if consumers reference readings
    if (data && data.observations && !data.readings) {
      data.readings = data.observations;
    }
    return data;
  },
};

export default airQualityApi;
