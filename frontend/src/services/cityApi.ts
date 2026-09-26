import apiClient from './api';
import { CityResponse } from '../types/city';

/**
 * City API Service
 * Consumes canonical Phase 5 backend endpoints:
 * - GET /api/v1/cities
 * - GET /api/v1/cities/{cityId}
 */
export const cityApi = {
  /**
   * Fetches all active cities configured in AeroSentinel.
   * Path: GET /api/v1/cities
   */
  getCities: async (): Promise<CityResponse[]> => {
    const response = await apiClient.get<CityResponse[]>('/cities');
    return response.data;
  },

  /**
   * Fetches detailed information for a specific city by UUID.
   * Path: GET /api/v1/cities/{cityId}
   */
  getCityById: async (cityId: string): Promise<CityResponse> => {
    if (!cityId || !cityId.trim()) {
      throw new Error("Parameter 'cityId' is required");
    }
    const response = await apiClient.get<CityResponse>(`/cities/${encodeURIComponent(cityId.trim())}`);
    return response.data;
  },
};

export default cityApi;
