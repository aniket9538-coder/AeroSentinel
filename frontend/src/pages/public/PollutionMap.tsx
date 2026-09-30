import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { PollutionMap as MapView } from '../../components/map/PollutionMap';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Layers } from 'lucide-react';

export const PollutionMap: React.FC = () => {
  const [showHotspots, setShowHotspots] = useState(true);
  const [showFires, setShowFires] = useState(true);
  const [showCitizen, setShowCitizen] = useState(true);
  const [showMonitoring, setShowMonitoring] = useState(false);

  // Sample seed data for interactive display
  const sampleStations = [
    { stationId: 'PUN-1', stationName: 'Shivajinagar', latitude: 18.5314, longitude: 73.8446, observedAt: new Date().toISOString(), pm25: 64, aqi: 142, source: 'CPCB' },
    { stationId: 'PUN-2', stationName: 'Katraj', latitude: 18.4575, longitude: 73.8677, observedAt: new Date().toISOString(), pm25: 42, aqi: 98, source: 'CPCB' },
    { stationId: 'PUN-3', stationName: 'Hadapsar MIDC', latitude: 18.5089, longitude: 73.9260, observedAt: new Date().toISOString(), pm25: 98, aqi: 185, source: 'CPCB' },
  ];

  const sampleHotspots = [
    { id: 'h1', h3Index: '8860144aa1fffff', cityId: 'pune', predictedAt: new Date().toISOString(), riskScore: 88, riskLevel: 'HIGH' as const, confidence: 0.89, modelVersion: 'xgb-v1.2' },
    { id: 'h2', h3Index: '8860144ab3fffff', cityId: 'pune', predictedAt: new Date().toISOString(), riskScore: 62, riskLevel: 'MEDIUM' as const, confidence: 0.78, modelVersion: 'xgb-v1.2' },
  ];

  const sampleFires = [
    { id: 'f1', cityId: 'pune', latitude: 18.4890, longitude: 73.9100, detectedAt: new Date().toISOString(), confidence: 92, frp: 14.5, satellite: 'VIIRS' },
  ];

  return (
    <PageContainer
      title="Hyperlocal Multi-Layer Spatial Map"
      subtitle="Interactive Uber H3 hexagonal grid overlay with multi-source telemetry"
      action={
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Button
            size="sm"
            variant={showHotspots ? 'primary' : 'secondary'}
            onClick={() => setShowHotspots(!showHotspots)}
          >
            H3 Risk
          </Button>
          <Button
            size="sm"
            variant={showMonitoring ? 'primary' : 'secondary'}
            onClick={() => setShowMonitoring(!showMonitoring)}
          >
            Monitoring Gaps
          </Button>
          <Button
            size="sm"
            variant={showFires ? 'primary' : 'secondary'}
            onClick={() => setShowFires(!showFires)}
          >
            Fires
          </Button>
          <Button
            size="sm"
            variant={showCitizen ? 'primary' : 'secondary'}
            onClick={() => setShowCitizen(!showCitizen)}
          >
            Citizen
          </Button>
        </div>
      }
    >
      <div style={{ height: '620px', width: '100%' }}>
        <MapView
          stations={sampleStations}
          hotspots={showHotspots ? sampleHotspots : []}
          fires={showFires ? sampleFires : []}
          showHotspots={showHotspots}
          showFires={showFires}
          showCitizenReports={showCitizen}
          showMonitoringCoverage={showMonitoring}
        />
      </div>
    </PageContainer>
  );
};
