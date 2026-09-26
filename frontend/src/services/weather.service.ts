import apiClient from './api';
import { WeatherObservation } from '../types';

export const weatherService = {
  getCurrentWeather: async (cityId: string): Promise<any> => {
    const response = await apiClient.get<any>(`/cities/${encodeURIComponent(cityId)}/weather/latest`);
    return response.data;
  },
};
