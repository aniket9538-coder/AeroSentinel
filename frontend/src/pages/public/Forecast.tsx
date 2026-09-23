import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { ForecastSummary } from '../../components/forecasting/ForecastSummary';
import { ForecastTimeline } from '../../components/forecasting/ForecastTimeline';
import { ForecastChart } from '../../components/charts/ForecastChart';
import { ForecastConfidence } from '../../components/forecasting/ForecastConfidence';
import { Card } from '../../components/common/Card';

export const Forecast: React.FC = () => {
  const sampleForecast = {
    h3Index: '8860144aa1fffff',
    generatedAt: new Date().toISOString(),
    unit: 'ug/m3',
    forecast: [
      { targetHour: 1, predictedPm25: 74, lowerBound: 66, upperBound: 82, confidence: 0.94 },
      { targetHour: 2, predictedPm25: 88, lowerBound: 76, upperBound: 100, confidence: 0.89 },
      { targetHour: 3, predictedPm25: 104, lowerBound: 88, upperBound: 120, confidence: 0.84 },
      { targetHour: 4, predictedPm25: 118, lowerBound: 98, upperBound: 138, confidence: 0.79 },
      { targetHour: 5, predictedPm25: 108, lowerBound: 86, upperBound: 130, confidence: 0.74 },
      { targetHour: 6, predictedPm25: 94, lowerBound: 72, upperBound: 116, confidence: 0.70 },
    ],
  };

  return (
    <PageContainer
      title="Short-Term (1–6h) Predictive Forecast"
      subtitle="Autoregressive gradient boosted ensemble projection for selected H3 cell"
    >
      <div style={{ marginBottom: '1.5rem' }}>
        <ForecastSummary forecast={sampleForecast} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
        <Card title="Predicted PM2.5 Trajectory & 95% Confidence Bounds">
          <ForecastChart forecast={sampleForecast.forecast} />
        </Card>
        <Card title="Model Certainty">
          <ForecastConfidence confidence={0.88} />
        </Card>
      </div>

      <Card title="Hourly Interval Predictions">
        <ForecastTimeline forecasts={sampleForecast.forecast} />
      </Card>
    </PageContainer>
  );
};
