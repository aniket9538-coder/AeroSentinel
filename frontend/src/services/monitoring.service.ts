import apiClient from './api';
import { MonitoringRecommendation } from '../types';

export const monitoringService = {
  getRecommendations: async (cityId: string): Promise<MonitoringRecommendation[]> => {
    const response = await apiClient.get<MonitoringRecommendation[]>(`/monitoring/recommendations?cityId=${cityId}`);
    return response.data;
  },
};
