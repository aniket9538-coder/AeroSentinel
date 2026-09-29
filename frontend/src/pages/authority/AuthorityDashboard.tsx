import React, { useState, useEffect } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { IncidentQueue } from '../../components/authority/IncidentQueue';
import { ActionPanel } from '../../components/authority/ActionPanel';
import { InspectionForm } from '../../components/authority/InspectionForm';
import { ResolutionPanel } from '../../components/authority/ResolutionPanel';
import { AlertDetails } from '../../components/alerts/AlertDetails';
import { Alert } from '../../types';
import alertApi from '../../services/alertApi';

export const AuthorityDashboard: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [selectedAlert, setSelectedAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const items = await alertApi.getAuthorityQueue();
      const mapped: Alert[] = items.map((item) => ({
        id: item.alertId,
        eventId: item.eventId,
        h3Index: item.h3Index,
        severity: item.severity === 'CRITICAL' ? 'CRITICAL' : item.severity === 'LOW' ? 'INFO' : 'WARNING',
        title: item.title,
        message: item.message,
        status: item.status,
        createdAt: item.createdAt,
        acknowledgedAt: item.acknowledgedAt || undefined,
      }));
      setAlerts(mapped);
      if (mapped.length > 0) {
        setSelectedAlert(mapped[0]);
      } else {
        setSelectedAlert(null);
      }
    } catch {
      setAlerts([]);
      setSelectedAlert(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleLogAction = (data: any) => {
    setActionLoading(true);
    setTimeout(() => {
      alert(`Mitigation action recorded: ${data.actionType}`);
      setActionLoading(false);
    }, 800);
  };

  const handleScheduleInspection = (data: any) => {
    setActionLoading(true);
    setTimeout(() => {
      alert(`Inspection team dispatched: ${data.assignedTeam}`);
      setActionLoading(false);
    }, 800);
  };

  const handleResolveAlert = async (alertId: string) => {
    setActionLoading(true);
    try {
      await alertApi.resolveAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.id !== alertId));
      setSelectedAlert((prev) => (prev?.id === alertId ? null : prev));
      alert('Incident officially resolved and closed.');
    } catch {
      alert('Failed to resolve alert.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <PageContainer
      title="Municipal Environmental Authority Hub"
      subtitle="Operational incident response, team dispatch, and mitigation tracking"
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#9ca3af' }}>
          Loading authority incidents...
        </div>
      ) : alerts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#9ca3af', background: '#111827', borderRadius: '12px' }}>
          No active authority alert candidates requiring intervention.
        </div>
      ) : (
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
                isLoading={actionLoading}
              />
              <ActionPanel
                alertId={selectedAlert?.id}
                onLogAction={handleLogAction}
                isLoading={actionLoading}
              />
            </div>
            <ResolutionPanel
              alertId={selectedAlert?.id}
              onResolve={handleResolveAlert}
              isLoading={actionLoading}
            />
          </div>
        </div>
      )}
    </PageContainer>
  );
};

export default AuthorityDashboard;
