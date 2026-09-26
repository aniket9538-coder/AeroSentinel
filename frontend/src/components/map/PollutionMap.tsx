import React, { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';
import * as h3 from 'h3-js';
import { SensorLayer } from './SensorLayer';
import { H3RiskLayer, H3CellData } from './H3RiskLayer';
import { FireLayer } from './FireLayer';
import { CitizenReportLayer } from './CitizenReportLayer';
import { AirObservation, AirQualityObservationResponse, HotspotPrediction, FireEvent, CitizenReport } from '../../types';
import { HotspotCell } from '../../types/hotspot';
import { GridCellResponse, GridCellObservationResponse } from '../../types/grid';
import { H3GridLayer } from './H3GridLayer';
import { useApp } from '../../store/AppContext';
import { Layers, Activity, Hexagon, CloudRain, Radio } from 'lucide-react';

interface PollutionMapProps {
  center?: [number, number];
  zoom?: number;
  cityId?: string;
  stations?: (AirObservation | AirQualityObservationResponse)[];
  hotspots?: (HotspotPrediction | H3CellData | HotspotCell)[];
  gridCells?: GridCellResponse[];
  selectedH3Index?: string | null;
  onSelectGridCell?: (cell: GridCellResponse) => void;
  selectedCellObservations?: GridCellObservationResponse | null;
  isLoadingObservations?: boolean;
  fires?: FireEvent[];
  citizenReports?: CitizenReport[];
  showHotspots?: boolean;
  showFires?: boolean;
  showCitizenReports?: boolean;
  height?: string;
  onSelectStation?: (station: any) => void;
  onSelectH3Cell?: (cell: H3CellData) => void;
  selectedCellId?: string;
}

// Helper to compute bounding box from H3 hotspots or grid cells
const calculateSpatialBounds = (
  hotspots?: (HotspotPrediction | H3CellData | HotspotCell)[],
  gridCells?: GridCellResponse[]
): L.LatLngBounds | null => {
  const coords: [number, number][] = [];

  if (hotspots && hotspots.length > 0) {
    for (const h of hotspots) {
      if (h && h.h3Index) {
        try {
          const boundary = h3.cellToBoundary(h.h3Index) as [number, number][];
          if (Array.isArray(boundary)) {
            for (const pt of boundary) {
              coords.push(pt);
            }
          }
        } catch {
          // ignore invalid h3
        }
      }
    }
  }

  if (gridCells && gridCells.length > 0) {
    for (const cell of gridCells) {
      if (cell.boundary && cell.boundary.length > 0) {
        for (const pt of cell.boundary) {
          coords.push([pt.lat, pt.lng]);
        }
      } else if (cell.h3Index) {
        try {
          const boundary = h3.cellToBoundary(cell.h3Index) as [number, number][];
          if (Array.isArray(boundary)) {
            for (const pt of boundary) {
              coords.push(pt);
            }
          }
        } catch {
          // ignore
        }
      }
    }
  }

  if (coords.length === 0) return null;
  const bounds = L.latLngBounds(coords);
  return bounds.isValid() ? bounds : null;
};

interface MapViewControllerProps {
  center: [number, number];
  zoom: number;
  cityId?: string;
  hotspots?: (HotspotPrediction | H3CellData | HotspotCell)[];
  gridCells?: GridCellResponse[];
  selectedCellId?: string | null;
}

// Controller to smoothly fit to spatial geometry or fly to updated coordinates when city is changed
const MapViewController: React.FC<MapViewControllerProps> = ({
  center,
  zoom,
  cityId,
  hotspots = [],
  gridCells = [],
  selectedCellId,
}) => {
  const map = useMap();
  const lastCityIdRef = useRef<string | undefined>(cityId);
  const lastCenterRef = useRef<[number, number]>(center);
  const lastGeometryFitRef = useRef<string | null>(null);

  // 1. Move to new city when center coordinates or city changes (BUG 2)
  useEffect(() => {
    const [lat, lng] = center;
    const centerChanged =
      Math.abs(lastCenterRef.current[0] - lat) > 0.0001 ||
      Math.abs(lastCenterRef.current[1] - lng) > 0.0001;
    const cityChanged = cityId !== undefined && cityId !== lastCityIdRef.current;

    if (centerChanged || cityChanged) {
      lastCenterRef.current = [lat, lng];
      lastCityIdRef.current = cityId;

      // Fit to spatial geometry immediately if already present for this city
      const bounds = calculateSpatialBounds(hotspots, gridCells);
      if (bounds) {
        map.fitBounds(bounds, { padding: [35, 35], maxZoom: 13, animate: true });
        lastGeometryFitRef.current = `${cityId || lat}_${hotspots.length}_${gridCells.length}`;
      } else {
        map.flyTo(center, zoom, { duration: 1.0 });
      }
    }
  }, [center[0], center[1], zoom, cityId, map]);

  // 2. When city spatial geometry (H3 risk layer or grid) arrives, fit to its bounds (BUG 2)
  useEffect(() => {
    const currentCellsCount = (hotspots?.length || 0) + (gridCells?.length || 0);
    if (currentCellsCount === 0) return;

    const geomKey = `${cityId || center[0]}_${hotspots?.length || 0}_${gridCells?.length || 0}`;
    if (lastGeometryFitRef.current !== geomKey) {
      const bounds = calculateSpatialBounds(hotspots, gridCells);
      if (bounds) {
        map.fitBounds(bounds, { padding: [35, 35], maxZoom: 13, animate: true });
        lastGeometryFitRef.current = geomKey;
      }
    }
  }, [hotspots, gridCells, cityId, center[0], center[1], map]);

  // 3. Ensure selected hotspot cell is visible inside the viewport (BUG 3)
  useEffect(() => {
    if (!selectedCellId) return;

    try {
      const [cellLat, cellLng] = h3.cellToLatLng(selectedCellId);
      const currentBounds = map.getBounds();
      // Only pan if outside current visible bounds to prevent awkward jumpiness
      if (currentBounds && currentBounds.isValid() && !currentBounds.contains([cellLat, cellLng])) {
        map.panTo([cellLat, cellLng], { animate: true });
      }
    } catch {
      // Ignore invalid H3 index
    }
  }, [selectedCellId, map]);

  return null;
};

export const PollutionMap: React.FC<PollutionMapProps> = ({
  center,
  zoom = 12,
  cityId,
  stations = [],
  hotspots = [],
  gridCells,
  selectedH3Index,
  onSelectGridCell,
  selectedCellObservations,
  isLoadingObservations,
  fires = [],
  citizenReports = [],
  showHotspots = true,
  showFires = false,
  showCitizenReports = false,
  height = '560px',
  onSelectStation,
  onSelectH3Cell,
  selectedCellId,
}) => {
  const { theme, selectedCity } = useApp();

  const effectiveCityId = cityId || selectedCity?.id;
  const activeSelectedId = selectedCellId || selectedH3Index || undefined;

  const activeCenter: [number, number] =
    center ||
    (selectedCity && typeof selectedCity.latitude === 'number' && typeof selectedCity.longitude === 'number'
      ? [selectedCity.latitude, selectedCity.longitude]
      : [18.5204, 73.8567]);

  // Internal layer toggles
  const [layerStations, setLayerStations] = useState(true);
  const [layerAirQuality, setLayerAirQuality] = useState(true);
  const [layerH3, setLayerH3] = useState(showHotspots);
  const [layerWeather, setLayerWeather] = useState(false);

  useEffect(() => {
    setLayerH3(showHotspots);
  }, [showHotspots]);

  const tileUrl =
    theme === 'dark'
      ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';

  return (
    <div
      style={{
        position: 'relative',
        height: height,
        width: '100%',
        borderRadius: '14px',
        overflow: 'hidden',
        border: '1px solid var(--border-subtle)',
        boxShadow: 'var(--shadow-md)',
      }}
    >
      {/* Top Floating Layer Controls */}
      <div
        style={{
          position: 'absolute',
          top: '12px',
          right: '12px',
          zIndex: 1000,
          background: 'var(--bg-surface)',
          padding: '0.35rem 0.5rem',
          borderRadius: '10px',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-md)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.35rem',
        }}
      >
        <span
          style={{
            fontSize: '0.7rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
            padding: '0 0.35rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
        >
          <Layers size={13} /> Layers:
        </span>

        <button
          onClick={() => setLayerStations(!layerStations)}
          style={{
            padding: '0.25rem 0.6rem',
            borderRadius: '6px',
            fontSize: '0.725rem',
            fontWeight: 600,
            cursor: 'pointer',
            border: layerStations
              ? '1px solid var(--brand-border)'
              : '1px solid var(--border-subtle)',
            background: layerStations ? 'var(--brand-surface)' : 'transparent',
            color: layerStations ? 'var(--brand-primary)' : 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
        >
          <Radio size={12} /> Stations
        </button>

        <button
          onClick={() => setLayerAirQuality(!layerAirQuality)}
          style={{
            padding: '0.25rem 0.6rem',
            borderRadius: '6px',
            fontSize: '0.725rem',
            fontWeight: 600,
            cursor: 'pointer',
            border: layerAirQuality
              ? '1px solid rgba(16, 185, 129, 0.4)'
              : '1px solid var(--border-subtle)',
            background: layerAirQuality ? 'rgba(16, 185, 129, 0.12)' : 'transparent',
            color: layerAirQuality ? 'var(--aqi-good)' : 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
        >
          <Activity size={12} /> Air Quality
        </button>

        <button
          onClick={() => setLayerH3(!layerH3)}
          style={{
            padding: '0.25rem 0.6rem',
            borderRadius: '6px',
            fontSize: '0.725rem',
            fontWeight: 600,
            cursor: 'pointer',
            border: layerH3
              ? '1px solid rgba(14, 165, 233, 0.4)'
              : '1px solid var(--border-subtle)',
            background: layerH3 ? 'rgba(14, 165, 233, 0.12)' : 'transparent',
            color: layerH3 ? 'var(--brand-primary)' : 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
        >
          <Hexagon size={12} /> H3 Grid
        </button>

        <button
          onClick={() => setLayerWeather(!layerWeather)}
          style={{
            padding: '0.25rem 0.6rem',
            borderRadius: '6px',
            fontSize: '0.725rem',
            fontWeight: 600,
            cursor: 'pointer',
            border: layerWeather
              ? '1px solid rgba(56, 189, 248, 0.4)'
              : '1px solid var(--border-subtle)',
            background: layerWeather ? 'rgba(56, 189, 248, 0.12)' : 'transparent',
            color: layerWeather ? 'var(--brand-primary)' : 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.3rem',
          }}
        >
          <CloudRain size={12} /> Weather
        </button>
      </div>

      {/* Map Canvas */}
      <MapContainer
        center={activeCenter}
        zoom={zoom}
        scrollWheelZoom={true}
        style={{ height: '100%', width: '100%' }}
      >
        <MapViewController
          center={activeCenter}
          zoom={zoom}
          cityId={effectiveCityId}
          hotspots={hotspots}
          gridCells={gridCells}
          selectedCellId={activeSelectedId}
        />
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a>'
          url={tileUrl}
        />

        {layerStations && (
          <SensorLayer stations={stations} onSelectStation={onSelectStation} />
        )}

        {layerH3 && (
          gridCells && gridCells.length > 0 ? (
            <H3GridLayer
              cells={gridCells}
              selectedH3Index={activeSelectedId}
              onSelectCell={onSelectGridCell}
              selectedCellObservations={selectedCellObservations}
              isLoadingObservations={isLoadingObservations}
            />
          ) : (
            <H3RiskLayer
              hotspots={hotspots}
              onSelectCell={onSelectH3Cell}
              selectedCellId={activeSelectedId}
            />
          )
        )}

        {showFires && <FireLayer fires={fires} />}
        {showCitizenReports && <CitizenReportLayer reports={citizenReports} />}
      </MapContainer>

      {/* Bottom Floating Legend Bar */}
      <div
        style={{
          position: 'absolute',
          bottom: '12px',
          left: '12px',
          right: '12px',
          zIndex: 1000,
          background: 'var(--bg-surface)',
          padding: '0.5rem 0.85rem',
          borderRadius: '10px',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-md)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '0.6rem',
          fontSize: '0.725rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#ffffff', border: '1px solid #38bdf8' }} />
            <span>CAAQMS Ground Station</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 600, color: 'var(--brand-primary)' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'rgba(14, 165, 233, 0.35)', border: '1px solid #0ea5e9' }} />
            <span>H3 Grid Cell</span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem', flexWrap: 'wrap' }}>
          <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>AQI Legend:</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--aqi-good)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Good (0–30)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--aqi-moderate)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Moderate (31–60)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--aqi-poor)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Poor (61–90)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--aqi-very-poor)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Very Poor (91–120)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--aqi-severe)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Severe (121+)</span>
          </div>
        </div>
      </div>
    </div>
  );
};
