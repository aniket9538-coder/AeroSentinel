import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { CitizenReport } from '../../types';

interface ReportStatusProps {
  reports?: CitizenReport[];
}

export const ReportStatus: React.FC<ReportStatusProps> = ({ reports = [] }) => {
  return (
    <Card title="Submitted Citizen Reports" subtitle="Evidence verification status">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.75rem' }}>
        {reports.length === 0 ? (
          <p style={{ fontSize: '0.85rem', color: '#9ca3af', textAlign: 'center', padding: '1rem' }}>
            No recent reports submitted.
          </p>
        ) : (
          reports.map((r) => (
            <div
              key={r.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0.75rem',
                backgroundColor: 'rgba(255, 255, 255, 0.02)',
                borderRadius: '8px',
                border: '1px solid rgba(255, 255, 255, 0.06)',
              }}
            >
              <div>
                <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#f3f4f6' }}>{r.category}</div>
                <div style={{ fontSize: '0.8rem', color: '#9ca3af', marginTop: '0.15rem' }}>{r.description}</div>
              </div>
              <Badge variant={r.status === 'VERIFIED' ? 'success' : r.status === 'PENDING' ? 'warning' : 'neutral'}>
                {r.status}
              </Badge>
            </div>
          ))
        )}
      </div>
    </Card>
  );
};
