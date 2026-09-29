import { useState, useEffect, useCallback, useRef } from 'react';
import evidenceApi from '../services/evidenceApi';
import { EvidenceSummaryResponse } from '../types/evidence';

export interface UseEvidenceResult {
  evidence: EvidenceSummaryResponse | null;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

/**
 * Authoritative React hook for fetching and managing F5 Evidence + WHY diagnostics.
 *
 * Enforces:
 * 1. Rapid H3 selection / stale response protection (superseded requests discarded).
 * 2. AbortController cancellation for in-flight requests.
 * 3. Clearing stale cell data immediately upon cell switch.
 * 4. Graceful handling of 404 (No active intelligence) and 400 (Bad H3 format).
 */
export const useEvidence = (h3Index?: string | null): UseEvidenceResult => {
  const [evidence, setEvidence] = useState<EvidenceSummaryResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const activeRequestIdRef = useRef<number>(0);
  const abortControllerRef = useRef<AbortController | null>(null);
  const currentH3Ref = useRef<string | null | undefined>(h3Index);
  currentH3Ref.current = h3Index;

  const fetchEvidence = useCallback(async () => {
    // Abort previous in-flight request if still pending
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    if (!h3Index || !h3Index.trim()) {
      setEvidence(null);
      setLoading(false);
      setError(null);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // Immediately clear stale evidence data from previous cell
    setEvidence(null);
    setLoading(true);
    setError(null);

    try {
      const data = await evidenceApi.getEvidence(h3Index.trim(), controller.signal);

      // Guard: Discard superseded responses from rapid cell switches
      if (requestId !== activeRequestIdRef.current) {
        return;
      }

      setEvidence(data);
    } catch (err: any) {
      // Ignore errors from intentional aborts
      if (err?.name === 'CanceledError' || err?.name === 'AbortError' || err?.code === 'ERR_CANCELED') {
        return;
      }

      if (requestId === activeRequestIdRef.current) {
        console.error('Error fetching evidence for H3 cell:', err);
        setEvidence(null);
        const msg = err?.response?.data?.message || err?.message || 'Failed to retrieve evidence data';
        setError(msg);
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [h3Index]);

  useEffect(() => {
    fetchEvidence();

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchEvidence]);

  return {
    evidence,
    loading,
    error,
    refresh: fetchEvidence,
  };
};

export default useEvidence;
