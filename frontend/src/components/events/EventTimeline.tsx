import React from 'react';
import { PollutionEvent } from '../../types';

interface EventTimelineProps {
  events?: PollutionEvent[];
}

export const EventTimeline: React.FC<EventTimelineProps> = ({ events = [] }) => {
  return (
    <div style={{ padding: '0.5rem 0' }}>
      {events.map((evt, idx) => (
        <div key={evt.id} style={{ display: 'flex', gap: '1rem', position: 'relative', paddingBottom: '1.25rem' }}>
          {idx !== events.length - 1 && (
            <div style={{
              position: 'absolute',
              left: '11px',
              top: '20px',
              bottom: 0,
              width: '2px',
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
            }} />
          )}
          <div style={{
            width: '24px',
            height: '24px',
            borderRadius: '50%',
            backgroundColor: evt.status === 'RESOLVED' ? '#10b981' : '#f59e0b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1,
          }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#ffffff' }} />
          </div>
          <div>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f3f4f6' }}>{evt.eventCode}</div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
              Status: {evt.status} | Started: {new Date(evt.startedAt).toLocaleTimeString()}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
