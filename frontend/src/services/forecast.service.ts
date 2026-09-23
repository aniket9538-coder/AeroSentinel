import apiClient from './api';
import { CellForecast } from '../types';

export const forecastService = {
  getCellForecast: async (h3Index: string): Promise<CellForecast> => {
    const response = await apiClient.get<CellForecast>(`/forecast/${h3Index}`);
    return response.data;
  },
};
