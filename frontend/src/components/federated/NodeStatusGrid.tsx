import React from 'react';
import { FederatedNode } from '../../types/federated';
import { CityNodeCard } from './CityNodeCard';
import { Network } from 'lucide-react';

interface NodeStatusGridProps {
  nodes: FederatedNode[];
  onHeartbeat?: (nodeId: string) => Promise<void> | void;
}

export const NodeStatusGrid: React.FC<NodeStatusGridProps> = ({ nodes, onHeartbeat }) => {
  const onlineCount = nodes.filter(
    (n) => (n.status || '').toUpperCase() === 'ONLINE' || (n.status || '').toUpperCase() === 'TRAINING'
  ).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Network size={18} color="#38bdf8" />
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#f3f4f6', margin: 0 }}>
            Participating Municipal Federated Nodes
          </h3>
        </div>
        <span
          style={{
            fontSize: '0.75rem',
            padding: '0.2rem 0.5rem',
            borderRadius: '9999px',
            background: 'rgba(56, 189, 248, 0.1)',
            color: '#38bdf8',
            border: '1px solid rgba(56, 189, 248, 0.25)',
          }}
        >
          {onlineCount} of {nodes.length} Active Nodes
        </span>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {nodes.map((node) => (
          <CityNodeCard
            key={node.nodeId || node.id}
            node={node}
            onHeartbeat={onHeartbeat}
          />
        ))}
      </div>
    </div>
  );
};

export default NodeStatusGrid;
