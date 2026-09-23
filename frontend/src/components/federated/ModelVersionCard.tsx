import React from 'react';
import { Card } from '../common/Card';
import { Cpu } from 'lucide-react';

interface ModelVersionCardProps {
  globalVersion: string;
  aggregationStrategy: string;
  lastAggregatedAt: string;
}

export const ModelVersionCard: React.FC<ModelVersionCardProps> = ({
  globalVersion = 'global-hotspot-v1.2',
  aggregationStrategy = 'Federated Averaging (FedAvg)',
  lastAggregatedAt = new Date().toLocaleTimeString(),
}) => {
  return (
    <Card title="Global Model Synchronization">
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.5rem' }}>
        <div style={{
          padding: '0.75rem',
          borderRadius: '10px',
          background: 'rgba(6, 182, 212, 0.15)',
          color: '#06b6d4',
        }}>
          <Cpu size={28} />
        </div>
        <div>
          <div style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f3f4f6' }}>{globalVersion}</div>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af', marginTop: '0.2rem' }}>
            Strategy: {aggregationStrategy}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.15rem' }}>
            Last Aggregation: {lastAggregatedAt}
          </div>
        </div>
      </div>
    </Card>
  );
};
