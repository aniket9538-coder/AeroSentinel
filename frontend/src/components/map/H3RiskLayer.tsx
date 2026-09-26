import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import * as h3 from 'h3-js';
import { HotspotPrediction } from '../../types';
import { HotspotCell, HotspotRiskLevel } from '../../types/hotspot';
import { getRiskStyle } from '../../utils/hotspotColors';

export interface H3CellData extends Partial<HotspotPrediction>, Partial<HotspotCell> {
  h3Index: string;
  riskScore?: number;
  riskLevel?: HotspotRiskLevel | any;
  confidence?: number;
  predictedAt?: string;
  freshness?: any;
  modelVersion?: string;
  pm25?: number;
  temperature?: number;
  humidity?: number;
  nearestStation?: string;
  nearestStationDistanceKm?: number;
  lastUpdated?: string;
}

interface H3RiskLayerProps {
  hotspots?: (HotspotPrediction | H3CellData | HotspotCell)[];
  onSelectCell?: (cell: H3CellData) => void;
  selectedCellId?: string;
}

export const H3RiskLayer: React.FC<H3RiskLayerProps> = ({
  hotspots = [],
  onSelectCell,
  selectedCellId,
}) => {
  // Sort so the selected cell is rendered last (top of SVG stack) for crisp visibility of selected styling (BUG 3)
  const sortedHotspots = React.useMemo(() => {
    if (!selectedCellId || hotspots.length <= 1) return hotspots;
    return [...hotspots].sort((a, b) => {
      const aId = (a as any)?.h3Index;
      const bId = (b as any)?.h3Index;
      if (aId === selectedCellId) return 1;
      if (bId === selectedCellId) return -1;
      return 0;
    });
  }, [hotspots, selectedCellId]);

  return (
    <>
      {sortedHotspots.map((cell) => {
        const cellData = cell as H3CellData;
        if (!cellData || !cellData.h3Index) return null;

        let coordinates: [number, number][] = [];
        try {
          // Convert H3 index to polygon boundary (returns [lat, lng][])
          coordinates = h3.cellToBoundary(cellData.h3Index) as [number, number][];
        } catch {
          return null;
        }

        const isSelected = selectedCellId === cellData.h3Index;
        const riskLevel = cellData.riskLevel || 'LOW';
        const riskStyle = getRiskStyle(riskLevel);
        const color = riskStyle.color;

        return (
          <Polygon
            key={cellData.h3Index || (cellData as any).id}
            positions={coordinates}
            pathOptions={{
              color: isSelected ? '#38bdf8' : color,
              fillColor: color,
              fillOpacity: isSelected ? 0.65 : 0.45,
              weight: isSelected ? 3 : 1.5,
            }}
            eventHandlers={{
              click: () => onSelectCell?.(cellData),
            }}
          >
            <Popup>
              <div
                style={{
                  padding: '0.85rem 1rem',
                  minWidth: '240px',
                  fontFamily: 'var(--font-body)',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '0.5rem',
                    borderBottom: '1px solid var(--border-subtle)',
                    paddingBottom: '0.35rem',
                  }}
                >
                  <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                    POTENTIAL HOTSPOT
                  </strong>
                  <span
                    style={{
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      padding: '0.15rem 0.4rem',
                      borderRadius: '4px',
                      background: riskStyle.bg,
                      color: riskStyle.color,
                      border: `1px solid ${riskStyle.border}`,
                    }}
                  >
                    {riskStyle.label}
                  </span>
                </div>

                <div style={{ marginBottom: '0.5rem' }}>
                  <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>H3 Cell Index</div>
                  <div
                    style={{
                      fontSize: '0.75rem',
                      fontFamily: 'var(--font-mono)',
                      color: 'var(--brand-primary)',
                      wordBreak: 'break-all',
                    }}
                  >
                    {cellData.h3Index}
                  </div>
                </div>

                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr',
                    columnGap: '1rem',
                    rowGap: '0.3rem',
                    fontSize: '0.78rem',
                  }}
                >
                  {cellData.riskScore !== undefined && (
                    <>
                      <span style={{ color: 'var(--text-muted)' }}>Risk Score</span>
                      <strong style={{ textAlign: 'right', color: color }}>
                        {(cellData.riskScore * 100).toFixed(1)}%
                      </strong>
                    </>
                  )}

                  {cellData.confidence !== undefined && (
                    <>
                      <span style={{ color: 'var(--text-muted)' }}>Confidence</span>
                      <span style={{ textAlign: 'right', color: 'var(--text-primary)' }}>
                        {(cellData.confidence * 100).toFixed(0)}%
                      </span>
                    </>
                  )}

                  {cellData.freshness && (
                    <>
                      <span style={{ color: 'var(--text-muted)' }}>Freshness</span>
                      <span style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                        {cellData.freshness}
                      </span>
                    </>
                  )}

                  {cellData.modelVersion && (
                    <>
                      <span style={{ color: 'var(--text-muted)' }}>Model</span>
                      <span style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                        {cellData.modelVersion}
                      </span>
                    </>
                  )}
                </div>
              </div>
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};

export default H3RiskLayer;
