import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { Alert } from '../../types';

interface IncidentQueueProps {
  alerts: Alert[];
  onSelectAlert?: (alert: Alert) => void;
  selectedAlertId?: string;
}

export const IncidentQueue: React.FC<IncidentQueueProps> = ({
  alerts,
  onSelectAlert,
  selectedAlertId,
}) => {
  return (
    <Card title="Active Incident Queue" subtitle="Prioritized municipal response triage">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.75rem' }}>
        {alerts.map((alert) => {
          const isSelected = alert.id === selectedAlertId;
          return (
            <div
              key={alert.id}
              onClick={() => onSelectAlert && onSelectAlert(alert)}
              style={{
                padding: '0.75rem',
                borderRadius: '8px',
                border: isSelected ? '1px solid #38bdf8' : '1px solid rgba(255, 255, 255, 0.08)',
                background: isSelected ? 'rgba(56, 189, 248, 0.08)' : 'rgba(255, 255, 255, 0.02)',
                cursor: 'pointer',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#f3f4f6' }}>{alert.title}</div>
                <div style={{ fontSize: '0.75rem', color: '#9ca3af', fontFamily: 'monospace' }}>
                  Cell: {alert.h3Index}
                </div>
              </div>
              <Badge variant={alert.severity === 'CRITICAL' ? 'danger' : 'warning'}>{alert.severity}</Badge>
            </div>
          );
        })}
      </div>
    </Card>
  );
};
