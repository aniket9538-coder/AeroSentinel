import apiClient from './api';
import { FederatedNode } from '../types';

export const federatedService = {
  getNodes: async (): Promise<FederatedNode[]> => {
    const response = await apiClient.get<FederatedNode[]>('/federated/nodes');
    return response.data;
  },
  triggerAggregation: async (): Promise<{ status: string; round: number; globalVersion: string }> => {
    const response = await apiClient.post('/federated/aggregate');
    return response.data;
  },
};
