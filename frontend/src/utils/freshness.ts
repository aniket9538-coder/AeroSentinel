/**
 * Freshness status representing data recency and source availability.
 * Conforms to F1 Phase 7 requirement:
 * - LIVE: observation timestamp is within freshness threshold
 * - STALE: observation timestamp exceeds freshness threshold
 * - NO_DATA: canonical API successfully returned empty observations list
 * - SOURCE_UNAVAILABLE: upstream provider or backend source is explicitly unavailable / unreachable
 */
export type FreshnessStatus = 'LIVE' | 'STALE' | 'NO_DATA' | 'SOURCE_UNAVAILABLE';

/**
 * Default fallback freshness threshold in hours.
 *
 * NOTE: prototype configurable fallback threshold; not an official SLA.
 * The AeroSentinel codebase currently does NOT define a fixed official freshness threshold
 * in its backend configuration, environment variables, or database schema.
 * This constant (24 hours) is provided strictly as a configurable fallback default so that
 * the threshold can be reconfigured or dynamically injected without modifying UI components.
 */
export const DEFAULT_FRESHNESS_THRESHOLD_HOURS = 24;
export const DEFAULT_FRESHNESS_THRESHOLD_MS = DEFAULT_FRESHNESS_THRESHOLD_HOURS * 60 * 60 * 1000;

/**
 * Evaluates whether an error represents genuine upstream source or provider unavailability
 * (e.g. HTTP 503 Service Unavailable, 502 Bad Gateway, connection refused, or explicit provider failure)
 * rather than a generic client error (e.g. 404 Not Found, 400 Bad Request, network offline).
 */
export function isSourceUnavailableError(error?: string | null, backendStatus?: string): boolean {
  if (backendStatus === 'ERROR') {
    return true;
  }
  if (!error) {
    return false;
  }
  const lower = error.toLowerCase();
  return (
    lower.includes('source_unavailable') ||
    lower.includes('503') ||
    lower.includes('502') ||
    lower.includes('504') ||
    lower.includes('service unavailable') ||
    lower.includes('bad gateway') ||
    lower.includes('gateway timeout') ||
    lower.includes('connection refused') ||
    lower.includes('provider unavailable') ||
    lower.includes('upstream')
  );
}

export interface FreshnessCalculationParams {
  /** Real observation timestamp string from backend (e.g. ISO-8601 UTC string) */
  observedAt?: string | null;
  /** Whether the canonical API returned any observations */
  hasObservations?: boolean;
  /** Whether the upstream provider or backend service is explicitly unavailable or failed */
  isSourceUnavailable?: boolean;
  /**
   * Optional custom freshness threshold in milliseconds.
   * If omitted, falls back to DEFAULT_FRESHNESS_THRESHOLD_MS.
   */
  thresholdMs?: number;
  /**
   * Reference timestamp for deterministic calculation (defaults to Date.now()).
   * Can be a Date object or epoch milliseconds.
   */
  referenceTime?: Date | number;
}

/**
 * Pure, deterministic calculation of observation freshness status.
 *
 * Rules:
 * 1. SOURCE_UNAVAILABLE: When upstream provider / backend error semantics explicitly indicate failure.
 * 2. NO_DATA: When canonical API returned zero observations or no observation is present.
 * 3. LIVE: When observation timestamp age is within the configured freshness threshold.
 * 4. STALE: When observation timestamp age exceeds the configured freshness threshold.
 *
 * Notes:
 * - Does not use frontend refresh time or fake timestamps.
 * - Parses actual ISO-8601 UTC strings.
 * - Handles slight negative age (clock skew) gracefully as LIVE.
 */
export function calculateFreshnessStatus(params: FreshnessCalculationParams): FreshnessStatus {
  if (params.isSourceUnavailable) {
    return 'SOURCE_UNAVAILABLE';
  }

  if (params.hasObservations === false || !params.observedAt) {
    return 'NO_DATA';
  }

  const observedMs = Date.parse(params.observedAt);
  if (isNaN(observedMs)) {
    return 'NO_DATA';
  }

  const refMs = params.referenceTime !== undefined
    ? (typeof params.referenceTime === 'number' ? params.referenceTime : params.referenceTime.getTime())
    : Date.now();

  const thresholdMs = params.thresholdMs !== undefined && params.thresholdMs >= 0
    ? params.thresholdMs
    : DEFAULT_FRESHNESS_THRESHOLD_MS;

  const ageMs = refMs - observedMs;

  // If observation age is within threshold (including slight clock skew where ageMs < 0)
  if (ageMs <= thresholdMs) {
    return 'LIVE';
  }

  return 'STALE';
}

/**
 * Maps freshness status to UI Badge component variants.
 */
export function getFreshnessBadgeVariant(
  status: FreshnessStatus
): 'success' | 'warning' | 'danger' | 'neutral' {
  switch (status) {
    case 'LIVE':
      return 'success';
    case 'STALE':
      return 'warning';
    case 'SOURCE_UNAVAILABLE':
      return 'danger';
    case 'NO_DATA':
    default:
      return 'neutral';
  }
}

/**
 * Formats an ISO-8601 UTC timestamp for human display in local timezone.
 * Parses ISO timestamps correctly without treating strings as local time.
 */
export function formatObservationTimestamp(isoString?: string | null): string {
  if (!isoString) return 'N/A';
  const timestamp = Date.parse(isoString);
  if (isNaN(timestamp)) return 'N/A';
  return new Date(timestamp).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });
}

/**
 * Calculates human-readable relative age string from an ISO timestamp.
 */
export function formatObservationAge(
  isoString?: string | null,
  referenceTime: number = Date.now()
): string {
  if (!isoString) return 'N/A';
  const timestamp = Date.parse(isoString);
  if (isNaN(timestamp)) return 'N/A';

  const diffMs = referenceTime - timestamp;
  if (diffMs < 0) return 'Just now';
  const diffMins = Math.floor(diffMs / (1000 * 60));
  if (diffMins < 1) return 'Just now';
  if (diffMins === 1) return '1 min ago';
  if (diffMins < 60) return `${diffMins} min ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours === 1) return '1h ago';
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return '1d ago';
  return `${diffDays}d ago`;
}
