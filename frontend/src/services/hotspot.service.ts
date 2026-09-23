import apiClient from './api';
import { HotspotPrediction } from '../types';

export const hotspotService = {
  getHotspots: async (cityId: string): Promise<HotspotPrediction[]> => {
    const response = await apiClient.get<HotspotPrediction[]>(`/hotspots?cityId=${cityId}`);
    return response.data;
  },
};
