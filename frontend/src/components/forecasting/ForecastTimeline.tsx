import React from 'react';
import { HourlyForecast } from '../../types';
import { Badge } from '../common/Badge';
import { TrendingUp, AlertTriangle, Clock } from 'lucide-react';

interface ForecastTimelineProps {
  forecasts?: HourlyForecast[];
  currentPm25?: number;
}

export const ForecastTimeline: React.FC<ForecastTimelineProps> = ({
  forecasts = [
    { targetHour: 1, predictedPm25: 124, lowerBound: 114, upperBound: 134, confidence: 0.92 },
    { targetHour: 2, predictedPm25: 137, lowerBound: 122, upperBound: 152, confidence: 0.86 },
    { targetHour: 3, predictedPm25: 132, lowerBound: 112, upperBound: 152, confidence: 0.81 },
    { targetHour: 4, predictedPm25: 125, lowerBound: 101, upperBound: 149, confidence: 0.76 },
    { targetHour: 5, predictedPm25: 116, lowerBound: 88, upperBound: 144, confidence: 0.72 },
    { targetHour: 6, predictedPm25: 108, lowerBound: 78, upperBound: 138, confidence: 0.68 },
  ],
  currentPm25 = 118,
}) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
        gap: '1rem',
      }}
    >
      {forecasts.map((f) => {
        const isPeak = f.predictedPm25 >= 135;
        const isHighRisk = f.predictedPm25 >= 120;
        const delta = f.predictedPm25 - currentPm25;

        return (
          <div
            key={f.targetHour}
            style={{
              padding: '1.25rem 1rem',
              borderRadius: '12px',
              background: isPeak
                ? 'rgba(239, 68, 68, 0.08)'
                : isHighRisk
                ? 'rgba(245, 158, 11, 0.06)'
                : 'var(--bg-surface-elevated)',
              border: isPeak
                ? '1px solid rgba(239, 68, 68, 0.35)'
                : isHighRisk
                ? '1px solid rgba(245, 158, 11, 0.3)'
                : '1px solid var(--border-subtle)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              position: 'relative',
              overflow: 'hidden',
              boxShadow: isPeak ? '0 0 16px rgba(239, 68, 68, 0.15)' : 'none',
              transition: 'transform 0.15s ease',
            }}
          >
            {/* Top Row: Horizon + Risk Badge */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: 'var(--text-muted)', fontSize: '0.8rem', fontWeight: 600 }}>
                <Clock size={12} />
                <span>+{f.targetHour}h</span>
              </div>
              {isPeak ? (
                <span
                  style={{
                    fontSize: '0.65rem',
                    fontWeight: 700,
                    padding: '0.15rem 0.45rem',
                    borderRadius: '4px',
                    background: 'rgba(239, 68, 68, 0.2)',
                    color: 'var(--accent-rose)',
                    letterSpacing: '0.04em',
                  }}
                >
                  HIGH RISK
                </span>
              ) : isHighRisk ? (
                <span
                  style={{
                    fontSize: '0.65rem',
                    fontWeight: 700,
                    padding: '0.15rem 0.45rem',
                    borderRadius: '4px',
                    background: 'rgba(245, 158, 11, 0.2)',
                    color: 'var(--accent-amber)',
                    letterSpacing: '0.04em',
                  }}
                >
                  ELEVATED
                </span>
              ) : (
                <span
                  style={{
                    fontSize: '0.65rem',
                    fontWeight: 600,
                    padding: '0.15rem 0.45rem',
                    borderRadius: '4px',
                    background: 'rgba(16, 185, 129, 0.15)',
                    color: 'var(--accent-teal)',
                  }}
                >
                  MODERATE
                </span>
              )}
            </div>

            {/* PM2.5 Prediction Value */}
            <div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.3rem' }}>
                <span
                  style={{
                    fontSize: '1.75rem',
                    fontWeight: 800,
                    fontFamily: 'var(--font-mono)',
                    color: isPeak ? 'var(--accent-rose)' : isHighRisk ? 'var(--accent-amber)' : 'var(--text-primary)',
                  }}
                >
                  {f.predictedPm25}
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>µg/m³</span>
              </div>

              {/* Delta vs now */}
              <div style={{ fontSize: '0.72rem', color: delta > 0 ? 'var(--accent-rose)' : 'var(--accent-teal)', marginTop: '0.2rem', fontWeight: 600 }}>
                {delta > 0 ? `+${delta} µg vs now` : `${delta} µg vs now`}
              </div>
            </div>

            {/* Bottom details: Bounds + Confidence */}
            <div
              style={{
                marginTop: '0.85rem',
                paddingTop: '0.65rem',
                borderTop: '1px solid var(--border-subtle)',
                fontSize: '0.72rem',
                color: 'var(--text-secondary)',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.2rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>90% range:</span>
                <strong style={{ color: 'var(--text-primary)' }}>{f.lowerBound}–{f.upperBound}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Confidence:</span>
                <strong style={{ color: 'var(--brand-primary)' }}>{Math.round(f.confidence * 100)}%</strong>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};
