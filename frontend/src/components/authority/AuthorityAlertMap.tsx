import React, { useEffect, useMemo, useRef } from 'react';
import { MapContainer, TileLayer, Polygon, CircleMarker, Popup, useMap } from 'react-leaflet';
import { MapPin, AlertCircle, Compass, Layers } from 'lucide-react';
import { useApp } from '../../store/AppContext';
import { getH3BoundarySafe, getH3CenterSafe } from '../../utils/h3Spatial';

export { getH3BoundarySafe, getH3CenterSafe };

export interface AuthorityAlertMapProps {
  h3Index?: string | null;
  severity?: string;
  title?: string;
  eventCode?: string;
  latitude?: number | null;
  longitude?: number | null;
  height?: string;
  className?: string;
}

/**
 * Smoothly updates map viewport when the active H3 cell or coordinates change.
 */
const MapCenterController: React.FC<{ center: [number, number]; zoom?: number }> = ({ center, zoom = 14 }) => {
  const map = useMap();
  const prevCenterRef = useRef<[number, number] | null>(null);

  useEffect(() => {
    if (!map || !center) return;
    const [lat, lng] = center;
    if (
      !prevCenterRef.current ||
      Math.abs(prevCenterRef.current[0] - lat) > 0.0001 ||
      Math.abs(prevCenterRef.current[1] - lng) > 0.0001
    ) {
      prevCenterRef.current = [lat, lng];
      try {
        map.setView([lat, lng], zoom, { animate: true });
      } catch {
        // Fallback for detached or rendering map instances
      }
    }
  }, [center, zoom, map]);

  return null;
};

export const AuthorityAlertMap: React.FC<AuthorityAlertMapProps> = ({
  h3Index,
  severity = 'HIGH',
  title = 'Potential Pollution Event',
  eventCode,
  latitude,
  longitude,
  height = '260px',
  className = '',
}) => {
  const { theme } = useApp();

  const boundary = useMemo(() => getH3BoundarySafe(h3Index), [h3Index]);
  const center = useMemo(() => getH3CenterSafe(h3Index), [h3Index]);

  // Determine explicit point coordinates (only if genuinely provided)
  const hasExactCoordinates = useMemo(() => {
    return (
      typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      !isNaN(latitude) &&
      !isNaN(longitude) &&
      (latitude !== 0 || longitude !== 0)
    );
  }, [latitude, longitude]);

  // Color styling based on authoritative alert severity
  const severityColors = useMemo(() => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL':
        return { stroke: '#dc2626', fill: '#ef4444', bg: '#fee2e2' };
      case 'HIGH':
        return { stroke: '#ea580c', fill: '#f97316', bg: '#ffedd5' };
      case 'MEDIUM':
      case 'MODERATE':
        return { stroke: '#d97706', fill: '#f59e0b', bg: '#fef3c7' };
      case 'LOW':
        return { stroke: '#059669', fill: '#10b981', bg: '#dcfce7' };
      default:
        return { stroke: '#0284c7', fill: '#38bdf8', bg: '#e0f2fe' };
    }
  }, [severity]);

  const tileUrl =
    (typeof import.meta !== 'undefined' && (import.meta as any).env?.VITE_MAP_TILE_URL) ||
    (theme === 'dark'
      ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png');

  // Fallback state when H3 is invalid or empty
  if (!h3Index || !boundary || !center) {
    return (
      <div
        className={className}
        style={{
          height,
          width: '100%',
          borderRadius: '10px',
          border: '1px dashed #cbd5e1',
          background: '#f8fafc',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '1.5rem',
          textAlign: 'center',
          boxSizing: 'border-box',
        }}
      >
        <AlertCircle size={28} color="#94a3b8" style={{ marginBottom: '0.5rem' }} />
        <span style={{ fontSize: '0.875rem', fontWeight: 700, color: '#475569' }}>
          Spatial cell unavailable
        </span>
        <p style={{ fontSize: '0.75rem', color: '#64748b', margin: '0.25rem 0 0', maxWidth: '320px' }}>
          {h3Index
            ? `The provided spatial index "${h3Index}" is not a valid Uber H3 Resolution 8 cell.`
            : 'No spatial H3 cell was attached to this alert candidate.'}
        </p>
      </div>
    );
  }

  const effectiveCenter: [number, number] = hasExactCoordinates && latitude != null && longitude != null
    ? [latitude, longitude]
    : center;

  return (
    <div
      className={className}
      style={{
        borderRadius: '12px',
        overflow: 'hidden',
        border: '1px solid #e2e8f0',
        background: '#ffffff',
        boxShadow: '0 1px 3px rgba(15, 23, 42, 0.05)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Map Header with Spatial Metadata */}
      <div
        style={{
          padding: '0.65rem 0.85rem',
          background: '#f8fafc',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
          <Layers size={14} color="#0284c7" />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0f172a', letterSpacing: '0.02em' }}>
            Spatial Context &middot; Potential Pollution Event area
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
          <span style={{ fontSize: '0.7rem', color: '#64748b' }}>H3 Cell:</span>
          <span
            style={{
              fontSize: '0.72rem',
              fontFamily: 'var(--font-mono, monospace)',
              fontWeight: 700,
              color: '#0284c7',
              background: '#f0f9ff',
              padding: '0.1rem 0.4rem',
              borderRadius: '4px',
              border: '1px solid #bae6fd',
            }}
          >
            {h3Index}
          </span>
        </div>
      </div>

      {/* Map Viewport Canvas */}
      <div style={{ height, width: '100%', position: 'relative' }}>
        <MapContainer
          center={effectiveCenter}
          zoom={14}
          scrollWheelZoom={false}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url={tileUrl}
          />
          <MapCenterController center={effectiveCenter} zoom={14} />

          {/* Authoritative Res-8 H3 Polygon Boundary */}
          <Polygon
            positions={boundary}
            pathOptions={{
              color: severityColors.stroke,
              weight: 2.5,
              fillColor: severityColors.fill,
              fillOpacity: 0.28,
            }}
          >
            <Popup>
              <div style={{ fontSize: '0.8rem', color: '#0f172a' }}>
                <strong style={{ color: severityColors.stroke, display: 'block', marginBottom: '0.2rem' }}>
                  {title}
                </strong>
                <div>H3 Index: <code style={{ fontFamily: 'monospace' }}>{h3Index}</code></div>
                {eventCode && <div>Event Code: <code style={{ fontFamily: 'monospace' }}>{eventCode}</code></div>}
                <div style={{ fontSize: '0.72rem', color: '#64748b', marginTop: '0.25rem' }}>
                  Authoritative H3 Res-8 spatial hexagon (&approx; 460m radius)
                </div>
              </div>
            </Popup>
          </Polygon>

          {/* Genuine point coordinates marker (only rendered if verified non-null) */}
          {hasExactCoordinates && latitude != null && longitude != null && (
            <CircleMarker
              center={[latitude, longitude]}
              radius={7}
              pathOptions={{
                color: '#ffffff',
                weight: 2,
                fillColor: severityColors.stroke,
                fillOpacity: 0.95,
              }}
            >
              <Popup>
                <div style={{ fontSize: '0.8rem', color: '#0f172a' }}>
                  <strong>Reported Inspection / Event Coordinate</strong>
                  <div style={{ marginTop: '0.2rem', fontFamily: 'monospace' }}>
                    Lat: {latitude.toFixed(5)}, Lng: {longitude.toFixed(5)}
                  </div>
                </div>
              </Popup>
            </CircleMarker>
          )}
        </MapContainer>
      </div>

      {/* Spatial Attribution Footer */}
      <div
        style={{
          padding: '0.5rem 0.85rem',
          background: '#ffffff',
          borderTop: '1px solid #f1f5f9',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '0.72rem',
          color: '#64748b',
          flexWrap: 'wrap',
          gap: '0.4rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
          <Compass size={12} color="#64748b" />
          <span>
            {hasExactCoordinates && latitude != null && longitude != null
              ? `Point Coordinate: ${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
              : 'Exact point coordinates not reported — H3 cell boundary represents authoritative spatial boundary'}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
          <MapPin size={12} color={severityColors.stroke} />
          <span style={{ fontWeight: 600, color: '#334155' }}>
            Resolution 8 Hexagon
          </span>
        </div>
      </div>
    </div>
  );
};
