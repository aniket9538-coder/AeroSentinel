import apiClient from './api';
import { AirObservation } from '../types';

export const airService = {
  getCurrentObservations: async (cityId: string): Promise<AirObservation[]> => {
    const response = await apiClient.get<AirObservation[]>(`/air/current?cityId=${cityId}`);
    return response.data;
  },
  getHistoricalObservations: async (stationId: string, hours = 24): Promise<AirObservation[]> => {
    const response = await apiClient.get<AirObservation[]>(`/air/observations?stationId=${stationId}&hours=${hours}`);
    return response.data;
  },
};
