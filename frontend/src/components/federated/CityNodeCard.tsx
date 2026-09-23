import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { FederatedNode } from '../../types';
import { Server } from 'lucide-react';

interface CityNodeCardProps {
  node: FederatedNode;
}

export const CityNodeCard: React.FC<CityNodeCardProps> = ({ node }) => {
  const isOnline = node.status === 'ONLINE';

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            padding: '0.5rem',
            borderRadius: '8px',
            background: isOnline ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
            color: isOnline ? '#10b981' : '#ef4444',
          }}>
            <Server size={20} />
          </div>
          <div>
            <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f3f4f6' }}>{node.nodeName}</h4>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Model Version: {node.modelVersion}</div>
          </div>
        </div>
        <Badge variant={isOnline ? 'success' : 'danger'}>{node.status}</Badge>
      </div>
      <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.75rem' }}>
        Last Synchronized: {new Date(node.lastUpdateAt).toLocaleTimeString()}
      </div>
    </Card>
  );
};
