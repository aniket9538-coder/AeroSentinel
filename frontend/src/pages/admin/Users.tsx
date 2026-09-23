import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';

export const UsersPage: React.FC = () => {
  return (
    <PageContainer title="User Management" subtitle="System roles and access control">
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0', borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
          <div>
            <div style={{ fontWeight: 600 }}>Officer Patil</div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>patil@pcmc.gov.in</div>
          </div>
          <Badge variant="warning">AUTHORITY</Badge>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0' }}>
          <div>
            <div style={{ fontWeight: 600 }}>Dr. S. Kulkarni</div>
            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>kulkarni@iitb.ac.in</div>
          </div>
          <Badge variant="info">ANALYST</Badge>
        </div>
      </Card>
    </PageContainer>
  );
};
