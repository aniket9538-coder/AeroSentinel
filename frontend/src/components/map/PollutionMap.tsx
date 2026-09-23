import React from 'react';
import { MapContainer, TileLayer } from 'react-leaflet';
import { SensorLayer } from './SensorLayer';
import { H3RiskLayer } from './H3RiskLayer';
import { FireLayer } from './FireLayer';
import { CitizenReportLayer } from './CitizenReportLayer';
import { AirObservation, HotspotPrediction, FireEvent, CitizenReport } from '../../types';

interface PollutionMapProps {
  center?: [number, number];
  zoom?: number;
  stations?: AirObservation[];
  hotspots?: HotspotPrediction[];
  fires?: FireEvent[];
  citizenReports?: CitizenReport[];
  showHotspots?: boolean;
  showFires?: boolean;
  showCitizenReports?: boolean;
}

export const PollutionMap: React.FC<PollutionMapProps> = ({
  center = [18.5204, 73.8567], // Default to Pune
  zoom = 12,
  stations = [],
  hotspots = [],
  fires = [],
  citizenReports = [],
  showHotspots = true,
  showFires = true,
  showCitizenReports = true,
}) => {
  return (
    <div style={{ height: '100%', minHeight: '550px', width: '100%', borderRadius: '12px', overflow: 'hidden', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
      <MapContainer
        center={center}
        zoom={zoom}
        scrollWheelZoom={true}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
        <SensorLayer stations={stations} />
        {showHotspots && <H3RiskLayer hotspots={hotspots} />}
        {showFires && <FireLayer fires={fires} />}
        {showCitizenReports && <CitizenReportLayer reports={citizenReports} />}
      </MapContainer>
    </div>
  );
};
