import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';

export const AdminDashboard: React.FC = () => {
  return (
    <PageContainer title="System Administration" subtitle="Manage municipalities, users, and platform health">
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.5rem' }}>
        <Card title="Active Cities" subtitle="Piloting deployment">
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#f3f4f6' }}>3</div>
          <div style={{ fontSize: '0.85rem', color: '#9ca3af', marginTop: '0.5rem' }}>
            Pune, Mumbai, Delhi
          </div>
        </Card>
        <Card title="Registered Users" subtitle="Citizens and authorities">
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#38bdf8' }}>1,248</div>
          <div style={{ fontSize: '0.85rem', color: '#9ca3af', marginTop: '0.5rem' }}>
            34 Municipal officers
          </div>
        </Card>
        <Card title="Database Health" subtitle="PostgreSQL + PostGIS">
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#10b981', marginTop: '0.5rem' }}>
            OPTIMAL (14ms)
          </div>
          <div style={{ marginTop: '0.5rem' }}>
            <Badge variant="success">FLYWAY V3 MIGRATED</Badge>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};
