import React, { useEffect, useMemo, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import * as h3 from 'h3-js';
import { MapPin, RotateCcw, Crosshair, AlertCircle, Info } from 'lucide-react';
import { useApp } from '../../store/AppContext';
import { getMapTileConfig } from '../../utils/mapTileConfig';

export interface LocationPickerMapProps {
  latitude: number;
  longitude: number;
  onSelectLocation: (coords: { lat: number; lng: number }) => void;
  height?: string;
  className?: string;
  defaultZoom?: number;
}

// Custom styled Leaflet DivIcon for sharp, crisp pin marker without external asset dependency
const createPickerIcon = () => {
  return L.divIcon({
    className: 'custom-picker-pin',
    html: `
      <div style="
        width: 32px;
        height: 32px;
        position: relative;
        transform: translate(-16px, -32px);
        filter: drop-shadow(0 3px 6px rgba(0,0,0,0.3));
        cursor: pointer;
      ">
        <svg viewBox="0 0 24 24" width="32" height="32" fill="#0284c7" stroke="#ffffff" stroke-width="2">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
          <circle cx="12" cy="9" r="3" fill="#ffffff"/>
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });
};

/**
 * Click handler component using react-leaflet useMapEvents.
 * Fires onSelectLocation whenever user clicks on the map canvas.
 */
const MapClickHandler: React.FC<{
  onSelectLocation: (coords: { lat: number; lng: number }) => void;
}> = ({ onSelectLocation }) => {
  useMapEvents({
    click(e) {
      if (e && e.latlng) {
        onSelectLocation({
          lat: Number(e.latlng.lat.toFixed(6)),
          lng: Number(e.latlng.lng.toFixed(6)),
        });
      }
    },
  });
  return null;
};

/**
 * Viewport updater to pan or fly smoothly when coordinates change externally.
 */
const MapCenterController: React.FC<{
  center: [number, number];
  zoom?: number;
}> = ({ center, zoom = 14 }) => {
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
        // Fallback for detached DOM or test mock environments
      }
    }
  }, [center, zoom, map]);

  return null;
};

export const LocationPickerMap: React.FC<LocationPickerMapProps> = ({
  latitude,
  longitude,
  onSelectLocation,
  height = '280px',
  className = '',
  defaultZoom = 14,
}) => {
  const { theme, selectedCity } = useApp();
  const [mapError, setMapError] = useState<string | null>(null);

  const pickerIcon = useMemo(() => createPickerIcon(), []);

  const hasValidCoords = useMemo(() => {
    return (
      typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      !isNaN(latitude) &&
      !isNaN(longitude)
    );
  }, [latitude, longitude]);

  // Informational visual H3 calculation (explicitly NOT authoritative)
  const visualH3 = useMemo(() => {
    if (!hasValidCoords) return null;
    try {
      return h3.latLngToCell(latitude, longitude, 8);
    } catch {
      return null;
    }
  }, [hasValidCoords, latitude, longitude]);

  const tileConfig = useMemo(() => getMapTileConfig(theme), [theme]);

  const defaultCenter: [number, number] = useMemo(() => {
    if (hasValidCoords) return [latitude, longitude];
    if (selectedCity?.latitude && selectedCity?.longitude) {
      return [selectedCity.latitude, selectedCity.longitude];
    }
    return [18.5204, 73.8567]; // Pune city center default
  }, [hasValidCoords, latitude, longitude, selectedCity]);

  const handleReset = () => {
    const defaultLat = selectedCity?.latitude ?? 18.5204;
    const defaultLng = selectedCity?.longitude ?? 73.8567;
    onSelectLocation({ lat: defaultLat, lng: defaultLng });
  };

  if (mapError) {
    return (
      <div
        className={className}
        style={{
          height,
          width: '100%',
          borderRadius: '10px',
          border: '1px solid #fecaca',
          background: '#fee2e2',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '1rem',
          textAlign: 'center',
        }}
      >
        <AlertCircle size={24} color="#dc2626" style={{ marginBottom: '0.4rem' }} />
        <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#dc2626' }}>
          Interactive Map Unavailable
        </span>
        <p style={{ fontSize: '0.75rem', color: '#64748b', margin: '0.2rem 0 0.5rem' }}>
          {mapError}
        </p>
        <button
          type="button"
          onClick={() => setMapError(null)}
          style={{
            fontSize: '0.75rem',
            padding: '0.25rem 0.6rem',
            borderRadius: '4px',
            border: '1px solid #dc2626',
            background: '#ffffff',
            color: '#dc2626',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          Retry Map Canvas
        </button>
      </div>
    );
  }

  return (
    <div
      className={className}
      style={{
        borderRadius: '12px',
        overflow: 'hidden',
        border: '1px solid var(--border-subtle, #e2e8f0)',
        background: 'var(--bg-surface, #ffffff)',
        boxShadow: '0 1px 3px rgba(15, 23, 42, 0.04)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Interactive Picker Guidance Header */}
      <div
        style={{
          padding: '0.55rem 0.85rem',
          background: 'var(--bg-surface-elevated, #f8fafc)',
          borderBottom: '1px solid var(--border-subtle, #e2e8f0)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Crosshair size={14} color="#0284c7" />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0f172a' }}>
            Click anywhere on map to pin exact observation location
          </span>
        </div>

        <button
          type="button"
          onClick={handleReset}
          title="Reset to City Center"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
            background: 'transparent',
            border: '1px solid var(--border-subtle, #cbd5e1)',
            borderRadius: '5px',
            padding: '0.2rem 0.5rem',
            fontSize: '0.7rem',
            fontWeight: 600,
            color: '#475569',
            cursor: 'pointer',
          }}
        >
          <RotateCcw size={11} />
          <span>Reset Center</span>
        </button>
      </div>

      {/* Map Canvas with Click Listener */}
      <div style={{ height, width: '100%', position: 'relative' }}>
        <MapContainer
          center={defaultCenter}
          zoom={defaultZoom}
          scrollWheelZoom={true}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution={tileConfig.attribution}
            url={tileConfig.url}
            maxZoom={tileConfig.maxZoom}
          />
          <MapCenterController center={defaultCenter} zoom={defaultZoom} />
          <MapClickHandler onSelectLocation={onSelectLocation} />

          {/* Draggable/Movable Marker positioned at selected coordinates */}
          {hasValidCoords && (
            <Marker position={[latitude, longitude]} icon={pickerIcon}>
              <Popup>
                <div style={{ fontSize: '0.8rem', color: '#0f172a' }}>
                  <strong style={{ color: '#0284c7', display: 'block', marginBottom: '0.2rem' }}>
                    Selected Report Location
                  </strong>
                  <div style={{ fontFamily: 'monospace' }}>
                    Lat: {latitude.toFixed(5)}, Lng: {longitude.toFixed(5)}
                  </div>
                  {visualH3 && (
                    <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: '0.3rem' }}>
                      Visual H3 Cell: <code style={{ fontFamily: 'monospace' }}>{visualH3}</code>
                    </div>
                  )}
                  <div style={{ fontSize: '0.68rem', color: '#059669', marginTop: '0.25rem' }}>
                    &check; Click elsewhere to adjust location
                  </div>
                </div>
              </Popup>
            </Marker>
          )}
        </MapContainer>
      </div>

      {/* Selected Coordinates Status Strip */}
      <div
        style={{
          padding: '0.55rem 0.85rem',
          background: 'var(--bg-surface, #ffffff)',
          borderTop: '1px solid var(--border-subtle, #f1f5f9)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.5rem',
          fontSize: '0.75rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <MapPin size={13} color="#0284c7" />
          <span style={{ color: '#64748b' }}>Selected Coordinates:</span>
          <span
            style={{
              fontFamily: 'var(--font-mono, monospace)',
              fontWeight: 700,
              color: '#0f172a',
              background: '#f1f5f9',
              padding: '0.1rem 0.45rem',
              borderRadius: '4px',
            }}
          >
            {hasValidCoords ? `${latitude.toFixed(5)}, ${longitude.toFixed(5)}` : 'None selected'}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: '#64748b', fontSize: '0.7rem' }}>
          <Info size={12} color="#0284c7" />
          <span>
            {visualH3 ? (
              <>Visual H3: <strong style={{ color: '#0284c7', fontFamily: 'monospace' }}>{visualH3}</strong> (Server H3 authoritative)</>
            ) : (
              'Spatial cell derived on server ingestion'
            )}
          </span>
        </div>
      </div>
    </div>
  );
};
