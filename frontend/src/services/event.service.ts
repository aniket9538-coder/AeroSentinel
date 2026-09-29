import apiClient from './api';
import { PollutionEventContext } from '../types';

export const eventService = {
  getEvents: async (params?: { cityId?: string; h3Index?: string }): Promise<PollutionEventContext[]> => {
    let url = '/events';
    const query = new URLSearchParams();
    if (params?.cityId) query.append('cityId', params.cityId);
    if (params?.h3Index) query.append('h3Index', params.h3Index);
    const qs = query.toString();
    if (qs) url += `?${qs}`;
    const response = await apiClient.get<PollutionEventContext[]>(url);
    return response.data;
  },
  getEventDetails: async (eventId: string): Promise<PollutionEventContext> => {
    const response = await apiClient.get<PollutionEventContext>(`/events/${eventId}`);
    return response.data;
  },
};
