import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { MonitoringRecommendation } from '../../types/monitoring';
import {
  Radio,
  Shield,
  Clock,
  Activity,
  AlertTriangle,
  Layers,
  MapPin,
  TrendingUp,
  Compass,
  CheckCircle2,
  HelpCircle,
  Copy,
  Check,
} from 'lucide-react';

interface MonitoringRecommendationDetailCardProps {
  recommendation: MonitoringRecommendation | null;
  isLoading?: boolean;
}

export const MonitoringRecommendationDetailCard: React.FC<MonitoringRecommendationDetailCardProps> = ({
  recommendation,
  isLoading = false,
}) => {
  const [copied, setCopied] = React.useState(false);

  const handleCopyH3 = () => {
    if (recommendation?.h3Index) {
      navigator.clipboard.writeText(recommendation.h3Index);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (isLoading) {
    return (
      <Card title="Monitoring Priority Details">
        <div style={{ padding: '3rem 1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <div className="animate-spin" style={{ display: 'inline-block', marginBottom: '0.75rem' }}>
            <Radio size={28} color="var(--brand-primary)" />
          </div>
          <div>Loading cell priority intelligence...</div>
        </div>
      </Card>
    );
  }

  if (!recommendation) {
    return (
      <Card title="Monitoring Priority Details">
        <div
          style={{
            padding: '3rem 1.5rem',
            textAlign: 'center',
            color: 'var(--text-muted)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <Layers size={36} color="var(--border-medium)" />
          <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
            No H3 Cell Selected
          </div>
          <div style={{ fontSize: '0.825rem', maxWidth: '300px', lineHeight: 1.5 }}>
            Select an H3 cell from the priority table to inspect its atmospheric risk, forecast uncertainty, station coverage, and recommended observation action.
          </div>
        </div>
      </Card>
    );
  }

  const rec = recommendation;
  const priorityColor =
    rec.priorityLevel === 'HIGH'
      ? '#ec4899'
      : rec.priorityLevel === 'MEDIUM'
      ? '#a855f7'
      : '#6366f1';

  const priorityBadgeVariant =
    rec.priorityLevel === 'HIGH'
      ? 'danger'
      : rec.priorityLevel === 'MEDIUM'
      ? 'warning'
      : 'info';

  const formattedPredTime = (() => {
    if (!rec.predictionTimestamp) return 'N/A';
    try {
      return new Date(rec.predictionTimestamp).toLocaleString([], {
        dateStyle: 'short',
        timeStyle: 'short',
      });
    } catch {
      return rec.predictionTimestamp;
    }
  })();

  const formattedForecastTime = (() => {
    if (!rec.forecastGeneratedAt) return 'N/A';
    try {
      return new Date(rec.forecastGeneratedAt).toLocaleString([], {
        dateStyle: 'short',
        timeStyle: 'short',
      });
    } catch {
      return rec.forecastGeneratedAt;
    }
  })();

  const distanceVal = rec.nearestStationDistanceKm ?? rec.stationDistanceKm;

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Radio size={18} color={priorityColor} />
          <span>Monitoring Priority Detail</span>
        </div>
      }
      action={
        <Badge variant={priorityBadgeVariant}>
          {rec.priorityLevel} PRIORITY ({rec.priorityScorePercent ?? Math.round(rec.priorityScore)}/100)
        </Badge>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Cell Index & Coordinates */}
        <div
          style={{
            padding: '0.85rem 1rem',
            borderRadius: '10px',
            backgroundColor: 'var(--bg-surface-elevated)',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Uber H3 Resolution 8 Cell
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.15rem' }}>
              {rec.h3Index}
            </div>
            <div style={{ fontSize: '0.775rem', color: 'var(--text-secondary)', marginTop: '0.2rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <MapPin size={12} />
              <span>{rec.latitude.toFixed(4)}° N, {rec.longitude.toFixed(4)}° E</span>
            </div>
          </div>
          <button
            onClick={handleCopyH3}
            title="Copy H3 Index"
            style={{
              padding: '0.35rem 0.65rem',
              borderRadius: '6px',
              border: '1px solid var(--border-medium)',
              background: 'var(--bg-surface)',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
              fontSize: '0.75rem',
            }}
          >
            {copied ? <Check size={12} color="var(--accent-emerald)" /> : <Copy size={12} />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
        </div>

        {/* Action Guidance Recommendation Banner */}
        <div
          style={{
            padding: '1rem',
            borderRadius: '10px',
            background: rec.priorityLevel === 'HIGH' ? 'rgba(236, 72, 153, 0.08)' : 'rgba(168, 85, 247, 0.08)',
            border: `1px solid ${priorityColor}40`,
            display: 'flex',
            flexDirection: 'column',
            gap: '0.4rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 700, color: priorityColor }}>
            <Activity size={16} />
            <span>Recommended Action ({rec.recommendationType})</span>
          </div>
          <div style={{ fontSize: '0.9rem', color: 'var(--text-primary)', fontWeight: 600, lineHeight: 1.4 }}>
            {rec.recommendation}
          </div>
          {rec.rationale && (
            <div style={{ fontSize: '0.775rem', color: 'var(--text-secondary)', marginTop: '0.35rem', lineHeight: 1.45, borderTop: '1px dashed var(--border-subtle)', paddingTop: '0.35rem' }}>
              <strong>Rationale:</strong> {rec.rationale}
            </div>
          )}
        </div>

        {/* 4 Pillars Breakdown (F3, F4, Coverage, Normalized Priority) */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.85rem' }}>
          {/* Pillar 1: F3 Atmospheric Risk */}
          <div
            style={{
              padding: '0.85rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: 'var(--accent-amber)', marginBottom: '0.4rem' }}>
              <Shield size={14} />
              <span>F3 RISK COMPONENT</span>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {rec.riskScore.toFixed(2)}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
              Level: <strong style={{ color: 'var(--text-primary)' }}>{rec.riskLevel}</strong>
            </div>
            {rec.f3Confidence != null && (
              <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                Confidence: {(rec.f3Confidence * 100).toFixed(0)}%
              </div>
            )}
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
              Updated: {formattedPredTime}
            </div>
          </div>

          {/* Pillar 2: F4 Forecast Uncertainty */}
          <div
            style={{
              padding: '0.85rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: 'var(--brand-primary)', marginBottom: '0.4rem' }}>
              <TrendingUp size={14} />
              <span>F4 UNCERTAINTY</span>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {rec.uncertaintyIntervalWidth != null ? `${rec.uncertaintyIntervalWidth.toFixed(1)} µg/m³` : 'N/A'}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
              PM2.5: {rec.predictedPm25 != null ? `${rec.predictedPm25.toFixed(1)} µg/m³` : 'N/A'}
            </div>
            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
              Interval: [{rec.lowerBound?.toFixed(1) ?? '—'}, {rec.upperBound?.toFixed(1) ?? '—'}]
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
              Norm Uncertainty: {rec.normalizedUncertainty != null ? rec.normalizedUncertainty.toFixed(2) : rec.uncertainty.toFixed(2)}
            </div>
          </div>

          {/* Pillar 3: F8-P2 Monitoring Coverage */}
          <div
            style={{
              padding: '0.85rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: 'var(--accent-purple)', marginBottom: '0.4rem' }}>
              <Compass size={14} />
              <span>STATION COVERAGE</span>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {distanceVal != null ? `${distanceVal.toFixed(2)} km` : 'No Station'}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
              Nearest: <strong style={{ color: 'var(--text-primary)' }}>{rec.nearestStationCode || 'N/A'}</strong>
            </div>
            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
              Coverage Gap: <strong style={{ color: rec.monitoringCoverageGapFlag === 1 ? 'var(--accent-rose)' : 'var(--accent-emerald)' }}>
                {rec.monitoringCoverageGapFlag === 1 ? 'YES (>7 km)' : 'NO (<=7 km)'}
              </strong>
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
              Stations within 5km: {rec.stationsWithin5kmCount ?? 0}
            </div>
          </div>

          {/* Pillar 4: F8-P3 Multi-Criteria Priority */}
          <div
            style={{
              padding: '0.85rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface-elevated)',
              border: `1px solid ${priorityColor}30`,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: priorityColor, marginBottom: '0.4rem' }}>
              <Radio size={14} />
              <span>PRIORITY FORMULA</span>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: priorityColor }}>
              {rec.priorityScorePercent ?? Math.round(rec.priorityScore)}/100
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
              Classification: <strong>{rec.priorityLevel}</strong>
            </div>
            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
              Norm Risk: {rec.normalizedRisk?.toFixed(2) ?? rec.riskScore.toFixed(2)} (45%)
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
              Norm Dist: {rec.normalizedDistance?.toFixed(2) ?? '—'} (25%)
            </div>
          </div>
        </div>

        {/* Operational Disclaimer */}
        <div
          style={{
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
            lineHeight: 1.4,
            padding: '0.5rem 0.75rem',
            borderRadius: '6px',
            background: 'var(--bg-glass)',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.4rem',
          }}
        >
          <HelpCircle size={14} style={{ flexShrink: 0, marginTop: '0.1rem', color: 'var(--text-secondary)' }} />
          <span>
            <strong>Operational Note:</strong> Station coverage gap indicates physical observation distance from existing CAAQMS monitors; it does not confirm or prove localized emission causality.
          </span>
        </div>
      </div>
    </Card>
  );
};
