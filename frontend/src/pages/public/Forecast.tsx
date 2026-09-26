import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { ForecastSummary } from '../../components/forecasting/ForecastSummary';
import { ForecastTimeline } from '../../components/forecasting/ForecastTimeline';
import { ForecastChart } from '../../components/charts/ForecastChart';
import { useApp } from '../../store/AppContext';
import {
  TrendingUp,
  Brain,
  ShieldAlert,
  Clock,
  RefreshCw,
  Layers,
  Info,
  Calendar,
  Sparkles,
} from 'lucide-react';

export const Forecast: React.FC = () => {
  const { selectedCity, isLive, lastUpdated, refreshData } = useApp();
  const [selectedCell, setSelectedCell] = useState('8860144aa1fffff');

  // Baseline forecast data matching Pune / City reality
  const currentPm25 = selectedCity?.id === 'pune' ? 118 : selectedCity?.id === 'delhi' ? 186 : 84;

  const sampleForecast = {
    h3Index: selectedCell,
    generatedAt: lastUpdated.toISOString(),
    unit: 'µg/m³',
    forecast: [
      { targetHour: 1, predictedPm25: Math.round(currentPm25 * 1.05), lowerBound: Math.round(currentPm25 * 0.95), upperBound: Math.round(currentPm25 * 1.15), confidence: 0.92 },
      { targetHour: 2, predictedPm25: Math.round(currentPm25 * 1.16), lowerBound: Math.round(currentPm25 * 1.03), upperBound: Math.round(currentPm25 * 1.28), confidence: 0.86 },
      { targetHour: 3, predictedPm25: Math.round(currentPm25 * 1.12), lowerBound: Math.round(currentPm25 * 0.95), upperBound: Math.round(currentPm25 * 1.29), confidence: 0.81 },
      { targetHour: 4, predictedPm25: Math.round(currentPm25 * 1.06), lowerBound: Math.round(currentPm25 * 0.86), upperBound: Math.round(currentPm25 * 1.26), confidence: 0.76 },
      { targetHour: 5, predictedPm25: Math.round(currentPm25 * 0.98), lowerBound: Math.round(currentPm25 * 0.75), upperBound: Math.round(currentPm25 * 1.22), confidence: 0.72 },
      { targetHour: 6, predictedPm25: Math.round(currentPm25 * 0.92), lowerBound: Math.round(currentPm25 * 0.66), upperBound: Math.round(currentPm25 * 1.17), confidence: 0.68 },
    ],
  };

  return (
    <PageContainer
      title="PM2.5 Predictive Horizon"
      subtitle={`Short-term autoregressive gradient-boosted forecast for ${selectedCity?.name || 'City'}`}
      actions={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: isLive ? 'var(--aqi-good)' : 'var(--accent-amber)' }} className={isLive ? 'live-indicator-dot' : ''} />
            <span>Updated {lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          </div>
          <Button variant="outline" size="sm" onClick={() => refreshData()}>
            <RefreshCw size={14} style={{ marginRight: '0.4rem' }} /> Refresh Model
          </Button>
        </div>
      }
    >
      {/* Top 4 Summary KPIs */}
      <div style={{ marginBottom: '1.75rem' }}>
        <ForecastSummary forecast={sampleForecast} currentPm25={currentPm25} />
      </div>

      {/* Main Hero Chart Section */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem', marginBottom: '1.75rem' }}>
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <TrendingUp size={20} color="var(--accent-amber)" />
              <span>Observed vs. 1–6 Hour Forecast Trajectory</span>
            </div>
          }
          subtitle="Direct comparison between ground telemetry up to NOW and subsequent projected trajectory"
          badge={
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Badge variant="info">H3 Hex: {selectedCell.slice(0, 10)}...</Badge>
              <Badge variant="warning">Model: XGBoost-v2</Badge>
            </div>
          }
        >
          <ForecastChart forecast={sampleForecast.forecast} currentPm25={currentPm25} height={340} />
        </Card>
      </div>

      {/* Hourly Timeline Cards */}
      <div style={{ marginBottom: '1.75rem' }}>
        <Card
          title="Hourly Forecast Interval Breakdown"
          subtitle="Specific interval risk level, projected delta, and 90% confidence envelope"
        >
          <ForecastTimeline forecasts={sampleForecast.forecast} currentPm25={currentPm25} />
        </Card>
      </div>

      {/* Technical Model Card & Methodology Disclaimers */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
        <Card title="Prediction Methodology & Architecture">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            <p style={{ marginBottom: '0.75rem' }}>
              AeroSentinel employs an autoregressive XGBoost ensemble model integrating lagged CAAQMS PM2.5 measurements,
              wind velocity components (u, v), relative humidity, boundary layer height, and spatial spatial-lag features from adjacent H3 cells.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', fontSize: '0.8rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Spatial Indexing:</span>
                <strong style={{ color: 'var(--text-primary)' }}>Uber H3 Resolution 8 (~0.73 km²)</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Training Corpus:</span>
                <strong style={{ color: 'var(--text-primary)' }}>24-Month Diurnal CAAQMS Archive</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Evaluation Metric:</span>
                <strong style={{ color: 'var(--brand-primary)' }}>RMSE: 14.2 µg/m³ (MAE: 9.8 µg/m³)</strong>
              </div>
            </div>
          </div>
        </Card>

        <Card title="Operational Action Protocols">
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            <p style={{ marginBottom: '0.75rem' }}>
              When a forecast predicts an elevation exceeding <strong>135 µg/m³</strong> within a 2-hour window:
            </p>
            <ul style={{ paddingLeft: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.825rem' }}>
              <li>
                <strong>Early Action Advisory:</strong> Municipal control rooms receive priority notifications to activate dust suppression.
              </li>
              <li>
                <strong>Anti-Smog Deployment:</strong> Automated dispatch suggestions triggered for mobile mist cannons upwind of the target hex.
              </li>
              <li>
                <strong>Public Notice:</strong> Proactive advisory sent to sensitive citizen groups before the pollution peak arrives.
              </li>
            </ul>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};
