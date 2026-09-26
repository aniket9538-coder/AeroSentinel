import React from 'react';
import { GridCellResponse, GridCellObservationResponse } from '../../types/grid';
import { Hexagon, Wind, Droplets, CloudRain, Clock, Database, CheckCircle2 } from 'lucide-react';

interface H3CellPopupProps {
  cell: GridCellResponse;
  observations?: GridCellObservationResponse | null;
  isLoadingObservations?: boolean;
}

export const H3CellPopup: React.FC<H3CellPopupProps> = ({
  cell,
  observations,
  isLoadingObservations = false,
}) => {
  // Latest observation is the last item because they are ordered chronologically ascending
  const latestAir =
    observations?.airObservations && observations.airObservations.length > 0
      ? observations.airObservations[observations.airObservations.length - 1]
      : null;

  const latestWeather =
    observations?.weatherObservations && observations.weatherObservations.length > 0
      ? observations.weatherObservations[observations.weatherObservations.length - 1]
      : null;

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
        padding: '0.75rem 0.85rem',
        minWidth: '260px',
        maxWidth: '320px',
        fontFamily: 'var(--font-body)',
        color: 'var(--text-primary)',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '0.65rem',
          paddingBottom: '0.45rem',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Hexagon size={16} color="var(--brand-primary)" />
          <strong style={{ fontSize: '0.85rem', letterSpacing: '0.03em' }}>
            H3 SPATIAL CELL
          </strong>
        </div>
        <span
          style={{
            fontSize: '0.675rem',
            fontWeight: 700,
            padding: '0.15rem 0.45rem',
            borderRadius: '4px',
            background: 'var(--brand-surface)',
            color: 'var(--brand-primary)',
            border: '1px solid var(--brand-border)',
          }}
        >
          Res {cell.resolution}
        </span>
      </div>

      {/* Cell Index */}
      <div style={{ marginBottom: '0.65rem' }}>
        <div style={{ fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
          H3 Index
        </div>
        <div
          style={{
            fontSize: '0.75rem',
            fontFamily: 'monospace',
            fontWeight: 600,
            color: 'var(--brand-primary)',
            wordBreak: 'break-all',
          }}
        >
          {cell.h3Index}
        </div>
      </div>

      {/* Loading state */}
      {isLoadingObservations && (
        <div
          style={{
            padding: '0.75rem 0',
            textAlign: 'center',
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
          }}
        >
          Loading telemetry...
        </div>
      )}

      {/* Air Quality Telemetry */}
      {!isLoadingObservations && (
        <div style={{ marginBottom: '0.65rem' }}>
          <div
            style={{
              fontSize: '0.675rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              color: 'var(--text-secondary)',
              marginBottom: '0.35rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.3rem',
            }}
          >
            <span>Air Quality</span>
          </div>

          {latestAir ? (
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.45rem 0.6rem',
                borderRadius: '6px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>PM2.5</span>
                <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {latestAir.pm25} µg/m³
                </span>
              </div>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  fontSize: '0.65rem',
                  color: 'var(--text-muted)',
                  marginTop: '0.25rem',
                }}
              >
                <span>Src: {latestAir.source}</span>
                <span>{latestAir.quality || 'VALID'}</span>
              </div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.25rem',
                  fontSize: '0.625rem',
                  color: 'var(--text-muted)',
                  marginTop: '0.25rem',
                }}
              >
                <Clock size={10} />
                <span>{formatTimestamp(latestAir.observedAt)}</span>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
              No air observations recorded in this cell.
            </div>
          )}
        </div>
      )}

      {/* Weather Telemetry */}
      {!isLoadingObservations && (
        <div>
          <div
            style={{
              fontSize: '0.675rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              color: 'var(--text-secondary)',
              marginBottom: '0.35rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.3rem',
            }}
          >
            <span>Physical Weather</span>
          </div>

          {latestWeather ? (
            <div
              style={{
                background: 'var(--bg-card)',
                padding: '0.45rem 0.6rem',
                borderRadius: '6px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '0.35rem',
                  marginBottom: '0.35rem',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Temp</div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>
                    {latestWeather.temperature !== null ? `${latestWeather.temperature} °C` : '—'}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Humidity</div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>
                    {latestWeather.humidity !== null ? `${latestWeather.humidity} %` : '—'}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Wind</div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>
                    {latestWeather.windSpeed !== null ? `${latestWeather.windSpeed} km/h` : '—'}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Rainfall</div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>
                    {latestWeather.rainfall !== null ? `${latestWeather.rainfall} mm` : '0 mm'}
                  </div>
                </div>
              </div>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  fontSize: '0.625rem',
                  color: 'var(--text-muted)',
                  borderTop: '1px solid var(--border-subtle)',
                  paddingTop: '0.25rem',
                }}
              >
                <span>Src: {latestWeather.source}</span>
                <span>{formatTimestamp(latestWeather.observedAt)}</span>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
              No weather observations recorded in this cell.
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default H3CellPopup;
