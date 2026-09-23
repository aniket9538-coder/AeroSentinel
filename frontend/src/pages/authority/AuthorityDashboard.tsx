import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { IncidentQueue } from '../../components/authority/IncidentQueue';
import { ActionPanel } from '../../components/authority/ActionPanel';
import { InspectionForm } from '../../components/authority/InspectionForm';
import { ResolutionPanel } from '../../components/authority/ResolutionPanel';
import { AlertDetails } from '../../components/alerts/AlertDetails';
import { Alert } from '../../types';

export const AuthorityDashboard: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([
    {
      id: 'alt-101',
      h3Index: '8860144aa1fffff',
      severity: 'WARNING',
      title: 'Projected PM2.5 Spike & Waste Burning',
      message: 'Model projects PM2.5 exceeding 120 µg/m³ within 2 hours. Corroborated with active fire detection.',
      status: 'OPEN',
      createdAt: new Date().toISOString(),
    },
    {
      id: 'alt-102',
      h3Index: '8860144ab3fffff',
      severity: 'CRITICAL',
      title: 'Industrial Corridor Stagnation',
      message: 'Severe particulate buildup with zero dispersion in Hadapsar Industrial cluster.',
      status: 'OPEN',
      createdAt: new Date(Date.now() - 1800000).toISOString(),
    },
  ]);

  const [selectedAlert, setSelectedAlert] = useState<Alert>(alerts[0]);
  const [loading, setLoading] = useState(false);

  const handleLogAction = (data: any) => {
    setLoading(true);
    setTimeout(() => {
      alert(`Mitigation action recorded: ${data.actionType}`);
      setLoading(false);
    }, 800);
  };

  const handleScheduleInspection = (data: any) => {
    setLoading(true);
    setTimeout(() => {
      alert(`Inspection team dispatched: ${data.assignedTeam}`);
      setLoading(false);
    }, 800);
  };

  const handleResolveAlert = (alertId: string) => {
    setLoading(true);
    setTimeout(() => {
      setAlerts(alerts.filter((a) => a.id !== alertId));
      alert('Incident officially resolved and closed.');
      setLoading(false);
    }, 800);
  };

  return (
    <PageContainer
      title="Municipal Environmental Authority Hub"
      subtitle="Operational incident response, team dispatch, and mitigation tracking"
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1.5rem' }}>
        <div>
          <IncidentQueue
            alerts={alerts}
            selectedAlertId={selectedAlert?.id}
            onSelectAlert={(a) => setSelectedAlert(a)}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {selectedAlert && <AlertDetails alert={selectedAlert} />}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <InspectionForm
              alertId={selectedAlert?.id}
              onScheduleInspection={handleScheduleInspection}
              isLoading={loading}
            />
            <ActionPanel
              alertId={selectedAlert?.id}
              onLogAction={handleLogAction}
              isLoading={loading}
            />
          </div>
          <ResolutionPanel
            alertId={selectedAlert?.id}
            onResolve={handleResolveAlert}
            isLoading={loading}
          />
        </div>
      </div>
    </PageContainer>
  );
};
