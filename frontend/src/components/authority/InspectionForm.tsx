import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';

interface InspectionFormProps {
  alertId?: string;
  onScheduleInspection: (data: { assignedTeam: string; scheduledAt: string; alertId: string }) => void;
  isLoading?: boolean;
}

export const InspectionForm: React.FC<InspectionFormProps> = ({
  alertId,
  onScheduleInspection,
  isLoading = false,
}) => {
  const [team, setTeam] = useState('Mobile Rapid Response Team 1');
  const [time, setTime] = useState(new Date().toISOString().slice(0, 16));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (alertId) {
      onScheduleInspection({ assignedTeam: team, scheduledAt: time, alertId });
    }
  };

  return (
    <Card title="Dispatch Field Inspection Team" subtitle="Direct physical ground verification">
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.75rem' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.25rem' }}>
            Inspection Crew
          </label>
          <input
            type="text"
            value={team}
            onChange={(e) => setTeam(e.target.value)}
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

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.25rem' }}>
            Scheduled Time
          </label>
          <input
            type="datetime-local"
            value={time}
            onChange={(e) => setTime(e.target.value)}
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

        <Button type="submit" variant="secondary" disabled={!alertId} isLoading={isLoading}>
          Dispatch Inspection Team
        </Button>
      </form>
    </Card>
  );
};
