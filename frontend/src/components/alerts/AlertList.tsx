import React from 'react';
import { AlertCard } from './AlertCard';
import { Alert } from '../../types';

interface AlertListProps {
  alerts: Alert[];
  onAcknowledge?: (alertId: string) => void;
}

export const AlertList: React.FC<AlertListProps> = ({ alerts, onAcknowledge }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
      {alerts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af', fontSize: '0.875rem' }}>
          No active authority alerts.
        </div>
      ) : (
        alerts.map((alert) => (
          <AlertCard key={alert.id} alert={alert} onAcknowledge={onAcknowledge} />
        ))
      )}
    </div>
  );
};
