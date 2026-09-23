import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { PollutionEvent } from '../../types';

interface EventCardProps {
  event: PollutionEvent;
  onClick?: () => void;
}

export const EventCard: React.FC<EventCardProps> = ({ event, onClick }) => {
  return (
    <div onClick={onClick} style={{ cursor: 'pointer', marginBottom: '0.75rem' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f3f4f6' }}>{event.eventCode}</div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af', fontFamily: 'monospace' }}>Cell: {event.h3Index}</div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Badge variant={event.severity === 'HIGH' ? 'danger' : 'warning'}>{event.severity}</Badge>
            <Badge variant="info">{event.status}</Badge>
          </div>
        </div>
      </Card>
    </div>
  );
};
