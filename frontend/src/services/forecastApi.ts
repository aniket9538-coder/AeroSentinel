import apiClient from './api';
import { ForecastResponse, ForecastGenerateRequest } from '../types/forecast';

/**
 * Validates the raw API response structure before returning to the UI.
 * Ensures the response strictly conforms to the F4-P4 authoritative contract.
 */
export function validateForecastResponse(data: any): ForecastResponse {
  if (!data || typeof data !== 'object') {
    throw new Error('MALFORMED_RESPONSE: Forecast response must be an object');
  }

  // Handle NO_DATA status
  if (data.status === 'NO_DATA') {
    return {
      h3Index: String(data.h3Index || ''),
      cityId: String(data.cityId || ''),
      baseTimestamp: data.baseTimestamp || null,
      generatedAt: data.generatedAt || new Date().toISOString(),
      modelVersion: data.modelVersion || 'forecast_regressors_v1',
      parentPredictionId: data.parentPredictionId || null,
      featureSnapshotId: data.featureSnapshotId || null,
      status: 'NO_DATA',
      freshness: 'NO_DATA',
      forecasts: [],
      forecastConfidence: null,
    };
  }

  if (!data.h3Index) {
    throw new Error('MALFORMED_RESPONSE: Missing h3Index');
  }

  if (!Array.isArray(data.forecasts)) {
    throw new Error('MALFORMED_RESPONSE: forecasts must be an array');
  }

  // Validate each forecast item
  const validForecasts = data.forecasts.map((f: any, idx: number) => {
    if (typeof f.horizonHours !== 'number' || ![1, 3, 6].includes(f.horizonHours)) {
      throw new Error(`MALFORMED_RESPONSE: Invalid horizonHours at index ${idx}: ${f.horizonHours}`);
    }
    if (typeof f.predictedPm25 !== 'number' || isNaN(f.predictedPm25)) {
      throw new Error(`MALFORMED_RESPONSE: Non-numeric predictedPm25 at index ${idx}`);
    }
    if (typeof f.lowerBound !== 'number' || isNaN(f.lowerBound) || f.lowerBound < 0) {
      throw new Error(`MALFORMED_RESPONSE: Invalid lowerBound at index ${idx}: ${f.lowerBound}`);
    }
    if (typeof f.upperBound !== 'number' || isNaN(f.upperBound)) {
      throw new Error(`MALFORMED_RESPONSE: Invalid upperBound at index ${idx}: ${f.upperBound}`);
    }
    if (f.lowerBound > f.predictedPm25 || f.predictedPm25 > f.upperBound) {
      throw new Error(`MALFORMED_RESPONSE: Bounds order violated at index ${idx}`);
    }

    return {
      horizonHours: f.horizonHours,
      targetTime: String(f.targetTime || ''),
      predictedPm25: Number(f.predictedPm25),
      lowerBound: Number(f.lowerBound),
      upperBound: Number(f.upperBound),
      unit: String(f.unit || 'ug/m3'),
    };
  });

  return {
    h3Index: String(data.h3Index),
    cityId: String(data.cityId || ''),
    baseTimestamp: data.baseTimestamp || null,
    generatedAt: String(data.generatedAt || ''),
    modelVersion: String(data.modelVersion || 'forecast_regressors_v1'),
    parentPredictionId: data.parentPredictionId || null,
    featureSnapshotId: data.featureSnapshotId || null,
    status: (data.status as any) || 'SUCCESS',
    freshness: (data.freshness as any) || 'LIVE',
    forecasts: validForecasts,
    forecastConfidence: null, // Strictly null: UI never accepts fabricated confidence
  };
}

export const forecastApi = {
  /**
   * Retrieves the latest persisted forecast for a specific H3 spatial cell.
   * Calls GET /api/v1/forecast/{h3Index}.
   */
  getForecast: async (h3Index: string): Promise<ForecastResponse> => {
    if (!h3Index || !h3Index.trim()) {
      throw new Error('INVALID_ARGUMENT: h3Index is required');
    }

    try {
      const response = await apiClient.get<ForecastResponse>(`/forecast/${h3Index.trim()}`);
      return validateForecastResponse(response.data);
    } catch (err: any) {
      // If backend returns 404 with NO_DATA status, return controlled NO_DATA response
      if (err.response && err.response.status === 404 && err.response.data?.status === 'NO_DATA') {
        return {
          h3Index: h3Index.trim(),
          cityId: '',
          baseTimestamp: null,
          generatedAt: new Date().toISOString(),
          modelVersion: 'forecast_regressors_v1',
          parentPredictionId: null,
          featureSnapshotId: null,
          status: 'NO_DATA',
          freshness: 'NO_DATA',
          forecasts: [],
          forecastConfidence: null,
        };
      }
      throw err;
    }
  },

  /**
   * Triggers explicit backend forecast generation for an F3 parent prediction.
   * Calls POST /api/v1/forecast/generate.
   */
  generateForecast: async (request: ForecastGenerateRequest): Promise<ForecastResponse> => {
    const response = await apiClient.post<ForecastResponse>('/forecast/generate', request);
    return validateForecastResponse(response.data);
  },
};

export default forecastApi;
