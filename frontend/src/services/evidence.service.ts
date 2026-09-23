import apiClient from './api';
import { EventEvidence } from '../types';

export const evidenceService = {
  getEvidenceForEvent: async (eventId: string): Promise<EventEvidence[]> => {
    const response = await apiClient.get<EventEvidence[]>(`/evidence?eventId=${eventId}`);
    return response.data;
  },
  explainHotspot: async (h3Index: string): Promise<{ summary: string; signals: string[]; recommendedAction: string }> => {
    const response = await apiClient.post('/ai/explain-hotspot', { h3Index });
    return response.data;
  },
};
