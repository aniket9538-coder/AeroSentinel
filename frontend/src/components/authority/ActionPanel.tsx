import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';

interface ActionPanelProps {
  alertId?: string;
  onLogAction: (data: { actionType: string; actionDetails: string; performedBy: string }) => void;
  isLoading?: boolean;
}

export const ActionPanel: React.FC<ActionPanelProps> = ({ alertId, onLogAction, isLoading = false }) => {
  const [actionType, setActionType] = useState('WATER_MISTING');
  const [details, setDetails] = useState('');
  const [officer, setOfficer] = useState('Officer R. Patil');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogAction({ actionType, actionDetails: details, performedBy: officer });
  };

  return (
    <Card title="Execute Mitigation Action" subtitle={`Associated Alert ID: ${alertId || 'None Selected'}`}>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.75rem' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.25rem' }}>
            Action Category
          </label>
          <select
            value={actionType}
            onChange={(e) => setActionType(e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem',
              backgroundColor: '#1f2937',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '6px',
              color: '#f3f4f6',
              fontSize: '0.875rem',
            }}
          >
            <option value="WATER_MISTING">Deploy Water Sprinklers / Anti-Smog Cannon</option>
            <option value="TRAFFIC_DIVERSION">Traffic Congestion Re-routing</option>
            <option value="BURNING_SUPPRESSION">Extinguish Open Burning / Solid Waste Fire</option>
            <option value="INDUSTRIAL_AUDIT">Notice to Industrial Facility</option>
            <option value="MOBILE_MONITORING">Deploy Mobile CAAQMS Sensor</option>
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.25rem' }}>
            Action Notes & Execution Details
          </label>
          <textarea
            rows={2}
            value={details}
            onChange={(e) => setDetails(e.target.value)}
            placeholder="e.g. Anti-smog gun truck 04 deployed to Sector 12 industrial corridor..."
            style={{
              width: '100%',
              padding: '0.5rem',
              backgroundColor: '#1f2937',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '6px',
              color: '#f3f4f6',
              fontSize: '0.875rem',
            }}
          />
        </div>

        <Button type="submit" variant="primary" disabled={!alertId} isLoading={isLoading}>
          Record Official Mitigation Action
        </Button>
      </form>
    </Card>
  );
};
