import apiClient from './api';
import { FireEvent } from '../types';

export const fireService = {
  getFires: async (cityId: string, hours = 24): Promise<FireEvent[]> => {
    const response = await apiClient.get<FireEvent[]>(`/fires?cityId=${cityId}&hours=${hours}`);
    return response.data;
  },
};
