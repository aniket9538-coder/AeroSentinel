import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { HotspotCell } from '../../types/hotspot';
import { getRiskStyle } from '../../utils/hotspotColors';
import { Flame, Shield, Clock, Cpu, Activity, AlertTriangle, Layers } from 'lucide-react';

interface HotspotCellDetailsCardProps {
  cell: HotspotCell | null;
  isLoading?: boolean;
}

export const HotspotCellDetailsCard: React.FC<HotspotCellDetailsCardProps> = ({
  cell,
  isLoading = false,
}) => {
  if (isLoading) {
    return (
      <Card title="Potential Hotspot Details">
        <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading cell risk intelligence...
        </div>
      </Card>
    );
  }

  if (!cell) {
    return (
      <Card title="Potential Hotspot Details">
        <div
          style={{
            padding: '2.5rem 1rem',
            textAlign: 'center',
            color: 'var(--text-muted)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <Layers size={36} color="var(--border-strong)" />
          <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
            No H3 Cell Selected
          </div>
          <div style={{ fontSize: '0.8rem', maxWidth: '280px', lineHeight: 1.4 }}>
            Click an H3 hexagonal cell on the risk map or select from the table to inspect risk metrics.
          </div>
        </div>
      </Card>
    );
  }

  const riskStyle = getRiskStyle(cell.riskLevel);
  const formattedTime = (() => {
    try {
      return new Date(cell.predictedAt).toLocaleString([], {
        dateStyle: 'medium',
        timeStyle: 'short',
      });
    } catch {
      return cell.predictedAt;
    }
  })();

  const freshnessVariant =
    cell.freshness === 'LIVE' ? 'success' : cell.freshness === 'STALE' ? 'warning' : 'danger';

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Flame size={18} color={riskStyle.color} />
          <span>Potential Hotspot</span>
        </div>
      }
      action={
        <Badge
          variant={
            cell.riskLevel === 'CRITICAL' || cell.riskLevel === 'HIGH'
              ? 'danger'
              : cell.riskLevel === 'MODERATE'
              ? 'warning'
              : 'success'
          }
        >
          {riskStyle.label.toUpperCase()}
        </Badge>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
        {/* Cell Index Header */}
        <div
          style={{
            padding: '0.75rem 0.9rem',
            borderRadius: '8px',
            backgroundColor: 'var(--bg-surface-elevated)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            H3 Cell Index
          </div>
          <div
            style={{
              fontSize: '0.95rem',
              fontWeight: 700,
              fontFamily: 'var(--font-mono)',
              color: 'var(--brand-primary)',
              wordBreak: 'break-all',
              marginTop: '0.15rem',
            }}
          >
            {cell.h3Index}
          </div>
        </div>

        {/* Risk Score Gauge & Progress Bar */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Risk Score
            </span>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, color: riskStyle.color, fontFamily: 'var(--font-heading)' }}>
              {(cell.riskScore * 100).toFixed(1)}%
            </span>
          </div>
          <div
            style={{
              height: '8px',
              borderRadius: '4px',
              backgroundColor: 'var(--bg-surface-elevated)',
              overflow: 'hidden',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div
              style={{
                height: '100%',
                width: `${Math.min(100, Math.max(5, cell.riskScore * 100))}%`,
                backgroundColor: riskStyle.color,
                borderRadius: '4px',
                transition: 'width 0.4s ease',
              }}
            />
          </div>
        </div>

        {/* 2x2 Metrics Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '0.75rem',
          }}
        >
          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              <Shield size={13} color="var(--brand-primary)" />
              <span>Confidence</span>
            </div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.2rem' }}>
              {(cell.confidence * 100).toFixed(0)}%
            </div>
          </div>

          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              <Activity size={13} color="var(--brand-primary)" />
              <span>Freshness</span>
            </div>
            <div style={{ marginTop: '0.35rem' }}>
              <Badge variant={freshnessVariant}>{cell.freshness}</Badge>
            </div>
          </div>

          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              <Cpu size={13} color="var(--brand-primary)" />
              <span>Model Version</span>
            </div>
            <div
              style={{
                fontSize: '0.78rem',
                fontWeight: 600,
                color: 'var(--text-secondary)',
                fontFamily: 'var(--font-mono)',
                marginTop: '0.25rem',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {cell.modelVersion}
            </div>
          </div>

          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              <Clock size={13} color="var(--brand-primary)" />
              <span>Predicted At</span>
            </div>
            <div
              style={{
                fontSize: '0.75rem',
                fontWeight: 500,
                color: 'var(--text-secondary)',
                marginTop: '0.25rem',
              }}
            >
              {formattedTime}
            </div>
          </div>
        </div>

        {/* Operational Disclaimer */}
        <div
          style={{
            padding: '0.7rem 0.85rem',
            borderRadius: '8px',
            backgroundColor: 'rgba(245, 158, 11, 0.08)',
            border: '1px solid rgba(245, 158, 11, 0.25)',
            fontSize: '0.73rem',
            color: 'var(--text-secondary)',
            lineHeight: 1.45,
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.5rem',
          }}
        >
          <AlertTriangle size={15} color="var(--accent-amber)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <span>
            <strong>Statistical Assessment:</strong> Represents potential elevated risk derived from environmental and meteorological patterns. Not an official regulatory citation.
          </span>
        </div>
      </div>
    </Card>
  );
};

export default HotspotCellDetailsCard;
