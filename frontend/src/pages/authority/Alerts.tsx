import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { AlertList } from '../../components/alerts/AlertList';

export const Alerts: React.FC = () => {
  const sampleAlerts = [
    {
      id: 'alt-1',
      h3Index: '8860144aa1fffff',
      severity: 'WARNING' as const,
      title: 'Agricultural Fire Plume Inversion',
      message: 'Projected spike above 150 µg/m³.',
      status: 'OPEN' as const,
      createdAt: new Date().toISOString(),
    },
    {
      id: 'alt-2',
      h3Index: '8860144ab3fffff',
      severity: 'CRITICAL' as const,
      title: 'Stagnant Industrial Emission',
      message: 'Persistent particulate threshold breach.',
      status: 'OPEN' as const,
      createdAt: new Date().toISOString(),
    },
  ];

  return (
    <PageContainer title="All Authority Alerts" subtitle="Active notifications requiring intervention">
      <AlertList alerts={sampleAlerts} />
    </PageContainer>
  );
};
