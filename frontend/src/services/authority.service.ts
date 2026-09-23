import apiClient from './api';
import { AuthorityAction, Inspection } from '../types';

export const authorityService = {
  createInspection: async (data: Partial<Inspection>): Promise<Inspection> => {
    const response = await apiClient.post<Inspection>('/inspections', data);
    return response.data;
  },
  logAction: async (data: Partial<AuthorityAction>): Promise<AuthorityAction> => {
    const response = await apiClient.post<AuthorityAction>('/actions', data);
    return response.data;
  },
};
