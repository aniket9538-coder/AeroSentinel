import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Alert } from '../../types';

interface AlertDetailsProps {
  alert: Alert;
}

export const AlertDetails: React.FC<AlertDetailsProps> = ({ alert }) => {
  return (
    <Card title="Alert Telemetry & Action Advisory" subtitle={`ID: ${alert.id}`}>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <Badge variant={alert.severity === 'CRITICAL' ? 'danger' : 'warning'}>{alert.severity}</Badge>
        <Badge variant="neutral">STATUS: {alert.status}</Badge>
      </div>
      <h4 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f3f4f6' }}>{alert.title}</h4>
      <p style={{ fontSize: '0.875rem', color: '#9ca3af', marginTop: '0.5rem', lineHeight: 1.5 }}>
        {alert.message}
      </p>
      <div style={{ marginTop: '1rem', padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
        <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Spatial Target (H3):</div>
        <div style={{ fontSize: '0.9rem', fontFamily: 'monospace', color: '#38bdf8' }}>{alert.h3Index}</div>
        <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.25rem' }}>
          Timestamp: {new Date(alert.createdAt).toLocaleString()}
        </div>
      </div>
    </Card>
  );
};
