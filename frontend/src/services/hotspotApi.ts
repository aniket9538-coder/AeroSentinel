import apiClient from './api';
import { HotspotOverviewResponse, HotspotCell } from '../types/hotspot';

export const hotspotApi = {
  /**
   * Retrieves the latest hotspot risk overview for all H3 cells in the city.
   *
   * @param cityId City UUID
   * @returns HotspotOverviewResponse
   */
  getHotspotsByCity: async (cityId: string): Promise<HotspotOverviewResponse> => {
    const response = await apiClient.get<HotspotOverviewResponse>(`/hotspots`, {
      params: { cityId },
    });
    return response.data;
  },

  /**
   * Retrieves the latest hotspot risk prediction for a specific H3 cell.
   *
   * @param h3Index 15-character Uber H3 cell identifier
   * @returns HotspotCell
   */
  getHotspotByH3: async (h3Index: string): Promise<HotspotCell> => {
    const response = await apiClient.get<HotspotCell>(`/hotspots/${h3Index}`);
    return response.data;
  },

  /**
   * Retrieves the rich multi-sensor spatial context for a specific H3 cell.
   * Integration anchor for F4 Forecast and F5 Evidence.
   *
   * @param h3Index 15-character Uber H3 cell identifier
   * @returns HotspotSpatialContext
   */
  getHotspotContext: async (h3Index: string) => {
    const response = await apiClient.get(`/hotspots/${h3Index}/context`);
    return response.data;
  },
};

export default hotspotApi;
