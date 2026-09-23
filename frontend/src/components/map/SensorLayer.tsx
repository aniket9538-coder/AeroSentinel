import React from 'react';
import { CircleMarker, Popup } from 'react-leaflet';
import { AirObservation } from '../../types';

interface SensorLayerProps {
  stations?: AirObservation[];
}

export const SensorLayer: React.FC<SensorLayerProps> = ({ stations = [] }) => {
  const getAqiColor = (pm25: number) => {
    if (pm25 <= 30) return '#10b981';
    if (pm25 <= 60) return '#84cc16';
    if (pm25 <= 90) return '#f59e0b';
    if (pm25 <= 120) return '#f97316';
    if (pm25 <= 250) return '#ef4444';
    return '#881337';
  };

  return (
    <>
      {stations.map((st) => (
        <CircleMarker
          key={st.stationId}
          center={[st.latitude, st.longitude]}
          radius={8}
          pathOptions={{
            color: '#ffffff',
            weight: 2,
            fillColor: getAqiColor(st.pm25),
            fillOpacity: 0.9,
          }}
        >
          <Popup>
            <div style={{ color: '#0f172a', fontSize: '0.85rem' }}>
              <strong>{st.stationName}</strong>
              <div style={{ color: '#64748b', fontSize: '0.75rem' }}>ID: {st.stationId} ({st.source})</div>
              <div style={{ marginTop: '0.4rem', fontWeight: 600 }}>
                PM2.5: {st.pm25} µg/m³
              </div>
              {st.pm10 && <div>PM10: {st.pm10} µg/m³</div>}
              {st.no2 && <div>NO2: {st.no2} ppb</div>}
              {st.aqi && <div>AQI: {st.aqi}</div>}
              <div style={{ fontSize: '0.7rem', color: '#94a3b8', marginTop: '0.2rem' }}>
                Observed: {new Date(st.observedAt).toLocaleTimeString()}
              </div>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </>
  );
};
