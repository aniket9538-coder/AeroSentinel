import apiClient from './api';
import { WeatherLatestResponse } from '../types/weather';

/**
 * Weather API Service
 * Consumes canonical Phase 5 backend endpoint:
 * - GET /api/v1/cities/{cityId}/weather/latest
 */
export const weatherApi = {
  /**
   * Fetches the latest real weather observation for a selected city from PostgreSQL.
   * Path: GET /api/v1/cities/{cityId}/weather/latest
   *
   * @param cityId City UUID
   */
  getLatestWeather: async (cityId: string): Promise<WeatherLatestResponse> => {
    if (!cityId || !cityId.trim()) {
      throw new Error("Parameter 'cityId' is required");
    }
    const response = await apiClient.get<WeatherLatestResponse>(
      `/cities/${encodeURIComponent(cityId.trim())}/weather/latest`
    );
    return response.data;
  },
};

export default weatherApi;
