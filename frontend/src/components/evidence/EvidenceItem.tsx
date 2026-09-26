import React from 'react';
import { EventEvidence } from '../../types';
import { Badge } from '../common/Badge';
import {
  TrendingUp,
  Wind,
  Flame,
  Satellite,
  Eye,
  AlertTriangle,
  Clock,
  ShieldCheck,
} from 'lucide-react';

export interface RichEvidenceItem {
  id: string;
  time: string;
  source: string;
  sourceCategory: 'sensor' | 'weather' | 'satellite' | 'citizen' | 'model';
  signal: string;
  detail: string;
  quality: string;
  confidenceScore: number;
}

interface EvidenceItemProps {
  evidence: RichEvidenceItem;
  isLast?: boolean;
}

export const EvidenceItem: React.FC<EvidenceItemProps> = ({ evidence, isLast = false }) => {
  const getIcon = () => {
    switch (evidence.sourceCategory) {
      case 'sensor':
        return <TrendingUp size={16} color="var(--brand-primary)" />;
      case 'weather':
        return <Wind size={16} color="var(--accent-teal)" />;
      case 'satellite':
        return <Satellite size={16} color="var(--accent-purple)" />;
      case 'citizen':
        return <Eye size={16} color="var(--accent-amber)" />;
      case 'model':
      default:
        return <AlertTriangle size={16} color="var(--accent-rose)" />;
    }
  };

  const getSourceBadgeVariant = () => {
    switch (evidence.sourceCategory) {
      case 'sensor':
        return 'info';
      case 'weather':
        return 'neutral';
      case 'satellite':
        return 'warning';
      case 'citizen':
        return 'danger';
      default:
        return 'neutral';
    }
  };

  return (
    <div style={{ display: 'flex', gap: '1.25rem', position: 'relative' }}>
      {/* Vertical Timeline Track & Node */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '36px' }}>
        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '50%',
            background: 'var(--bg-surface-elevated)',
            border: '2px solid var(--border-medium)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 2,
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          {getIcon()}
        </div>
        {!isLast && (
          <div
            style={{
              width: '2px',
              flex: 1,
              background: 'var(--border-subtle)',
              margin: '4px 0',
            }}
          />
        )}
      </div>

      {/* Evidence Content Card */}
      <div
        style={{
          flex: 1,
          padding: '1rem 1.25rem',
          borderRadius: '12px',
          background: 'var(--bg-surface-elevated)',
          border: '1px solid var(--border-subtle)',
          marginBottom: isLast ? '0' : '1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.4rem',
        }}
      >
        {/* Top Header: Timestamp + Source + Quality Badge */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.825rem',
                fontWeight: 700,
                color: 'var(--brand-primary)',
                background: 'var(--brand-surface)',
                padding: '0.15rem 0.5rem',
                borderRadius: '4px',
                border: '1px solid var(--brand-border)',
              }}
            >
              {evidence.time}
            </span>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {evidence.source}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Quality:</span>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                padding: '0.15rem 0.5rem',
                borderRadius: '4px',
                background: 'rgba(16, 185, 129, 0.12)',
                color: 'var(--accent-teal)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
              }}
            >
              {evidence.quality}
            </span>
          </div>
        </div>

        {/* Signal Title */}
        <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          {evidence.signal}
        </div>

        {/* Detail description */}
        <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', lineHeight: 1.45 }}>
          {evidence.detail}
        </div>

        {/* Confidence metric indicator */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '0.35rem', paddingTop: '0.4rem', borderTop: '1px solid var(--border-subtle)', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          <ShieldCheck size={13} color="var(--accent-teal)" />
          <span>Confidence Score: <strong>{(evidence.confidenceScore * 100).toFixed(0)}%</strong></span>
        </div>
      </div>
    </div>
  );
};
