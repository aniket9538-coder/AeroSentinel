import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { PM25Chart } from '../../components/charts/PM25Chart';
import { AQIChart } from '../../components/charts/AQIChart';
import { WindChart } from '../../components/charts/WindChart';
import { Badge } from '../../components/common/Badge';

export const Dashboard: React.FC = () => {
  return (
    <PageContainer
      title="Public Air Quality Overview"
      subtitle="Real-time multi-station telemetry and ambient conditions"
      action={<Badge variant="success">LIVE STREAMING</Badge>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>City Average PM2.5</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f3f4f6', marginTop: '0.25rem' }}>
            68.4 <span style={{ fontSize: '0.9rem', color: '#9ca3af' }}>µg/m³</span>
          </div>
          <div style={{ marginTop: '0.4rem' }}>
            <Badge variant="warning">MODERATE</Badge>
          </div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Peak Station AQI</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f97316', marginTop: '0.25rem' }}>
            185
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.4rem' }}>Hadapsar Industrial Area</div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Ambient Temperature</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#38bdf8', marginTop: '0.25rem' }}>
            28.2°C
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.4rem' }}>Humidity: 62%</div>
        </Card>
        <Card>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>Active Fire Detections</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#ea580c', marginTop: '0.25rem' }}>
            3
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.4rem' }}>Within 25km Radius</div>
        </Card>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
        <Card title="24-Hour PM2.5 Concentration Trend">
          <PM25Chart />
        </Card>
        <Card title="Wind Vector & Dispersion">
          <WindChart />
        </Card>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
        <Card title="Station Comparative Air Quality Index (AQI)">
          <AQIChart />
        </Card>
      </div>
    </PageContainer>
  );
};
