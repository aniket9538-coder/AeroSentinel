import apiClient from './api';

export interface HealthResponse {
  status: 'UP' | 'DOWN';
  service: string;
  timestamp: string;
}

export const healthService = {
  getHealth: async (): Promise<HealthResponse> => {
    const response = await apiClient.get<HealthResponse>('/health');
    return response.data;
  },
};

export default healthService;
