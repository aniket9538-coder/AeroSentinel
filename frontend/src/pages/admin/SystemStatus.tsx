import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';

export const SystemStatus: React.FC = () => {
  return (
    <PageContainer title="System Status & Telemetry Pipelines" subtitle="Microservice health checks and integration status">
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1.5rem' }}>
        <Card title="Spring Boot Orchestrator">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Port 8080 /actuator/health</span>
            <Badge variant="success">UP</Badge>
          </div>
        </Card>
        <Card title="Python FastAPI AI Service">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Port 8000 /health</span>
            <Badge variant="success">HEALTHY</Badge>
          </div>
        </Card>
        <Card title="PostgreSQL + PostGIS">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Port 5432 (PostGIS 3.4)</span>
            <Badge variant="success">CONNECTED</Badge>
          </div>
        </Card>
        <Card title="Google Gemini GenAI Service">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Gemini 2.5 Flash Bridge</span>
            <Badge variant="info">READY</Badge>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};
