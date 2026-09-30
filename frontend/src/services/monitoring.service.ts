import apiClient from './api';
import { MonitoringRecommendation } from '../types';

export const monitoringService = {
  /**
   * Retrieves monitoring recommendations for all observed H3 cells across a city (F8-P4 / P5).
   * Backend orders cells deterministically by priorityScorePercent descending (with h3Index tie-breaker).
   */
  getRecommendations: async (cityId: string): Promise<MonitoringRecommendation[]> => {
    const response = await apiClient.get<MonitoringRecommendation[]>(`/monitoring/recommendations?cityId=${encodeURIComponent(cityId)}`);
    return response.data;
  },

  /**
   * Retrieves individual monitoring recommendation details for a single H3 cell.
   */
  getRecommendationByH3: async (h3Index: string, cityId?: string): Promise<MonitoringRecommendation> => {
    const url = cityId
      ? `/monitoring/recommendations/${encodeURIComponent(h3Index)}?cityId=${encodeURIComponent(cityId)}`
      : `/monitoring/recommendations/${encodeURIComponent(h3Index)}`;
    const response = await apiClient.get<MonitoringRecommendation>(url);
    return response.data;
  },
};
