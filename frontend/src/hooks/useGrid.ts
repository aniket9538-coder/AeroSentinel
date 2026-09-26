import { useState, useEffect, useCallback, useRef } from 'react';
import { gridApi } from '../services/gridApi';
import {
  GridCellResponse,
  GridCellObservationResponse,
} from '../types/grid';

export interface UseGridResult {
  cells: GridCellResponse[];
  isLoadingGrid: boolean;
  isLoading: boolean;
  loading: boolean;
  gridError: string | null;
  error: string | null;
  selectedH3Index: string | null;
  setSelectedH3Index: (h3Index: string | null) => void;
  selectCell: (h3Index: string | null) => void;
  selectedCell: GridCellResponse | null;
  selectedCellObservations: GridCellObservationResponse | null;
  isLoadingObservations: boolean;
  loadingObservations: boolean;
  observationsError: string | null;
  refetchGrid: () => Promise<void>;
  refetch: () => Promise<void>;
  refetchObservations: () => Promise<void>;
}

/**
 * Custom hook to manage city-level H3 spatial grid and cell observations.
 * Guarantees zero stale state display on city switching via request sequencing.
 */
export function useGrid(cityId?: string | null): UseGridResult {
  const [cells, setCells] = useState<GridCellResponse[]>([]);
  const [isLoadingGrid, setIsLoadingGrid] = useState<boolean>(false);
  const [gridError, setGridError] = useState<string | null>(null);

  const [selectedH3Index, setSelectedH3Index] = useState<string | null>(null);
  const [selectedCellObservations, setSelectedCellObservations] =
    useState<GridCellObservationResponse | null>(null);
  const [isLoadingObservations, setIsLoadingObservations] =
    useState<boolean>(false);
  const [observationsError, setObservationsError] = useState<string | null>(null);

  const gridRequestIdRef = useRef<number>(0);
  const obsRequestIdRef = useRef<number>(0);

  // 1. Fetch grid cells for current city
  const fetchGrid = useCallback(async () => {
    if (!cityId || !cityId.trim()) {
      setCells([]);
      setSelectedH3Index(null);
      setSelectedCellObservations(null);
      setIsLoadingGrid(false);
      setGridError(null);
      return;
    }

    const requestId = ++gridRequestIdRef.current;
    setIsLoadingGrid(true);
    setGridError(null);

    // Immediately clear previous city's grid and selection to prevent mixed state
    setCells([]);
    setSelectedH3Index(null);
    setSelectedCellObservations(null);

    try {
      const data = await gridApi.getCityGrid(cityId.trim());
      if (requestId === gridRequestIdRef.current) {
        setCells(Array.isArray(data) ? data : []);
        setGridError(null);

        // Auto-select first cell if available so details and chart render immediately
        if (Array.isArray(data) && data.length > 0) {
          setSelectedH3Index(data[0].h3Index);
        }
      }
    } catch (err: any) {
      if (requestId === gridRequestIdRef.current) {
        setCells([]);
        setGridError(err?.response?.data?.message || err?.message || 'Failed to load grid cells');
      }
    } finally {
      if (requestId === gridRequestIdRef.current) {
        setIsLoadingGrid(false);
      }
    }
  }, [cityId]);

  useEffect(() => {
    fetchGrid();
  }, [fetchGrid]);

  // 2. Fetch observations whenever selected H3 cell changes
  const fetchObservations = useCallback(async () => {
    if (!selectedH3Index || !selectedH3Index.trim()) {
      setSelectedCellObservations(null);
      setIsLoadingObservations(false);
      setObservationsError(null);
      return;
    }

    const requestId = ++obsRequestIdRef.current;
    setIsLoadingObservations(true);
    setObservationsError(null);

    try {
      const data = await gridApi.getCellObservations(selectedH3Index.trim());
      if (requestId === obsRequestIdRef.current) {
        setSelectedCellObservations(data);
        setObservationsError(null);
      }
    } catch (err: any) {
      if (requestId === obsRequestIdRef.current) {
        setSelectedCellObservations(null);
        setObservationsError(
          err?.response?.data?.message || err?.message || 'Failed to load cell observations'
        );
      }
    } finally {
      if (requestId === obsRequestIdRef.current) {
        setIsLoadingObservations(false);
      }
    }
  }, [selectedH3Index]);

  useEffect(() => {
    fetchObservations();
  }, [fetchObservations]);

  const selectedCell = cells.find((c) => c.h3Index === selectedH3Index) || null;

  return {
    cells,
    isLoadingGrid,
    isLoading: isLoadingGrid,
    loading: isLoadingGrid,
    gridError,
    error: gridError,
    selectedH3Index,
    setSelectedH3Index,
    selectCell: setSelectedH3Index,
    selectedCell,
    selectedCellObservations,
    isLoadingObservations,
    loadingObservations: isLoadingObservations,
    observationsError,
    refetchGrid: fetchGrid,
    refetch: fetchGrid,
    refetchObservations: fetchObservations,
  };
}

export default useGrid;
