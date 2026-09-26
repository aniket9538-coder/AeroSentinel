import { useState, useEffect, useCallback, useRef } from 'react';
import { weatherApi } from '../services/weatherApi';
import { WeatherLatestResponse } from '../types/weather';

interface UseWeatherResult {
  weather: WeatherLatestResponse | null;
  isLoading: boolean;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

/**
 * Custom hook to fetch and manage latest weather for a selected city.
 * Guarantees zero stale state display on city switching via request sequencing.
 */
export function useWeather(cityId?: string | null): UseWeatherResult {
  const [weather, setWeather] = useState<WeatherLatestResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const activeRequestIdRef = useRef<number>(0);

  const fetchWeather = useCallback(async () => {
    if (!cityId || !cityId.trim()) {
      setWeather(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    setIsLoading(true);
    setError(null);

    // Immediately clear previous city's weather data to avoid mixed state
    setWeather(null);

    try {
      const data = await weatherApi.getLatestWeather(cityId.trim());
      if (requestId === activeRequestIdRef.current) {
        setWeather(data);
        setError(null);
      }
    } catch (err: any) {
      if (requestId === activeRequestIdRef.current) {
        setWeather(null);
        setError(err?.response?.data?.message || err?.message || 'Failed to load weather data');
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setIsLoading(false);
      }
    }
  }, [cityId]);

  useEffect(() => {
    fetchWeather();
  }, [fetchWeather]);

  return {
    weather,
    isLoading,
    loading: isLoading,
    error,
    refetch: fetchWeather,
  };
}

export default useWeather;
