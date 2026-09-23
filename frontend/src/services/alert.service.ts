import apiClient from './api';
import { Alert } from '../types';

export const alertService = {
  getAlerts: async (cityId: string, status = 'OPEN'): Promise<Alert[]> => {
    const response = await apiClient.get<Alert[]>(`/alerts?cityId=${cityId}&status=${status}`);
    return response.data;
  },
  acknowledgeAlert: async (alertId: string): Promise<Alert> => {
    const response = await apiClient.patch<Alert>(`/alerts/${alertId}/acknowledge`);
    return response.data;
  },
};
