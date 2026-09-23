import React from 'react';
import { CircleMarker, Popup } from 'react-leaflet';
import { FireEvent } from '../../types';

interface FireLayerProps {
  fires?: FireEvent[];
}

export const FireLayer: React.FC<FireLayerProps> = ({ fires = [] }) => {
  return (
    <>
      {fires.map((fire) => (
        <CircleMarker
          key={fire.id}
          center={[fire.latitude, fire.longitude]}
          radius={6}
          pathOptions={{
            color: '#f97316',
            fillColor: '#ea580c',
            fillOpacity: 0.8,
            weight: 1.5,
          }}
        >
          <Popup>
            <div style={{ color: '#0f172a', fontSize: '0.85rem' }}>
              <strong style={{ color: '#ea580c' }}>Active Thermal Anomaly (NASA FIRMS)</strong>
              <div>Confidence: {fire.confidence}%</div>
              {fire.frp && <div>Fire Radiative Power: {fire.frp} MW</div>}
              <div>Satellite: {fire.satellite}</div>
              <div style={{ fontSize: '0.7rem', color: '#64748b' }}>
                Detected: {new Date(fire.detectedAt).toLocaleTimeString()}
              </div>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </>
  );
};
