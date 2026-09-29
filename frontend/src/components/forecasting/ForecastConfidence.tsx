import React from 'react';
import { ConfidenceChart } from '../charts/ConfidenceChart';
import { ShieldAlert } from 'lucide-react';

interface ForecastConfidenceProps {
  confidence?: number | null;
}

export const ForecastConfidence: React.FC<ForecastConfidenceProps> = ({ confidence }) => {
  if (confidence == null) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.6rem',
          padding: '0.75rem 1rem',
          borderRadius: '8px',
          background: 'var(--bg-surface-elevated)',
          border: '1px solid var(--border-subtle)',
          color: 'var(--text-secondary)',
          fontSize: '0.825rem',
        }}
      >
        <ShieldAlert size={18} color="var(--text-muted)" />
        <span>
          <strong>Forecast Confidence:</strong> Not available. Prediction ranges are provided instead.
        </span>
      </div>
    );
  }

  return (
    <div>
      <ConfidenceChart confidence={confidence} label="Forecast Model Agreement" />
    </div>
  );
};

export default ForecastConfidence;
