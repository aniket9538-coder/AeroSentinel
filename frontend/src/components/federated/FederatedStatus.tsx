import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';

interface FederatedStatusProps {
  currentRound: number;
  totalNodes: number;
  activeNodes: number;
  status: string;
}

export const FederatedStatus: React.FC<FederatedStatusProps> = ({
  currentRound,
  totalNodes,
  activeNodes,
  status,
}) => {
  return (
    <Card title="Federated Orchestration Status" subtitle="Privacy-preserving multi-city learning coordinator">
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginTop: '0.75rem' }}>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Current Round</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f3f4f6' }}>Round #{currentRound}</div>
        </div>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Participating Nodes</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#38bdf8' }}>
            {activeNodes} / {totalNodes}
          </div>
        </div>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Coordinator State</div>
          <div style={{ marginTop: '0.25rem' }}>
            <Badge variant="success">{status}</Badge>
          </div>
        </div>
      </div>
    </Card>
  );
};
