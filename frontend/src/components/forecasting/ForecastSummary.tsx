import React from 'react';
import { Card } from '../common/Card';
import { CellForecast } from '../../types';

interface ForecastSummaryProps {
  forecast?: CellForecast;
}

export const ForecastSummary: React.FC<ForecastSummaryProps> = ({ forecast }) => {
  const peakForecast = forecast?.forecast.reduce((max, cur) => (cur.predictedPm25 > max.predictedPm25 ? cur : max), forecast.forecast[0]);

  return (
    <Card title="Short-Term (1–6h) Predictive Summary">
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginTop: '0.75rem' }}>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Current Horizon</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f3f4f6' }}>+1 to +6 Hours</div>
        </div>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Projected Peak</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#38bdf8' }}>
            {peakForecast ? `${peakForecast.predictedPm25.toFixed(0)} µg/m³` : 'N/A'}
          </div>
        </div>
        <div style={{ padding: '0.75rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Confidence</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#10b981' }}>
            {peakForecast ? `${(peakForecast.confidence * 100).toFixed(0)}%` : 'N/A'}
          </div>
        </div>
      </div>
    </Card>
  );
};
