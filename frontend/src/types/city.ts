/**
 * Canonical F1 City DTO representing the response from
 * GET /api/v1/cities and GET /api/v1/cities/{cityId}.
 */
export interface CityResponse {
  id: string;
  name: string;
  state: string;
  country?: string;
  timezone: string;
  latitude?: number;
  longitude?: number;
  active?: boolean;
  createdAt?: string;
}
