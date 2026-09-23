import apiClient from './api';
import { WeatherObservation } from '../types';

export const weatherService = {
  getCurrentWeather: async (cityId: string): Promise<WeatherObservation> => {
    const response = await apiClient.get<WeatherObservation>(`/weather/current?cityId=${cityId}`);
    return response.data;
  },
};
