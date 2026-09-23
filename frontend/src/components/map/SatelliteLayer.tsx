import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import * as h3 from 'h3-js';
import { SatelliteObservation } from '../../types';

interface SatelliteLayerProps {
  observations?: SatelliteObservation[];
}

export const SatelliteLayer: React.FC<SatelliteLayerProps> = ({ observations = [] }) => {
  return (
    <>
      {observations.map((obs) => {
        let boundary: [number, number][] = [];
        try {
          boundary = h3.cellToBoundary(obs.h3Index) as [number, number][];
        } catch {
          return null;
        }

        return (
          <Polygon
            key={obs.id}
            positions={boundary}
            pathOptions={{
              color: '#8b5cf6',
              fillColor: '#a78bfa',
              fillOpacity: 0.25,
              weight: 1,
            }}
          >
            <Popup>
              <div style={{ color: '#0f172a', fontSize: '0.85rem' }}>
                <strong style={{ color: '#7c3aed' }}>Sentinel-5P Tropospheric Column</strong>
                <div>H3: {obs.h3Index}</div>
                {obs.no2Value && <div>NO2: {obs.no2Value.toExponential(2)} mol/m²</div>}
                {obs.aerosolIndicator && <div>Aerosol Index: {obs.aerosolIndicator.toFixed(2)}</div>}
                <div style={{ fontSize: '0.7rem', color: '#64748b' }}>Product: {obs.sourceProduct}</div>
              </div>
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};
