import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import * as h3 from 'h3-js';
import { HotspotPrediction } from '../../types';

interface H3RiskLayerProps {
  hotspots?: HotspotPrediction[];
}

export const H3RiskLayer: React.FC<H3RiskLayerProps> = ({ hotspots = [] }) => {
  return (
    <>
      {hotspots.map((hotspot) => {
        let coordinates: [number, number][] = [];
        try {
          // Convert H3 index to polygon boundary (returns [lat, lng][])
          coordinates = h3.cellToBoundary(hotspot.h3Index) as [number, number][];
        } catch {
          // Fallback if cell is mock
          return null;
        }

        const color =
          hotspot.riskLevel === 'HIGH'
            ? '#ef4444'
            : hotspot.riskLevel === 'MEDIUM'
            ? '#f59e0b'
            : '#10b981';

        return (
          <Polygon
            key={hotspot.id}
            positions={coordinates}
            pathOptions={{
              color: color,
              fillColor: color,
              fillOpacity: 0.45,
              weight: 2,
            }}
          >
            <Popup>
              <div style={{ color: '#1e293b', fontSize: '0.85rem' }}>
                <strong style={{ fontSize: '0.95rem' }}>H3 Cell: {hotspot.h3Index}</strong>
                <div style={{ marginTop: '0.25rem' }}>
                  Risk Score: <strong>{hotspot.riskScore}/100 ({hotspot.riskLevel})</strong>
                </div>
                <div>Confidence: {(hotspot.confidence * 100).toFixed(0)}%</div>
                <div>Model: {hotspot.modelVersion}</div>
              </div>
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};
