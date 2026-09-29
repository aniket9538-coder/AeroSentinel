import { useState, useEffect, useCallback, useRef } from 'react';
import forecastApi from '../services/forecastApi';
import { ForecastResponse } from '../types/forecast';

export interface UseForecastResult {
  forecast: ForecastResponse | null;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

export const useForecast = (h3Index?: string | null): UseForecastResult => {
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const activeRequestIdRef = useRef<number>(0);
  const currentH3Ref = useRef<string | null | undefined>(h3Index);
  currentH3Ref.current = h3Index;

  const fetchForecast = useCallback(async () => {
    if (!h3Index || !h3Index.trim()) {
      setForecast(null);
      setLoading(false);
      setError(null);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    try {
      const data = await forecastApi.getForecast(h3Index.trim());

      // Guard: Discard superseded responses from previous cell requests
      if (requestId !== activeRequestIdRef.current) {
        return;
      }

      setForecast(data);
    } catch (err: any) {
      if (requestId === activeRequestIdRef.current) {
        console.error('Error fetching forecast for H3 cell:', err);
        setForecast(null);
        const msg = err?.response?.data?.message || err?.message || 'Failed to retrieve forecast data';
        setError(msg);
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [h3Index]);

  useEffect(() => {
    fetchForecast();
  }, [fetchForecast]);

  return {
    forecast,
    loading,
    error,
    refresh: fetchForecast,
  };
};

export default useForecast;
