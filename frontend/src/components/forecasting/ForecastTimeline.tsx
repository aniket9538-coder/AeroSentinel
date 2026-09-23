import React from 'react';
import { HourlyForecast } from '../../types';

interface ForecastTimelineProps {
  forecasts?: HourlyForecast[];
}

export const ForecastTimeline: React.FC<ForecastTimelineProps> = ({ forecasts = [] }) => {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(100px, 1fr))', gap: '0.75rem' }}>
      {forecasts.map((f) => (
        <div
          key={f.targetHour}
          style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '8px',
            padding: '0.75rem',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>+{f.targetHour} Hour</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#38bdf8', margin: '0.25rem 0' }}>
            {f.predictedPm25}
          </div>
          <div style={{ fontSize: '0.7rem', color: '#6b7280' }}>µg/m³</div>
        </div>
      ))}
    </div>
  );
};
