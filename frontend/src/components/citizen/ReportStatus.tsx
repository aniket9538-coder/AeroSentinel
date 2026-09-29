import React from 'react';
import { Card } from '../common/Card';
import { Badge, BadgeVariant } from '../common/Badge';
import { CitizenReport } from '../../types';
import {
  FileText,
  Clock,
  MapPin,
  CheckCircle2,
  AlertCircle,
  Eye,
  Layers,
  Sparkles,
  ShieldAlert,
} from 'lucide-react';

interface ReportStatusProps {
  reports?: CitizenReport[];
  onSelectReport?: (report: CitizenReport) => void;
  selectedReportId?: string | null;
}

export const ReportStatus: React.FC<ReportStatusProps> = ({
  reports = [],
  onSelectReport,
  selectedReportId,
}) => {
  return (
    <Card
      title="Recent Community Evidence"
      subtitle="Live crowdsourced ground observations under municipal review"
      badge={<Badge variant="info">{reports.length} Active Feeds</Badge>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', marginTop: '0.75rem' }}>
        {reports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--text-muted)' }}>
            <Eye size={28} style={{ margin: '0 auto 0.5rem auto', opacity: 0.4 }} />
            <p style={{ fontSize: '0.85rem', fontWeight: 500 }}>No recent citizen evidence submitted in this sector.</p>
            <p style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>New ground reports will appear here in real time.</p>
          </div>
        ) : (
          reports.map((r) => {
            const reportRef = r.reportId || (r.id ? `CR-${r.id.slice(0, 8).toUpperCase()}` : 'CR-LOCAL');
            const isVerified = r.status === 'VERIFIED';
            const isPending = r.status === 'PENDING';
            const isAnalyzing = r.status === 'ANALYZING';
            const isFailed = r.status === 'FAILED' || r.status === 'ERROR';
            const isAnalyzed = r.status === 'ANALYZED';
            const isSelected = selectedReportId === r.id;

            const isRealGemini = r.visionAnalysis?.modelVersion?.toLowerCase().startsWith('gemini');
            const isFallback = r.visionAnalysis?.modelVersion?.toLowerCase().includes('fallback');

            let statusVariant: BadgeVariant = 'neutral';
            if (isVerified) statusVariant = 'success';
            else if (isPending || isAnalyzing) statusVariant = 'warning';
            else if (isFailed) statusVariant = 'danger';
            else if (isAnalyzed) statusVariant = 'info';

            return (
              <div
                key={r.id}
                role="button"
                tabIndex={0}
                onClick={() => onSelectReport && onSelectReport(r)}
                onKeyDown={(e) => {
                  if ((e.key === 'Enter' || e.key === ' ') && onSelectReport) {
                    onSelectReport(r);
                  }
                }}
                style={{
                  padding: '1rem',
                  borderRadius: '10px',
                  background: isSelected ? 'rgba(56, 189, 248, 0.08)' : 'var(--bg-surface-elevated, #ffffff)',
                  border: isSelected ? '1px solid var(--brand-primary, #38bdf8)' : '1px solid var(--border-subtle, #e2e8f0)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.55rem',
                  cursor: onSelectReport ? 'pointer' : 'default',
                  transition: 'all 0.15s ease',
                  boxShadow: isSelected ? '0 0 10px rgba(56, 189, 248, 0.15)' : 'none',
                }}
                aria-label={`View details for report ${reportRef}`}
              >
                {/* Header Row */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.4rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <span
                      style={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '0.15rem 0.45rem',
                        borderRadius: '4px',
                        background: 'rgba(56, 189, 248, 0.12)',
                        color: 'var(--brand-primary, #0284c7)',
                      }}
                    >
                      {r.category}
                    </span>
                    <strong style={{ fontSize: '0.78rem', color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                      {reportRef}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <span
                      style={{
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        padding: '0.1rem 0.35rem',
                        borderRadius: '3px',
                        background: 'rgba(245, 158, 11, 0.12)',
                        color: 'var(--accent-amber, #d97706)',
                        border: '1px solid rgba(245, 158, 11, 0.25)',
                      }}
                    >
                      UNVERIFIED
                    </span>
                    <Badge variant={statusVariant}>
                      {r.status || 'SUBMITTED'}
                    </Badge>
                  </div>
                </div>

                {/* Description */}
                <div
                  style={{
                    fontSize: '0.825rem',
                    color: 'var(--text-primary)',
                    lineHeight: 1.45,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                  }}
                >
                  {r.description || 'Observed emission at indicated spatial coordinates.'}
                </div>

                {/* AI Model indicator tag if available */}
                {r.visionAnalysis && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
                    <Sparkles size={11} style={{ color: isRealGemini ? 'var(--accent-purple, #a855f7)' : 'var(--accent-amber, #f59e0b)' }} />
                    <span>
                      {isRealGemini ? 'Analyzed by Gemini Vision' : isFallback ? 'Deterministic Fallback' : 'AI Analysis Attached'}
                    </span>
                    {r.visionAnalysis.confidence !== undefined && (
                      <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                        ({Math.round(r.visionAnalysis.confidence * 100)}%)
                      </span>
                    )}
                  </div>
                )}

                {/* Footer Meta Row */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    fontSize: '0.72rem',
                    color: 'var(--text-muted)',
                    paddingTop: '0.4rem',
                    borderTop: '1px solid var(--border-subtle, #f1f5f9)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <MapPin size={11} />
                    {r.h3Index ? (
                      <span style={{ fontFamily: 'var(--font-mono)' }}>H3: {r.h3Index.slice(0, 8)}...</span>
                    ) : (
                      <span>{r.latitude.toFixed(3)}, {r.longitude.toFixed(3)}</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <Clock size={11} />
                    <span>
                      {r.submittedAt ? new Date(r.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent'}
                    </span>
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
