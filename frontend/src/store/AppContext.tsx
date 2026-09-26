import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { CityResponse } from '../types/city';
import { AirQualityObservationResponse } from '../types/airQuality';
import { WeatherObservation } from '../types';
import { WeatherLatestResponse } from '../types/weather';
import { cityApi } from '../services/cityApi';
import { airQualityApi } from '../services/airQualityApi';
import { weatherService } from '../services/weather.service';
import { healthService, HealthResponse } from '../services/health.service';
import apiClient from '../services/api';

export interface CityConfig extends CityResponse {
  defaultZoom: number;
}

interface AppContextValue {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
  selectedCity: CityConfig | null;
  setSelectedCity: (city: CityConfig) => void;
  availableCities: CityConfig[];
  stations: AirQualityObservationResponse[];
  weather: WeatherLatestResponse | WeatherObservation | null;
  isLoading: boolean;
  isCitiesLoading: boolean;
  isLive: boolean;
  isOnline: boolean;
  lastUpdated: Date;
  refreshData: () => Promise<void>;
  fetchCities: () => Promise<void>;
  sidebarOpen: boolean;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  backendStatus: 'CONNECTING' | 'CONNECTED' | 'ERROR';
  backendHealth: HealthResponse | null;
  checkBackendHealth: () => Promise<void>;
  error: string | null;
}

const AppContext = createContext<AppContextValue | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const saved = localStorage.getItem('aerosentinel_theme');
    return saved === 'light' ? 'light' : 'dark';
  });

  const [availableCities, setAvailableCities] = useState<CityConfig[]>([]);
  const [selectedCity, setSelectedCity] = useState<CityConfig | null>(null);
  const [stations, setStations] = useState<AirQualityObservationResponse[]>([]);
  const [weather, setWeather] = useState<WeatherLatestResponse | WeatherObservation | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isCitiesLoading, setIsCitiesLoading] = useState<boolean>(true);
  const [isLive, setIsLive] = useState<boolean>(false);
  const [isOnline, setIsOnline] = useState<boolean>(() => {
    return typeof navigator !== 'undefined' && typeof navigator.onLine === 'boolean'
      ? navigator.onLine
      : true;
  });
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(true);
  const [backendStatus, setBackendStatus] = useState<'CONNECTING' | 'CONNECTED' | 'ERROR'>('CONNECTING');
  const [backendHealth, setBackendHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Request race condition tracking ref
  const activeRequestIdRef = React.useRef<number>(0);

  // Listen for native online / offline events
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      checkBackendHealth();
    };

    const handleOffline = () => {
      setIsOnline(false);
      setBackendStatus('ERROR');
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Sync theme attribute to document
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('aerosentinel_theme', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  }, []);

  const toggleSidebar = useCallback(() => {
    setSidebarOpen((prev) => !prev);
  }, []);

  const checkBackendHealth = useCallback(async () => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setBackendStatus('ERROR');
      return;
    }
    try {
      const health = await healthService.getHealth();
      if (health && health.status === 'UP') {
        setBackendHealth(health);
        setBackendStatus('CONNECTED');
      } else {
        setBackendStatus('ERROR');
      }
    } catch {
      setBackendStatus('ERROR');
    }
  }, []);

  // 1. Initial Load: Fetch canonical cities from backend
  const fetchCities = useCallback(async () => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setIsCitiesLoading(false);
      setError("You're offline. Check your network connection and retry.");
      setAvailableCities([]);
      setSelectedCity(null);
      return;
    }

    setIsCitiesLoading(true);
    setError(null);
    try {
      const cities = await cityApi.getCities();
      if (cities && cities.length > 0) {
        const configs: CityConfig[] = cities.map((c) => ({
          ...c,
          defaultZoom: c.name?.toLowerCase() === 'pune' ? 12 : 11,
        }));
        setAvailableCities(configs);
        setSelectedCity((prev) => {
          if (prev && configs.some((c) => c.id === prev.id)) {
            return prev;
          }
          return configs[0];
        });
        setError(null);
      } else {
        // Genuine empty cities response
        setAvailableCities([]);
        setSelectedCity(null);
        setError(null);
      }
    } catch (err: any) {
      const isNetError = !navigator.onLine || err?.code === 'ERR_NETWORK';
      setError(
        isNetError
          ? "You're offline. Check your network connection and retry."
          : err?.message || 'Failed to load cities from backend'
      );
      setAvailableCities([]);
      setSelectedCity(null);
    } finally {
      setIsCitiesLoading(false);
    }
  }, []);

  // 2. Fetch Latest Air Quality from Canonical API for current selectedCity
  const refreshData = useCallback(async () => {
    if (!selectedCity?.id) {
      await fetchCities();
      return;
    }

    // Increment request ID to guard against out-of-order race conditions
    const requestId = ++activeRequestIdRef.current;

    setIsLoading(true);
    setError(null);

    // Immediately clear current stations on new refresh/city switch to prevent stale data display
    setStations([]);

    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setIsLoading(false);
      setIsLive(false);
      setError("You're offline. Check your connection and retry.");
      return;
    }

    checkBackendHealth();

    try {
      // Parallel fetch for canonical latest air quality, station coordinates, and weather
      const [latestAirResult, sensorsResult, weatherResult] = await Promise.allSettled([
        airQualityApi.getLatestAirQuality(selectedCity.id),
        apiClient.get<any[]>(`/sensors?cityId=${encodeURIComponent(selectedCity.id)}`),
        weatherService.getCurrentWeather(selectedCity.id),
      ]);

      // If a newer request was dispatched while this one was in flight, discard this response
      if (requestId !== activeRequestIdRef.current) {
        return;
      }

      if (latestAirResult.status === 'fulfilled' && latestAirResult.value) {
        const airData = latestAirResult.value;
        const observations = airData.observations || [];

        // Enrich observations with verified station coordinates from sensor metadata if available
        let sensorCoordsMap = new Map<string, { lat: number; lng: number }>();
        if (sensorsResult.status === 'fulfilled' && Array.isArray(sensorsResult.value.data)) {
          sensorsResult.value.data.forEach((s: any) => {
            if (s.stationCode && typeof s.latitude === 'number' && typeof s.longitude === 'number') {
              sensorCoordsMap.set(s.stationCode, { lat: s.latitude, lng: s.longitude });
            }
          });
        }

        const enriched = observations.map((obs) => {
          const coords = sensorCoordsMap.get(obs.stationId);
          return {
            ...obs,
            latitude: coords ? coords.lat : selectedCity.latitude,
            longitude: coords ? coords.lng : selectedCity.longitude,
          };
        });

        setStations(enriched);
        setIsLive(enriched.length > 0);
        setError(null);
      } else {
        // Real empty vs real error state - NO fake fallback
        const isNetErr = !navigator.onLine;
        const failureReason = isNetErr
          ? "You're offline. Check your network connection and retry."
          : latestAirResult.status === 'rejected'
          ? latestAirResult.reason?.message || 'Error querying latest air quality'
          : null;

        if (failureReason) {
          setError(failureReason);
        } else {
          setError(null);
        }
        setStations([]);
        setIsLive(false);
      }

      if (weatherResult.status === 'fulfilled' && weatherResult.value) {
        setWeather(weatherResult.value);
      } else {
        setWeather(null);
      }

      setLastUpdated(new Date());
    } catch (err: any) {
      if (requestId !== activeRequestIdRef.current) {
        return;
      }
      const isNetErr = !navigator.onLine || err?.code === 'ERR_NETWORK';
      setError(
        isNetErr
          ? "You're offline. Check your connection and retry."
          : err?.message || 'Failed to fetch environmental telemetry'
      );
      setStations([]);
      setIsLive(false);
      setLastUpdated(new Date());
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setIsLoading(false);
      }
    }
  }, [selectedCity, checkBackendHealth, fetchCities]);

  // Initial mount: load cities & start health check interval
  useEffect(() => {
    fetchCities();
    checkBackendHealth();
    const interval = setInterval(() => {
      if (navigator.onLine) {
        checkBackendHealth();
      }
    }, 15000);
    return () => clearInterval(interval);
  }, [fetchCities, checkBackendHealth]);

  // When selectedCity updates, refresh latest air quality
  useEffect(() => {
    if (selectedCity?.id) {
      refreshData();
    }
  }, [selectedCity, refreshData]);

  const value: AppContextValue = {
    theme,
    toggleTheme,
    selectedCity,
    setSelectedCity,
    availableCities,
    stations,
    weather,
    isLoading,
    isCitiesLoading,
    isLive,
    isOnline,
    lastUpdated,
    refreshData,
    fetchCities,
    sidebarOpen,
    setSidebarOpen,
    toggleSidebar,
    backendStatus,
    backendHealth,
    checkBackendHealth,
    error,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useApp = (): AppContextValue => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
