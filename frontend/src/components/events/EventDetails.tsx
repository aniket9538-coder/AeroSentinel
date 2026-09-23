import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { PollutionEvent } from '../../types';

interface EventDetailsProps {
  event: PollutionEvent;
}

export const EventDetails: React.FC<EventDetailsProps> = ({ event }) => {
  return (
    <Card title={`Incident Event ${event.eventCode}`} subtitle={`H3 Cell: ${event.h3Index}`}>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <Badge variant={event.severity === 'HIGH' ? 'danger' : 'warning'}>{event.severity} SEVERITY</Badge>
        <Badge variant="info">{event.status}</Badge>
      </div>
      <div style={{ fontSize: '0.85rem', color: '#9ca3af' }}>
        Started: {new Date(event.startedAt).toLocaleString()}
      </div>
      {event.resolvedAt && (
        <div style={{ fontSize: '0.85rem', color: '#10b981', marginTop: '0.25rem' }}>
          Resolved: {new Date(event.resolvedAt).toLocaleString()}
        </div>
      )}
    </Card>
  );
};
