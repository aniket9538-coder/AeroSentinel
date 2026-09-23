import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { Alert } from '../../types';
import { AlertTriangle } from 'lucide-react';

interface AlertCardProps {
  alert: Alert;
  onAcknowledge?: (alertId: string) => void;
  onClick?: () => void;
}

export const AlertCard: React.FC<AlertCardProps> = ({ alert, onAcknowledge, onClick }) => {
  const isCritical = alert.severity === 'CRITICAL';

  return (
    <Card className={isCritical ? 'border-rose-500/30' : ''}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
          <div style={{
            padding: '0.5rem',
            borderRadius: '8px',
            background: isCritical ? 'rgba(244, 63, 94, 0.15)' : 'rgba(245, 158, 11, 0.15)',
            color: isCritical ? '#f43f5e' : '#f59e0b',
          }}>
            <AlertTriangle size={20} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f3f4f6' }}>{alert.title}</h4>
              <Badge variant={isCritical ? 'danger' : 'warning'}>{alert.severity}</Badge>
            </div>
            <p style={{ fontSize: '0.85rem', color: '#9ca3af', marginTop: '0.25rem' }}>{alert.message}</p>
            <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.5rem', fontFamily: 'monospace' }}>
              Cell: {alert.h3Index} | Time: {new Date(alert.createdAt).toLocaleTimeString()}
            </div>
          </div>
        </div>

        {alert.status === 'OPEN' && onAcknowledge && (
          <Button size="sm" variant="outline" onClick={() => onAcknowledge(alert.id)}>
            Acknowledge
          </Button>
        )}
      </div>
    </Card>
  );
};
