import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { CellForecast } from '../../types';
import { TrendingUp, AlertTriangle, ShieldCheck, Clock, BrainCircuit } from 'lucide-react';

interface ForecastSummaryProps {
  forecast?: CellForecast;
  currentPm25?: number;
}

export const ForecastSummary: React.FC<ForecastSummaryProps> = ({
  forecast,
  currentPm25 = 118,
}) => {
  const forecasts = forecast?.forecast || [
    { targetHour: 1, predictedPm25: 124, lowerBound: 114, upperBound: 134, confidence: 0.92 },
    { targetHour: 2, predictedPm25: 137, lowerBound: 122, upperBound: 152, confidence: 0.86 },
    { targetHour: 3, predictedPm25: 132, lowerBound: 112, upperBound: 152, confidence: 0.81 },
    { targetHour: 4, predictedPm25: 125, lowerBound: 101, upperBound: 149, confidence: 0.76 },
    { targetHour: 5, predictedPm25: 116, lowerBound: 88, upperBound: 144, confidence: 0.72 },
    { targetHour: 6, predictedPm25: 108, lowerBound: 78, upperBound: 138, confidence: 0.68 },
  ];

  const peak = forecasts.reduce((max, cur) => (cur.predictedPm25 > max.predictedPm25 ? cur : max), forecasts[0]);
  const avgConfidence = Math.round(
    (forecasts.reduce((acc, cur) => acc + cur.confidence, 0) / forecasts.length) * 100
  );

  return (
    <div>
      {/* Disclaimer / Model Header banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.65rem 1.25rem',
          borderRadius: '10px',
          background: 'rgba(245, 158, 11, 0.08)',
          border: '1px solid rgba(245, 158, 11, 0.25)',
          marginBottom: '1rem',
          fontSize: '0.825rem',
          color: 'var(--text-secondary)',
          flexWrap: 'wrap',
          gap: '0.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <BrainCircuit size={16} color="var(--accent-amber)" />
          <strong style={{ color: 'var(--accent-amber)' }}>Short-term model forecast</strong>
          <span>— Predictive outlook from spatial-temporal gradient boosted model. Not measured telemetry.</span>
        </div>
        <Badge variant="warning">Lead Time: 1–6 Hours</Badge>
      </div>

      {/* 4 KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
          gap: '1rem',
        }}
      >
        {/* KPI 1: Current PM2.5 */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Current PM2.5 (Observed)
            </span>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--brand-primary)' }} className="live-indicator-dot" />
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>
              {currentPm25}
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>µg/m³</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
            Ground CAAQMS sensor telemetry
          </div>
        </Card>

        {/* KPI 2: Peak Expected */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Peak Expected
            </span>
            <TrendingUp size={16} color="var(--accent-rose)" />
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent-rose)', fontFamily: 'var(--font-mono)' }}>
              {peak?.predictedPm25}
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>µg/m³</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--accent-rose)', marginTop: '0.35rem', fontWeight: 600 }}>
            +{((peak.predictedPm25 - currentPm25) / currentPm25 * 100).toFixed(0)}% projected elevation
          </div>
        </Card>

        {/* KPI 3: Peak Horizon */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Peak Horizon
            </span>
            <Clock size={16} color="var(--accent-amber)" />
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent-amber)', fontFamily: 'var(--font-mono)' }}>
              +{peak?.targetHour}h
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>hours ahead</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
            Optimal early intervention window
          </div>
        </Card>

        {/* KPI 4: Model Confidence */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Model Confidence
            </span>
            <ShieldCheck size={16} color="var(--accent-teal)" />
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent-teal)', fontFamily: 'var(--font-mono)' }}>
              {avgConfidence}%
            </span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
            Based on historical residual validation
          </div>
        </Card>
      </div>
    </div>
  );
};
