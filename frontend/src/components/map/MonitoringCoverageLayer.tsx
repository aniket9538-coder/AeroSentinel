import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import * as h3 from 'h3-js';
import { MonitoringRecommendation } from '../../types';

interface MonitoringCoverageLayerProps {
  recommendations?: MonitoringRecommendation[];
}

export const MonitoringCoverageLayer: React.FC<MonitoringCoverageLayerProps> = ({ recommendations = [] }) => {
  return (
    <>
      {recommendations.map((rec) => {
        let boundary: [number, number][] = [];
        try {
          boundary = h3.cellToBoundary(rec.h3Index) as [number, number][];
        } catch {
          return null;
        }

        const color =
          rec.priorityLevel === 'HIGH'
            ? '#ec4899'
            : rec.priorityLevel === 'MEDIUM'
            ? '#a855f7'
            : '#6366f1';

        return (
          <Polygon
            key={rec.h3Index}
            positions={boundary}
            pathOptions={{
              color: color,
              fillColor: color,
              fillOpacity: 0.35,
              weight: 1.5,
              dashArray: '4, 4',
            }}
          >
            <Popup>
              <div style={{ color: '#0f172a', fontSize: '0.85rem' }}>
                <strong style={{ color: '#be185d' }}>Monitoring Blindspot Priority: {rec.priorityLevel}</strong>
                <div>Priority Score: {rec.priorityScore.toFixed(1)}/100</div>
                <div>Distance to nearest CAAQMS: {rec.stationDistanceKm.toFixed(1)} km</div>
                <div>Model Uncertainty: {(rec.uncertainty * 100).toFixed(0)}%</div>
                <div style={{ marginTop: '0.25rem', fontSize: '0.75rem', fontWeight: 600 }}>
                  Recommendation: Deploy mobile sensor / field verification
                </div>
              </div>
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};
