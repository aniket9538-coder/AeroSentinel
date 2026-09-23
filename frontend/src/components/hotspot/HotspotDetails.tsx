import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { HotspotPrediction } from '../../types';

interface HotspotDetailsProps {
  hotspot: HotspotPrediction;
}

export const HotspotDetails: React.FC<HotspotDetailsProps> = ({ hotspot }) => {
  return (
    <Card title="Hotspot Diagnostic Information" subtitle={`H3 Spatial Index: ${hotspot.h3Index}`}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem', marginTop: '1rem' }}>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <span style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Risk Assessment</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
            <span style={{ fontSize: '1.25rem', fontWeight: 700 }}>{hotspot.riskScore}/100</span>
            <Badge variant={hotspot.riskLevel === 'HIGH' ? 'danger' : 'warning'}>{hotspot.riskLevel}</Badge>
          </div>
        </div>

        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <span style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Model Certainty</span>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, marginTop: '0.25rem' }}>
            {(hotspot.confidence * 100).toFixed(1)}%
          </div>
        </div>
      </div>

      <div style={{ marginTop: '1.25rem' }}>
        <span style={{ fontSize: '0.8rem', color: '#9ca3af', fontWeight: 600 }}>Identified Contributors:</span>
        <ul style={{ paddingLeft: '1.25rem', marginTop: '0.5rem', fontSize: '0.85rem', color: '#e5e7eb' }}>
          {hotspot.primaryFactors?.map((f, i) => (
            <li key={i} style={{ marginBottom: '0.25rem' }}>{f}</li>
          )) || <li>Atmospheric boundary layer compression and low wind stagnation</li>}
        </ul>
      </div>
    </Card>
  );
};
