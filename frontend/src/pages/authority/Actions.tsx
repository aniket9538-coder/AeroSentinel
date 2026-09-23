import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { ActionPanel } from '../../components/authority/ActionPanel';

export const ActionsPage: React.FC = () => {
  return (
    <PageContainer title="Mitigation Action Log" subtitle="Record interventions and compliance actions">
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <ActionPanel alertId="sample-alert" onLogAction={() => alert('Mitigation action recorded!')} />
      </div>
    </PageContainer>
  );
};
