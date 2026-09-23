import React from 'react';
import { ConfidenceChart } from '../charts/ConfidenceChart';

interface ForecastConfidenceProps {
  confidence?: number;
}

export const ForecastConfidence: React.FC<ForecastConfidenceProps> = ({ confidence = 0.88 }) => {
  return (
    <div>
      <ConfidenceChart confidence={confidence} label="Ensemble Model Agreement" />
    </div>
  );
};
