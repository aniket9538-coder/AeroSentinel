import React from 'react';
import { CircleMarker, Popup } from 'react-leaflet';
import { AirObservation, AirQualityObservationResponse } from '../../types';

interface SensorLayerProps {
  stations?: (AirObservation | AirQualityObservationResponse)[];
  onSelectStation?: (station: any) => void;
}

export const SensorLayer: React.FC<SensorLayerProps> = ({ stations = [], onSelectStation }) => {
  const getAqiColor = (pm25: number) => {
    if (pm25 <= 30) return '#10b981'; // Good
    if (pm25 <= 60) return '#f59e0b'; // Moderate
    if (pm25 <= 90) return '#f97316'; // Poor
    if (pm25 <= 120) return '#ef4444'; // Very Poor
    return '#881337'; // Severe
  };

  const getAqiLabel = (pm25: number) => {
    if (pm25 <= 30) return 'GOOD';
    if (pm25 <= 60) return 'MODERATE';
    if (pm25 <= 90) return 'POOR';
    if (pm25 <= 120) return 'VERY POOR';
    return 'SEVERE';
  };

  const formatRelativeTime = (isoString: string) => {
    try {
      const diffMs = Date.now() - new Date(isoString).getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      if (diffMins < 1) return 'Just now';
      if (diffMins === 1) return '1 min ago';
      if (diffMins < 60) return `${diffMins} min ago`;
      const diffHours = Math.floor(diffMins / 60);
      return `${diffHours}h ago`;
    } catch {
      return 'Recently';
    }
  };

  return (
    <>
      {stations.map((st) => {
        if (
          typeof st.latitude !== 'number' ||
          typeof st.longitude !== 'number' ||
          isNaN(st.latitude) ||
          isNaN(st.longitude)
        ) {
          return null;
        }

        const color = getAqiColor(st.pm25);
        const aqiLabel = getAqiLabel(st.pm25);
        const timeAgo = formatRelativeTime(st.observedAt);

        return (
          <CircleMarker
            key={st.stationId}
            center={[st.latitude, st.longitude]}
            radius={9}
            pathOptions={{
              color: '#ffffff',
              weight: 2,
              fillColor: color,
              fillOpacity: 0.95,
            }}
            eventHandlers={{
              click: () => onSelectStation?.(st),
            }}
          >
            <Popup>
              <div
                style={{
                  padding: '0.85rem 1rem',
                  minWidth: '220px',
                  fontFamily: 'var(--font-body)',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '0.5rem',
                    borderBottom: '1px solid var(--border-subtle)',
                    paddingBottom: '0.35rem',
                  }}
                >
                  <strong style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                    {st.stationName}
                  </strong>
                  <span
                    style={{
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      padding: '0.15rem 0.4rem',
                      borderRadius: '4px',
                      background: `${color}20`,
                      color: color,
                      border: `1px solid ${color}40`,
                    }}
                  >
                    {aqiLabel}
                  </span>
                </div>

                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr',
                    columnGap: '1rem',
                    rowGap: '0.3rem',
                    fontSize: '0.8rem',
                  }}
                >
                  <span style={{ color: 'var(--text-muted)' }}>PM2.5</span>
                  <strong style={{ color: color, textAlign: 'right' }}>
                    {st.pm25} µg/m³
                  </strong>

                  {'quality' in st && (
                    <>
                      <span style={{ color: 'var(--text-muted)' }}>Quality</span>
                      <strong style={{ textAlign: 'right', color: 'var(--text-primary)' }}>
                        {(st as any).quality}
                      </strong>
                    </>
                  )}

                  <span style={{ color: 'var(--text-muted)' }}>Updated</span>
                  <span style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                    {timeAgo}
                  </span>

                  <span style={{ color: 'var(--text-muted)' }}>Source</span>
                  <span style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                    {st.source || 'N/A'}
                  </span>
                </div>
              </div>
            </Popup>
          </CircleMarker>
        );
      })}
    </>
  );
};
