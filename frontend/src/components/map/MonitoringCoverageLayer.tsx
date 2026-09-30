import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import * as h3 from 'h3-js';
import { MonitoringRecommendation } from '../../types';

interface MonitoringCoverageLayerProps {
  recommendations?: MonitoringRecommendation[];
  onSelectRecommendation?: (rec: MonitoringRecommendation) => void;
  selectedH3Index?: string | null;
}

export const MonitoringCoverageLayer: React.FC<MonitoringCoverageLayerProps> = ({
  recommendations = [],
  onSelectRecommendation,
  selectedH3Index,
}) => {
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

        const isSelected = selectedH3Index === rec.h3Index;
        const distanceKm = rec.nearestStationDistanceKm ?? rec.stationDistanceKm;
        const priorityScoreDisplay = rec.priorityScorePercent ?? Math.round(rec.priorityScore);

        return (
          <Polygon
            key={rec.h3Index}
            positions={boundary}
            pathOptions={{
              color: isSelected ? '#ffffff' : color,
              fillColor: color,
              fillOpacity: isSelected ? 0.6 : 0.35,
              weight: isSelected ? 3 : 1.5,
              dashArray: '4, 4',
            }}
            eventHandlers={{
              click: () => onSelectRecommendation?.(rec),
            }}
          >
            <Popup>
              <div
                style={{
                  padding: '0.85rem 1rem',
                  minWidth: '260px',
                  maxWidth: '320px',
                  fontFamily: 'var(--font-body, system-ui, sans-serif)',
                  color: '#0f172a',
                  lineHeight: 1.4,
                }}
              >
                {/* Header with Title and Priority Badge */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '0.5rem',
                    paddingBottom: '0.35rem',
                    borderBottom: '1px solid #e2e8f0',
                  }}
                >
                  <strong style={{ fontSize: '0.85rem', color: '#0f172a', letterSpacing: '0.02em' }}>
                    MONITORING PRIORITY
                  </strong>
                  <span
                    style={{
                      fontSize: '0.675rem',
                      fontWeight: 700,
                      padding: '0.15rem 0.45rem',
                      borderRadius: '4px',
                      background: `${color}20`,
                      color: color,
                      border: `1px solid ${color}60`,
                    }}
                  >
                    {rec.priorityLevel} ({priorityScoreDisplay}/100)
                  </span>
                </div>

                {/* H3 Cell Index */}
                <div style={{ marginBottom: '0.5rem' }}>
                  <div style={{ fontSize: '0.65rem', textTransform: 'uppercase', color: '#64748b' }}>
                    H3 Cell
                  </div>
                  <div
                    style={{
                      fontSize: '0.775rem',
                      fontFamily: 'monospace',
                      fontWeight: 600,
                      color: '#0f172a',
                      wordBreak: 'break-all',
                    }}
                  >
                    {rec.h3Index}
                  </div>
                </div>

                {/* Metrics Grid */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr',
                    columnGap: '0.85rem',
                    rowGap: '0.3rem',
                    fontSize: '0.78rem',
                    marginBottom: '0.5rem',
                    background: '#f8fafc',
                    padding: '0.5rem 0.65rem',
                    borderRadius: '6px',
                    border: '1px solid #f1f5f9',
                  }}
                >
                  <span style={{ color: '#64748b' }}>Priority:</span>
                  <strong style={{ textAlign: 'right', color: color }}>
                    {rec.priorityLevel}
                  </strong>

                  <span style={{ color: '#64748b' }}>Priority Score:</span>
                  <strong style={{ textAlign: 'right', color: '#0f172a' }}>
                    {priorityScoreDisplay}/100
                  </strong>

                  <span style={{ color: '#64748b' }}>Risk:</span>
                  <span style={{ textAlign: 'right', color: '#0f172a' }}>
                    <strong>{rec.riskScore.toFixed(2)}</strong> ({rec.riskLevel})
                  </span>

                  <span style={{ color: '#64748b' }}>Risk Level:</span>
                  <span style={{ textAlign: 'right', fontWeight: 600, color: '#0f172a' }}>
                    {rec.riskLevel}
                  </span>

                  <span style={{ color: '#64748b' }}>Forecast Uncertainty:</span>
                  <span style={{ textAlign: 'right', fontWeight: 600, color: '#0284c7' }}>
                    {rec.uncertaintyIntervalWidth != null
                      ? `${rec.uncertaintyIntervalWidth.toFixed(1)} µg/m³`
                      : `${(rec.uncertainty * 100).toFixed(0)}%`}
                  </span>

                  <span style={{ color: '#64748b' }}>Nearest Station:</span>
                  <span style={{ textAlign: 'right', fontWeight: 600, color: '#0f172a', wordBreak: 'break-word' }}>
                    {rec.nearestStationName || rec.nearestStationCode || 'Unknown'}
                  </span>

                  <span style={{ color: '#64748b' }}>Distance:</span>
                  <strong style={{ textAlign: 'right', color: '#0f172a' }}>
                    {distanceKm != null ? `${distanceKm.toFixed(1)} km` : 'N/A'}
                  </strong>

                  <span style={{ color: '#64748b' }}>Coverage Gap:</span>
                  <strong style={{ textAlign: 'right', color: rec.monitoringCoverageGapFlag === 1 ? '#e11d48' : '#059669' }}>
                    {rec.monitoringCoverageGapFlag === 1 ? 'YES' : 'NO'}
                  </strong>
                </div>

                {/* Recommendation Box */}
                <div
                  style={{
                    padding: '0.45rem 0.6rem',
                    borderRadius: '6px',
                    background: `${color}12`,
                    border: `1px solid ${color}30`,
                    marginBottom: '0.5rem',
                  }}
                >
                  <div style={{ fontSize: '0.675rem', fontWeight: 700, color: color, textTransform: 'uppercase', marginBottom: '0.15rem' }}>
                    Recommendation: {rec.recommendationType ? rec.recommendationType.replace(/_/g, ' ') : 'N/A'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#334155', fontWeight: 500 }}>
                    Action Guidance: {rec.recommendation}
                  </div>
                </div>

                {/* Non-alarmist Disclaimer */}
                <div
                  style={{
                    fontSize: '0.675rem',
                    color: '#64748b',
                    fontStyle: 'italic',
                    lineHeight: 1.35,
                    borderTop: '1px solid #e2e8f0',
                    paddingTop: '0.35rem',
                  }}
                >
                  Monitoring gap indicates limited proximity to existing monitoring stations; it does not by itself confirm pollution.
                </div>
              </div>
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};

export default MonitoringCoverageLayer;
