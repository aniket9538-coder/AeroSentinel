import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { CitizenReport } from '../../types';
import {
  FileText,
  Clock,
  MapPin,
  CheckCircle2,
  AlertCircle,
  Eye,
  Layers,
} from 'lucide-react';

interface ReportStatusProps {
  reports?: CitizenReport[];
  onSelectReport?: (report: CitizenReport) => void;
}

export const ReportStatus: React.FC<ReportStatusProps> = ({
  reports = [],
  onSelectReport,
}) => {
  return (
    <Card
      title="Recent Community Evidence"
      subtitle="Crowdsourced ground signals under municipal review"
      badge={<Badge variant="info">{reports.length} Active Feeds</Badge>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', marginTop: '0.75rem' }}>
        {reports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)' }}>
            <Eye size={24} style={{ margin: '0 auto 0.5rem auto', opacity: 0.5 }} />
            <p style={{ fontSize: '0.85rem' }}>No recent citizen evidence submitted in this sector.</p>
          </div>
        ) : (
          reports.map((r) => {
            const isVerified = r.status === 'VERIFIED';
            const isPending = r.status === 'PENDING';

            return (
              <div
                key={r.id}
                onClick={() => onSelectReport && onSelectReport(r)}
                style={{
                  padding: '1rem',
                  borderRadius: '10px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.5rem',
                  cursor: onSelectReport ? 'pointer' : 'default',
                  transition: 'border-color 0.15s ease',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span
                      style={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '0.15rem 0.45rem',
                        borderRadius: '4px',
                        background: 'rgba(56, 189, 248, 0.12)',
                        color: 'var(--brand-primary)',
                      }}
                    >
                      {r.category}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>#{r.id}</span>
                  </div>

                  <Badge variant={isVerified ? 'success' : isPending ? 'warning' : 'neutral'}>
                    {r.status}
                  </Badge>
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)', lineHeight: 1.45 }}>
                  {r.description}
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    fontSize: '0.72rem',
                    color: 'var(--text-muted)',
                    paddingTop: '0.4rem',
                    borderTop: '1px solid var(--border-subtle)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <MapPin size={12} />
                    <span>{r.latitude.toFixed(4)}, {r.longitude.toFixed(4)}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <Clock size={12} />
                    <span>{new Date(r.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </Card>
  );
};
