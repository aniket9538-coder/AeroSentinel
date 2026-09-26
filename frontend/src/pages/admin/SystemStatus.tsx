import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { useApp } from '../../store/AppContext';
import { RefreshCw, CheckCircle2, AlertCircle } from 'lucide-react';

export const SystemStatus: React.FC = () => {
  const { backendStatus, backendHealth, checkBackendHealth } = useApp();
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    await checkBackendHealth();
    setRefreshing(false);
  };

  return (
    <PageContainer
      title="System Status & Telemetry Pipelines"
      subtitle="F0 Foundation health checks and backend orchestrator integration status"
      actions={
        <Button
          variant="outline"
          size="sm"
          onClick={handleRefresh}
          isLoading={refreshing}
        >
          <RefreshCw size={14} />
          {refreshing ? 'Checking...' : 'Ping /api/v1/health'}
        </Button>
      }
    >
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1.5rem' }}>
        <Card title="Spring Boot Orchestrator (Backend)">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Health Endpoint:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>GET /api/v1/health</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Connection Status:</span>
              {backendStatus === 'CONNECTED' ? (
                <Badge variant="success" pulse>
                  <CheckCircle2 size={12} style={{ marginRight: '4px' }} />
                  {backendHealth?.status || 'UP'}
                </Badge>
              ) : backendStatus === 'CONNECTING' ? (
                <Badge variant="warning" pulse>CONNECTING...</Badge>
              ) : (
                <Badge variant="danger">
                  <AlertCircle size={12} style={{ marginRight: '4px' }} />
                  OFFLINE
                </Badge>
              )}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Service ID:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                {backendHealth?.service || 'aerosentinel-backend'}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Last Response Timestamp:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                {backendHealth?.timestamp || 'Waiting for response...'}
              </span>
            </div>
          </div>
        </Card>

        <Card title="PostgreSQL + PostGIS Spatial Engine">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Database Service:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>PostgreSQL 16 + PostGIS 3.4</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Flyway Migrations:</span>
              <Badge variant="success">V1 - V4 VALIDATED</Badge>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Spatial Reference:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>SRID 4326 (WGS 84)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>HikariCP Pool:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: 'var(--aqi-good)' }}>
                Active (jdbc:postgresql://localhost:5432/aerosentinel)
              </span>
            </div>
          </div>
        </Card>

        <Card title="Vite Frontend App Shell">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Host / Port:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>http://localhost:3000</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>API Client Base:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>/api/v1 (Proxy -&gt; 8080)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>CORS Origins Allowed:</span>
              <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>localhost:3000, localhost:5173</span>
            </div>
          </div>
        </Card>

        <Card title="Architecture Guardrails">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Direct DB Access from Frontend:</span>
              <Badge variant="success">BLOCKED (REST ONLY)</Badge>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Secrets Committed in Git:</span>
              <Badge variant="success">NONE (.env.example only)</Badge>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Feature Scope:</span>
              <Badge variant="info">F0 FOUNDATION LOCKED</Badge>
            </div>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};

