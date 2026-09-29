import apiClient from './api';
import { AuthorityQueueItem } from '../types/alert';

export const alertApi = {
  /**
   * Fetches the municipal Authority Queue containing evidence-backed alert candidates.
   */
  getAuthorityQueue: async (params?: { cityId?: string; status?: string }): Promise<AuthorityQueueItem[]> => {
    const query = new URLSearchParams();
    if (params?.cityId) query.append('cityId', params.cityId);
    if (params?.status) query.append('status', params.status);

    const qs = query.toString() ? `?${query.toString()}` : '';
    const response = await apiClient.get<AuthorityQueueItem[]>(`/alerts/authority${qs}`);
    return response.data;
  },

  /**
   * Retrieves a single authoritative Alert candidate by UUID.
   */
  getAlertById: async (alertId: string): Promise<AuthorityQueueItem> => {
    if (!alertId || !alertId.trim()) {
      throw new Error('INVALID_INPUT: Alert ID cannot be empty');
    }
    const response = await apiClient.get<AuthorityQueueItem>(`/alerts/${encodeURIComponent(alertId)}`);
    return response.data;
  },

  /**
   * Acknowledges an alert candidate, transitioning status from OPEN to ACKNOWLEDGED.
   */
  acknowledgeAlert: async (alertId: string, userId?: string): Promise<AuthorityQueueItem> => {
    if (!alertId || !alertId.trim()) {
      throw new Error('INVALID_INPUT: Alert ID cannot be empty');
    }
    const query = userId ? `?userId=${encodeURIComponent(userId)}` : '';
    const response = await apiClient.patch<AuthorityQueueItem>(`/alerts/${encodeURIComponent(alertId)}/acknowledge${query}`);
    return response.data;
  },

  /**
   * Resolves an alert candidate, transitioning status to RESOLVED.
   */
  resolveAlert: async (alertId: string, notes?: string, userId?: string): Promise<AuthorityQueueItem> => {
    if (!alertId || !alertId.trim()) {
      throw new Error('INVALID_INPUT: Alert ID cannot be empty');
    }
    const query = userId ? `?userId=${encodeURIComponent(userId)}` : '';
    const body = notes ? { resolutionNotes: notes.trim(), resolvedBy: userId } : undefined;
    const response = await apiClient.patch<AuthorityQueueItem>(
      `/alerts/${encodeURIComponent(alertId)}/resolve${query}`,
      body
    );
    return response.data;
  },

  /**
   * Dismisses an alert candidate, transitioning status to DISMISSED.
   */
  dismissAlert: async (alertId: string, reason: string, userId?: string): Promise<AuthorityQueueItem> => {
    if (!alertId || !alertId.trim()) {
      throw new Error('INVALID_INPUT: Alert ID cannot be empty');
    }
    if (!reason || !reason.trim()) {
      throw new Error('INVALID_INPUT: Dismissal reason is required');
    }
    const query = userId ? `?userId=${encodeURIComponent(userId)}` : '';
    const body = { dismissalReason: reason.trim(), dismissedBy: userId };
    const response = await apiClient.patch<AuthorityQueueItem>(
      `/alerts/${encodeURIComponent(alertId)}/dismiss${query}`,
      body
    );
    return response.data;
  },
};

export default alertApi;
