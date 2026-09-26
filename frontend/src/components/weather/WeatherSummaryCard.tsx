import React from 'react';
import { WeatherLatestResponse } from '../../types/weather';
import { Badge } from '../common/Badge';
import {
  calculateFreshnessStatus,
  getFreshnessBadgeVariant,
  isSourceUnavailableError,
  FreshnessStatus,
} from '../../utils/freshness';
import {
  CloudSun,
  Droplets,
  Wind,
  Compass,
  CloudRain,
  Clock,
  Database,
  RefreshCw,
  AlertCircle,
  Hexagon,
} from 'lucide-react';

interface WeatherSummaryCardProps {
  weather: WeatherLatestResponse | null;
  cityName?: string;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

export const WeatherSummaryCard: React.FC<WeatherSummaryCardProps> = ({
  weather,
  cityName,
  isLoading = false,
  error = null,
  onRetry,
}) => {
  // Convert wind degrees to cardinal direction
  const getWindDirectionCardinal = (deg: number | null): string => {
    if (deg === null || deg === undefined) return '—';
    const directions = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
    const index = Math.round((deg % 360) / 22.5);
    return directions[index % 16];
  };

  const cardinal = getWindDirectionCardinal(weather?.windDirection ?? null);

  const isSourceUnavail = isSourceUnavailableError(error);

  // Calculate authoritative freshness based strictly on backend observedAt
  const freshness: FreshnessStatus = React.useMemo(() => {
    if (isSourceUnavail && !weather) {
      return 'SOURCE_UNAVAILABLE';
    }
    if (!weather || !weather.observedAt || weather.temperature === null) {
      return 'NO_DATA';
    }
    const computed = calculateFreshnessStatus({
      observedAt: weather.observedAt,
      hasObservations: true,
      isSourceUnavailable: false,
    });
    // Requirement 5: If upstream provider is unavailable, last-known data must never be labeled LIVE
    if (isSourceUnavail && computed === 'LIVE') {
      return 'STALE';
    }
    return computed;
  }, [weather, isSourceUnavail]);

  const freshnessVariant = getFreshnessBadgeVariant(freshness);

  const formatTimestamp = (iso?: string | null) => {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        timeZoneName: 'short',
      });
    } catch {
      return iso;
    }
  };

  return (
    <div
      style={{
        background: 'var(--bg-surface)',
        borderRadius: '12px',
        padding: '1.25rem',
        border: '1px solid var(--border-subtle)',
        boxShadow: 'var(--shadow-sm)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.5rem',
          paddingBottom: '0.75rem',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CloudSun size={18} color="var(--brand-primary)" />
            <h3
              style={{
                margin: 0,
                fontSize: '0.95rem',
                fontWeight: 700,
                color: 'var(--text-primary)',
                letterSpacing: '0.02em',
                textTransform: 'uppercase',
              }}
            >
              {cityName ? `${cityName} Weather` : 'Current Weather'}
            </h3>
          </div>
          <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            Physical atmospheric conditions
          </div>
        </div>

        {/* Freshness Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          {isLoading ? (
            <Badge variant="neutral">SYNCING...</Badge>
          ) : isSourceUnavail && !weather ? (
            <Badge variant="danger">SOURCE_UNAVAILABLE</Badge>
          ) : error && !weather ? (
            <Badge variant="danger">ERROR</Badge>
          ) : (
            <Badge variant={freshnessVariant}>{freshness}</Badge>
          )}
        </div>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div
          style={{
            padding: '2rem 1rem',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            color: 'var(--text-muted)',
          }}
        >
          <RefreshCw size={24} className="animate-spin" />
          <span style={{ fontSize: '0.8rem' }}>Loading physical weather...</span>
        </div>
      )}

      {/* Degraded Banner when error exists but valid last-known DB data is available */}
      {!isLoading && error && weather && weather.temperature !== null && (
        <div
          style={{
            padding: '0.65rem 0.85rem',
            borderRadius: '8px',
            background: 'rgba(245, 158, 11, 0.12)',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            color: 'var(--accent-amber)',
            fontSize: '0.775rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <span>Upstream provider sync degraded ({error}). Displaying last-known observation.</span>
          {onRetry && (
            <button
              onClick={onRetry}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--brand-primary)',
                cursor: 'pointer',
                fontWeight: 600,
                fontSize: '0.725rem',
              }}
            >
              Retry
            </button>
          )}
        </div>
      )}

      {/* Error State when no data is available */}
      {!isLoading && error && (!weather || weather.temperature === null) && (
        <div
          style={{
            padding: '1.5rem 1rem',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.6rem',
            color: 'var(--status-danger)',
            textAlign: 'center',
          }}
        >
          <AlertCircle size={28} />
          <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{error}</div>
          {onRetry && (
            <button
              onClick={onRetry}
              style={{
                padding: '0.35rem 0.85rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                borderRadius: '6px',
                background: 'var(--brand-surface)',
                color: 'var(--brand-primary)',
                border: '1px solid var(--brand-border)',
                cursor: 'pointer',
              }}
            >
              Retry
            </button>
          )}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && (!weather || weather.temperature === null) && (
        <div
          style={{
            padding: '2rem 1rem',
            textAlign: 'center',
            color: 'var(--text-muted)',
            fontSize: '0.85rem',
          }}
        >
          No weather observations available for this city.
        </div>
      )}

      {/* Real Telemetry Grid */}
      {!isLoading && weather && weather.temperature !== null && (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
              gap: '0.75rem',
            }}
          >
            {/* Temperature */}
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.85rem',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: 600,
                  marginBottom: '0.35rem',
                }}
              >
                <CloudSun size={14} color="#f59e0b" />
                <span>TEMPERATURE</span>
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {weather.temperature.toFixed(1)} <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>°C</span>
              </div>
            </div>

            {/* Humidity */}
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.85rem',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: 600,
                  marginBottom: '0.35rem',
                }}
              >
                <Droplets size={14} color="#0ea5e9" />
                <span>HUMIDITY</span>
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {weather.humidity !== null ? Math.round(weather.humidity) : '—'}{' '}
                <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>%</span>
              </div>
            </div>

            {/* Wind Speed */}
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.85rem',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: 600,
                  marginBottom: '0.35rem',
                }}
              >
                <Wind size={14} color="#10b981" />
                <span>WIND SPEED</span>
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {weather.windSpeed !== null ? weather.windSpeed.toFixed(1) : '—'}{' '}
                <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>km/h</span>
              </div>
            </div>

            {/* Wind Direction */}
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.85rem',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: 600,
                  marginBottom: '0.35rem',
                }}
              >
                <Compass size={14} color="#8b5cf6" />
                <span>DIRECTION</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem' }}>
                <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  {weather.windDirection !== null ? Math.round(weather.windDirection) : '—'}°
                </span>
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: 'var(--brand-primary)',
                    background: 'var(--brand-surface)',
                    padding: '0.1rem 0.35rem',
                    borderRadius: '4px',
                    border: '1px solid var(--brand-border)',
                  }}
                >
                  {cardinal}
                </span>
              </div>
            </div>

            {/* Rainfall */}
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.85rem',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: 600,
                  marginBottom: '0.35rem',
                }}
              >
                <CloudRain size={14} color="#06b6d4" />
                <span>RAINFALL</span>
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {weather.rainfall !== null ? weather.rainfall.toFixed(1) : '0.0'}{' '}
                <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>mm</span>
              </div>
            </div>
          </div>

          {/* Provenance Metadata Footer */}
          <div
            style={{
              paddingTop: '0.75rem',
              borderTop: '1px solid var(--border-subtle)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.5rem',
              fontSize: '0.7rem',
              color: 'var(--text-muted)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Database size={13} color="var(--brand-primary)" />
              <span>
                Source: <strong style={{ color: 'var(--text-primary)' }}>{weather.source || 'OPEN_METEO'}</strong>
              </span>
              {weather.h3Index && (
                <>
                  <span>•</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
                    <Hexagon size={12} />
                    <span style={{ fontFamily: 'monospace' }}>{weather.h3Index}</span>
                  </span>
                </>
              )}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Clock size={13} />
              <span>Observed: {formatTimestamp(weather.observedAt)}</span>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default WeatherSummaryCard;
