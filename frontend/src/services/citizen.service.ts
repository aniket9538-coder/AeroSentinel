import apiClient from './api';
import { CitizenReport } from '../types';

export const citizenService = {
  getReports: async (cityId: string): Promise<CitizenReport[]> => {
    const response = await apiClient.get<CitizenReport[]>(`/citizen/reports?cityId=${cityId}`);
    return response.data;
  },
  submitReport: async (formData: FormData): Promise<CitizenReport> => {
    const response = await apiClient.post<CitizenReport>('/citizen/reports', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};
