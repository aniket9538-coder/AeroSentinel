import apiClient from './api';
import {
  GridCellResponse,
  GridCellObservationResponse,
} from '../types/grid';

/**
 * Spatial Grid API Service
 * Consumes canonical Phase 5 backend endpoints:
 * - GET /api/v1/grid?cityId={cityId}
 * - GET /api/v1/grid/{h3Index}
 * - GET /api/v1/grid/{h3Index}/observations
 */
export const gridApi = {
  /**
   * Fetches all real, database-backed H3 grid cells for a city.
   * Path: GET /api/v1/grid?cityId={cityId}
   *
   * @param cityId City UUID
   */
  getCityGrid: async (cityId: string): Promise<GridCellResponse[]> => {
    if (!cityId || !cityId.trim()) {
      throw new Error("Parameter 'cityId' is required");
    }
    const response = await apiClient.get<GridCellResponse[]>('/grid', {
      params: {
        cityId: cityId.trim(),
      },
    });
    return response.data;
  },

  /**
   * Fetches detailed spatial attributes and boundaries for an authoritative H3 cell.
   * Path: GET /api/v1/grid/{h3Index}
   *
   * @param h3Index 15-character hex H3 index
   */
  getCell: async (h3Index: string): Promise<GridCellResponse> => {
    if (!h3Index || !h3Index.trim()) {
      throw new Error("Parameter 'h3Index' is required");
    }
    const response = await apiClient.get<GridCellResponse>(
      `/grid/${encodeURIComponent(h3Index.trim())}`
    );
    return response.data;
  },

  /**
   * Fetches combined air and weather observations for an H3 cell,
   * chronologically ascending.
   * Path: GET /api/v1/grid/{h3Index}/observations
   *
   * @param h3Index 15-character hex H3 index
   */
  getCellObservations: async (
    h3Index: string
  ): Promise<GridCellObservationResponse> => {
    if (!h3Index || !h3Index.trim()) {
      throw new Error("Parameter 'h3Index' is required");
    }
    const response = await apiClient.get<GridCellObservationResponse>(
      `/grid/${encodeURIComponent(h3Index.trim())}/observations`
    );
    return response.data;
  },
};

export default gridApi;
