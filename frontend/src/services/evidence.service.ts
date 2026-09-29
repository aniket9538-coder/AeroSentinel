import apiClient from './api';
import { EventEvidence } from '../types';
import { evidenceApi } from './evidenceApi';
import { EvidenceSummaryResponse } from '../types/evidence';

export const evidenceService = {
  getEvidenceForEvent: async (eventId: string): Promise<EventEvidence[]> => {
    const response = await apiClient.get<EventEvidence[]>(`/evidence?eventId=${eventId}`);
    return response.data;
  },

  getEvidenceByH3: async (h3Index: string): Promise<EvidenceSummaryResponse> => {
    return evidenceApi.getEvidence(h3Index);
  },

  explainHotspot: async (h3Index: string): Promise<{ summary: string; signals: string[]; recommendedAction: string }> => {
    const data = await evidenceApi.getEvidence(h3Index);
    return {
      summary: data.aiInterpretation?.summaryPublic || 'No summary available',
      signals: data.aiInterpretation?.supportingSignals || [],
      recommendedAction: data.recommendedVerification?.action || 'No action specified',
    };
  },
};

export default evidenceService;
