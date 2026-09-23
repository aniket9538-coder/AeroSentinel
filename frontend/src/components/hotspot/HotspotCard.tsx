import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { HotspotPrediction } from '../../types';

interface HotspotCardProps {
  hotspot: HotspotPrediction;
  onClick?: () => void;
  isSelected?: boolean;
}

export const HotspotCard: React.FC<HotspotCardProps> = ({ hotspot, onClick, isSelected }) => {
  const badgeVariant =
    hotspot.riskLevel === 'HIGH' ? 'danger' : hotspot.riskLevel === 'MEDIUM' ? 'warning' : 'success';

  return (
    <div
      onClick={onClick}
      style={{
        cursor: 'pointer',
        border: isSelected ? '1px solid #38bdf8' : '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '12px',
        transition: 'all 0.2s ease',
      }}
    >
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: '#9ca3af', fontFamily: 'monospace' }}>
              HEX: {hotspot.h3Index}
            </span>
            <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f3f4f6', marginTop: '0.15rem' }}>
              Score: {hotspot.riskScore}/100
            </h4>
          </div>
          <Badge variant={badgeVariant}>{hotspot.riskLevel} RISK</Badge>
        </div>
        <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
          Certainty: {(hotspot.confidence * 100).toFixed(0)}% | Model: {hotspot.modelVersion}
        </div>
      </Card>
    </div>
  );
};
