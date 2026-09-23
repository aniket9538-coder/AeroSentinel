import apiClient from './api';
import { PollutionEvent } from '../types';

export const eventService = {
  getEvents: async (cityId: string): Promise<PollutionEvent[]> => {
    const response = await apiClient.get<PollutionEvent[]>(`/events?cityId=${cityId}`);
    return response.data;
  },
  getEventDetails: async (eventId: string): Promise<PollutionEvent> => {
    const response = await apiClient.get<PollutionEvent>(`/events/${eventId}`);
    return response.data;
  },
};
