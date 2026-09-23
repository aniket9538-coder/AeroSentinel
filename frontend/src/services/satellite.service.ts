import apiClient from './api';
import { SatelliteObservation } from '../types';

export const satelliteService = {
  getSatelliteObservations: async (cityId: string): Promise<SatelliteObservation[]> => {
    const response = await apiClient.get<SatelliteObservation[]>(`/satellite?cityId=${cityId}`);
    return response.data;
  },
};
