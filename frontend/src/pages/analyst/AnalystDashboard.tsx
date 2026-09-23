import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { PollutantTrendChart } from '../../components/charts/PollutantTrendChart';
import { PM25Chart } from '../../components/charts/PM25Chart';
import { WindChart } from '../../components/charts/WindChart';
import { Badge } from '../../components/common/Badge';

export const AnalystDashboard: React.FC = () => {
  return (
    <PageContainer
      title="Environmental Analyst Portal"
      subtitle="Deep-dive correlation analysis, multi-parameter trends, and model performance"
      action={<Badge variant="info">MODEL VERSION: v1.2-STABLE</Badge>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Forecast MAE</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#10b981', marginTop: '0.25rem' }}>
            6.2 <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>µg/m³</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.25rem' }}>1-Hour Ahead Horizon</div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Hotspot F1-Score</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#38bdf8', marginTop: '0.25rem' }}>
            0.87
          </div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.25rem' }}>Spike Detection (&gt;150 µg/m³)</div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Avg Advance Lead Time</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f59e0b', marginTop: '0.25rem' }}>
            2.4 <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>Hours</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.25rem' }}>Early Action Warning</div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Gemini Evidence Agreement</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#8b5cf6', marginTop: '0.25rem' }}>
            94.2%
          </div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.25rem' }}>Multimodal Corroboration</div>
        </Card>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
        <Card title="Multi-Pollutant Cross-Correlation (PM2.5, PM10, NO2)">
          <PollutantTrendChart />
        </Card>
        <Card title="Atmospheric Wind Vector Analysis">
          <WindChart />
        </Card>
      </div>
    </PageContainer>
  );
};
