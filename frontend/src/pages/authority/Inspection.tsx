import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { InspectionForm } from '../../components/authority/InspectionForm';

export const InspectionPage: React.FC = () => {
  return (
    <PageContainer title="Field Verification & Inspections" subtitle="Mobile response scheduling">
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <InspectionForm alertId="sample-alert" onScheduleInspection={() => alert('Inspection scheduled!')} />
      </div>
    </PageContainer>
  );
};
