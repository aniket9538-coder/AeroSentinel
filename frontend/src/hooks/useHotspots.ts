import { useState, useEffect, useCallback, useRef } from 'react';
import hotspotApi from '../services/hotspotApi';
import { HotspotOverviewResponse, HotspotCell } from '../types/hotspot';

export interface UseHotspotsResult {
  overview: HotspotOverviewResponse | null;
  loading: boolean;
  error: string | null;
  selectedH3Index: string | null;
  selectedCell: HotspotCell | null;
  loadingSelectedCell: boolean;
  refresh: () => Promise<void>;
  selectCell: (h3Index: string | null) => Promise<void>;
}

export const useHotspots = (cityId?: string): UseHotspotsResult => {
  const [overview, setOverview] = useState<HotspotOverviewResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedH3Index, setSelectedH3Index] = useState<string | null>(null);
  const [selectedCell, setSelectedCell] = useState<HotspotCell | null>(null);
  const [loadingSelectedCell, setLoadingSelectedCell] = useState<boolean>(false);

  // Request sequencing refs to prevent out-of-order race conditions
  const activeRequestIdRef = useRef<number>(0);
  const cellRequestIdRef = useRef<number>(0);
  const previousCityIdRef = useRef<string | undefined>(undefined);
  const selectedH3IndexRef = useRef<string | null>(selectedH3Index);
  selectedH3IndexRef.current = selectedH3Index;

  const fetchHotspots = useCallback(async () => {
    const isCitySwitch = cityId !== previousCityIdRef.current;
    previousCityIdRef.current = cityId;

    if (!cityId || !cityId.trim()) {
      setOverview(null);
      setSelectedH3Index(null);
      setSelectedCell(null);
      setLoading(false);
      setError(null);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    // Immediately clear previous city's data and selection on city switch (BUG 1, BUG 8)
    if (isCitySwitch) {
      setOverview(null);
      setSelectedH3Index(null);
      setSelectedCell(null);
    }

    try {
      const data = await hotspotApi.getHotspotsByCity(cityId.trim());

      // Guard: Discard superseded responses from previous city requests (BUG 4)
      if (requestId !== activeRequestIdRef.current) {
        return;
      }

      setOverview(data);

      const cells = Array.isArray(data?.cells) ? data.cells : [];

      if (cells.length > 0) {
        // If this was a refresh on the same city and the previously selected cell is still present, retain it
        let cellToSelect: HotspotCell = cells[0];
        const currentSelectedH3 = selectedH3IndexRef.current;
        if (!isCitySwitch && currentSelectedH3) {
          const matching = cells.find((c) => c.h3Index === currentSelectedH3);
          if (matching) {
            cellToSelect = matching;
          }
        }

        // BUG 6: Automatic selection rule - use backend-ranked order (cells[0])
        // BUG 7: Use already-loaded overview record without duplicate GET
        setSelectedH3Index(cellToSelect.h3Index);
        setSelectedCell(cellToSelect);
      } else {
        // Empty city clears selection (BUG 1, BUG 8)
        setSelectedH3Index(null);
        setSelectedCell(null);
      }
    } catch (err: any) {
      if (requestId === activeRequestIdRef.current) {
        console.error('Error fetching hotspots for city:', err);
        setOverview(null);
        setSelectedH3Index(null);
        setSelectedCell(null);
        setError(err?.response?.data?.message || err?.message || 'Failed to load hotspot intelligence');
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [cityId]);

  useEffect(() => {
    fetchHotspots();
  }, [cityId]);

  const selectCell = useCallback(
    async (h3Index: string | null) => {
      setSelectedH3Index(h3Index);
      if (!h3Index) {
        setSelectedCell(null);
        return;
      }

      // BUG 7: Prefer already-loaded overview record when present
      if (overview?.cells) {
        const cached = overview.cells.find((c) => c.h3Index === h3Index);
        if (cached) {
          setSelectedCell(cached);
          return;
        }
      }

      // Otherwise fetch on demand if not found in current overview
      const cellReqId = ++cellRequestIdRef.current;
      setLoadingSelectedCell(true);
      try {
        const cell = await hotspotApi.getHotspotByH3(h3Index);
        if (cellReqId === cellRequestIdRef.current) {
          setSelectedCell(cell);
        }
      } catch (err) {
        console.error('Error fetching single H3 hotspot cell:', err);
      } finally {
        if (cellReqId === cellRequestIdRef.current) {
          setLoadingSelectedCell(false);
        }
      }
    },
    [overview]
  );

  return {
    overview,
    loading,
    error,
    selectedH3Index,
    selectedCell,
    loadingSelectedCell,
    refresh: fetchHotspots,
    selectCell,
  };
};

export default useHotspots;
