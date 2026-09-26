import React from 'react';
import { GridCellResponse, GridCellObservationResponse } from '../../types/grid';
import { Hexagon, Clock, Database, RefreshCw, AlertCircle, Inbox, Activity, CloudSun } from 'lucide-react';

interface CellDetailsCardProps {
  cell: GridCellResponse | null;
  observations: GridCellObservationResponse | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

export const CellDetailsCard: React.FC<CellDetailsCardProps> = ({
  cell,
  observations,
  isLoading = false,
  error = null,
  onRetry,
}) => {
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

  const latestAir =
    observations?.airObservations && observations.airObservations.length > 0
      ? observations.airObservations[observations.airObservations.length - 1]
      : null;

  const latestWeather =
    observations?.weatherObservations && observations.weatherObservations.length > 0
      ? observations.weatherObservations[observations.weatherObservations.length - 1]
      : null;

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
        gap: '0.85rem',
        height: '100%',
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
          paddingBottom: '0.65rem',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
          <Hexagon size={18} color="var(--brand-primary)" />
          <h3
            style={{
              margin: 0,
              fontSize: '0.95rem',
              fontWeight: 700,
              color: 'var(--text-primary)',
              letterSpacing: '0.01em',
            }}
          >
            SELECTED H3 CELL
          </h3>
        </div>

        {cell && (
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
            Resolution {cell.resolution}
          </span>
        )}
      </div>

      {/* No Cell Selected */}
      {!cell && (
        <div
          style={{
            padding: '2.5rem 1rem',
            textAlign: 'center',
            color: 'var(--text-muted)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.6rem',
            flex: 1,
          }}
        >
          <Inbox size={32} strokeWidth={1.5} />
          <div style={{ fontSize: '0.85rem', fontWeight: 500, maxWidth: '240px' }}>
            Select an H3 cell on the map to view local air-quality and weather observations.
          </div>
        </div>
      )}

      {/* Cell Selected */}
      {cell && (
        <>
          {/* H3 Index & Centroid */}
          <div
            style={{
              background: 'var(--bg-card)',
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Hexagon Address
            </div>
            <div
              style={{
                fontSize: '0.85rem',
                fontFamily: 'monospace',
                fontWeight: 700,
                color: 'var(--brand-primary)',
                wordBreak: 'break-all',
              }}
            >
              {cell.h3Index}
            </div>
            <div
              style={{
                fontSize: '0.7rem',
                color: 'var(--text-muted)',
                marginTop: '0.25rem',
              }}
            >
              Centroid: {cell.center.lat.toFixed(4)}, {cell.center.lng.toFixed(4)}
            </div>
          </div>

          {/* Loading Telemetry */}
          {isLoading && (
            <div
              style={{
                padding: '2rem 1rem',
                textAlign: 'center',
                color: 'var(--text-muted)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                flex: 1,
              }}
            >
              <RefreshCw size={20} className="animate-spin" />
              <span style={{ fontSize: '0.775rem' }}>Loading cell observations...</span>
            </div>
          )}

          {/* Error Loading Telemetry */}
          {!isLoading && error && (
            <div
              style={{
                padding: '1.5rem 1rem',
                textAlign: 'center',
                color: 'var(--status-danger)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <AlertCircle size={24} />
              <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>Unable to load cell observations.</div>
              {onRetry && (
                <button
                  onClick={onRetry}
                  style={{
                    padding: '0.3rem 0.75rem',
                    fontSize: '0.725rem',
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

          {/* Observations Loaded */}
          {!isLoading && !error && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', flex: 1 }}>
              {/* Air Quality Card */}
              <div
                style={{
                  background: 'var(--bg-card)',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '0.45rem',
                  }}
                >
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      color: 'var(--text-secondary)',
                      textTransform: 'uppercase',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                    }}
                  >
                    <Activity size={12} color="var(--brand-primary)" /> Local Air Quality
                  </span>
                  {observations?.airObservations && (
                    <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
                      {observations.airObservations.length} readings
                    </span>
                  )}
                </div>

                {latestAir ? (
                  <>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {latestAir.stationName || latestAir.stationId}
                      </span>
                      <span style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {latestAir.pm25}{' '}
                        <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>µg/m³</span>
                      </span>
                    </div>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '0.65rem',
                        color: 'var(--text-muted)',
                        marginTop: '0.35rem',
                      }}
                    >
                      <span>Src: {latestAir.source}</span>
                      <span>Quality: {latestAir.quality || 'VALID'}</span>
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
                  </>
                ) : (
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No air observations recorded in this cell.
                  </div>
                )}
              </div>

              {/* Weather Card */}
              <div
                style={{
                  background: 'var(--bg-card)',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '0.45rem',
                  }}
                >
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      color: 'var(--text-secondary)',
                      textTransform: 'uppercase',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                    }}
                  >
                    <CloudSun size={12} color="#f59e0b" /> Physical Weather
                  </span>
                  {observations?.weatherObservations && (
                    <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
                      {observations.weatherObservations.length} readings
                    </span>
                  )}
                </div>

                {latestWeather ? (
                  <>
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '0.4rem',
                        marginBottom: '0.35rem',
                      }}
                    >
                      <div>
                        <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Temp</div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                          {latestWeather.temperature !== null ? `${latestWeather.temperature.toFixed(1)} °C` : '—'}
                        </div>
                      </div>
                      <div>
                        <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Humidity</div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                          {latestWeather.humidity !== null ? `${Math.round(latestWeather.humidity)} %` : '—'}
                        </div>
                      </div>
                      <div>
                        <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Wind</div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                          {latestWeather.windSpeed !== null ? `${latestWeather.windSpeed.toFixed(1)} km/h` : '—'}
                        </div>
                      </div>
                      <div>
                        <div style={{ fontSize: '0.625rem', color: 'var(--text-muted)' }}>Rainfall</div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                          {latestWeather.rainfall !== null ? `${latestWeather.rainfall.toFixed(1)} mm` : '0 mm'}
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
                        paddingTop: '0.3rem',
                      }}
                    >
                      <span>Src: {latestWeather.source}</span>
                      <span>{formatTimestamp(latestWeather.observedAt)}</span>
                    </div>
                  </>
                ) : (
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No weather observations recorded in this cell.
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default CellDetailsCard;
