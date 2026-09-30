import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { monitoringService } from '../services/monitoring.service';
import {
  MonitoringRecommendation,
  MonitoringPriorityLevel,
  MonitoringSummaryMetrics,
} from '../types/monitoring';

export type PriorityFilterOption = 'ALL' | MonitoringPriorityLevel;

export interface UseMonitoringRecommendationsResult {
  recommendations: MonitoringRecommendation[];
  filteredRecommendations: MonitoringRecommendation[];
  loading: boolean;
  error: string | null;
  priorityFilter: PriorityFilterOption;
  setPriorityFilter: (filter: PriorityFilterOption) => void;
  selectedH3Index: string | null;
  selectedRecommendation: MonitoringRecommendation | null;
  selectH3: (h3Index: string | null) => void;
  refresh: () => Promise<void>;
  summaryMetrics: MonitoringSummaryMetrics;
}

export const useMonitoringRecommendations = (cityId?: string): UseMonitoringRecommendationsResult => {
  const [recommendations, setRecommendations] = useState<MonitoringRecommendation[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [priorityFilter, setPriorityFilter] = useState<PriorityFilterOption>('ALL');
  const [selectedH3Index, setSelectedH3Index] = useState<string | null>(null);

  const activeRequestIdRef = useRef<number>(0);
  const previousCityIdRef = useRef<string | undefined>(undefined);
  const selectedH3IndexRef = useRef<string | null>(selectedH3Index);
  selectedH3IndexRef.current = selectedH3Index;

  const fetchRecommendations = useCallback(async () => {
    const isCitySwitch = cityId !== previousCityIdRef.current;
    previousCityIdRef.current = cityId;

    if (!cityId || !cityId.trim()) {
      setRecommendations([]);
      setSelectedH3Index(null);
      setLoading(false);
      setError(null);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    if (isCitySwitch) {
      setRecommendations([]);
      setSelectedH3Index(null);
    }

    try {
      const data = await monitoringService.getRecommendations(cityId.trim());

      // Discard superseded responses from previous city requests
      if (requestId !== activeRequestIdRef.current) {
        return;
      }

      const recs = Array.isArray(data) ? data : [];
      setRecommendations(recs);

      if (recs.length > 0) {
        let recToSelect: MonitoringRecommendation = recs[0];
        const currentSelectedH3 = selectedH3IndexRef.current;
        if (!isCitySwitch && currentSelectedH3) {
          const matching = recs.find((r) => r.h3Index === currentSelectedH3);
          if (matching) {
            recToSelect = matching;
          }
        }
        setSelectedH3Index(recToSelect.h3Index);
      } else {
        setSelectedH3Index(null);
      }
      setError(null);
    } catch (err: any) {
      if (requestId !== activeRequestIdRef.current) {
        return;
      }
      const safeMessage =
        err?.response?.data?.message ||
        err?.message ||
        'Monitoring recommendations are currently unavailable.';
      setError(safeMessage);
      setRecommendations([]);
      setSelectedH3Index(null);
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [cityId]);

  useEffect(() => {
    fetchRecommendations();
  }, [fetchRecommendations]);

  const selectH3 = useCallback((h3Index: string | null) => {
    setSelectedH3Index(h3Index);
  }, []);

  const selectedRecommendation = useMemo(() => {
    if (!selectedH3Index || recommendations.length === 0) return null;
    return recommendations.find((r) => r.h3Index === selectedH3Index) || null;
  }, [selectedH3Index, recommendations]);

  const filteredRecommendations = useMemo(() => {
    if (priorityFilter === 'ALL') {
      return recommendations;
    }
    return recommendations.filter((r) => r.priorityLevel === priorityFilter);
  }, [recommendations, priorityFilter]);

  const summaryMetrics = useMemo<MonitoringSummaryMetrics>(() => {
    let high = 0;
    let medium = 0;
    let low = 0;
    let coverageGaps = 0;
    let maxDist = 0;
    let distSum = 0;
    let distCount = 0;

    for (const r of recommendations) {
      if (r.priorityLevel === 'HIGH') high++;
      else if (r.priorityLevel === 'MEDIUM') medium++;
      else if (r.priorityLevel === 'LOW') low++;

      if (r.monitoringCoverageGapFlag === 1) coverageGaps++;

      const dist = r.nearestStationDistanceKm ?? r.stationDistanceKm;
      if (dist != null && !isNaN(dist)) {
        if (dist > maxDist) maxDist = dist;
        distSum += dist;
        distCount++;
      }
    }

    return {
      totalCells: recommendations.length,
      highPriorityCount: high,
      mediumPriorityCount: medium,
      lowPriorityCount: low,
      coverageGapsCount: coverageGaps,
      maxDistanceKm: Math.round(maxDist * 100) / 100,
      avgDistanceKm: distCount > 0 ? Math.round((distSum / distCount) * 100) / 100 : 0,
    };
  }, [recommendations]);

  return {
    recommendations,
    filteredRecommendations,
    loading,
    error,
    priorityFilter,
    setPriorityFilter,
    selectedH3Index,
    selectedRecommendation,
    selectH3,
    refresh: fetchRecommendations,
    summaryMetrics,
  };
};
