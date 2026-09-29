import forecastApi from './forecastApi';
import { ForecastResponse } from '../types/forecast';

export const forecastService = {
  getCellForecast: async (h3Index: string): Promise<ForecastResponse> => {
    return forecastApi.getForecast(h3Index);
  },
  ...forecastApi,
};

export default forecastService;
