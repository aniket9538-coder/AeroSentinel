import React, { useState, useEffect } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { InspectionForm } from '../../components/authority/InspectionForm';
import { alertApi } from '../../services/alertApi';
import { AuthorityQueueItem } from '../../types/alert';
import { Shield, AlertCircle, RefreshCw, CheckCircle2 } from 'lucide-react';

export const InspectionPage: React.FC = () => {
  const [alerts, setAlerts] = useState<AuthorityQueueItem[]>([]);
  const [selectedAlertId, setSelectedAlertId] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAlerts = async () => {
    setLoading(true);
    setError(null);
    try {
      const queue = await alertApi.getAuthorityQueue();
      // Show acknowledged or assigned alerts first, or all actionable alerts
      setAlerts(queue);
      if (queue.length > 0) {
        const preferred = queue.find(a => a.status === 'ACKNOWLEDGED') || queue[0];
        setSelectedAlertId(preferred.alertId);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load authority queue');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const selectedAlert = alerts.find(a => a.alertId === selectedAlertId);

  return (
    <PageContainer
      title="Field Team Assignment & Field Verification"
      subtitle="Operational physical ground inspection dispatch and observation recording"
    >
      <div style={{ maxWidth: '800px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {/* Alert Candidate Selector */}
        <div style={{
          background: '#111827',
          padding: '1.25rem',
          borderRadius: '10px',
          border: '1px solid #1f2937',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#e5e7eb' }}>
              Select Authority Alert Candidate for Field Inspection
            </label>
            <button
              onClick={fetchAlerts}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.35rem',
                background: 'transparent',
                border: '1px solid #374151',
                color: '#9ca3af',
                padding: '0.25rem 0.6rem',
                borderRadius: '6px',
                fontSize: '0.75rem',
                cursor: 'pointer',
              }}
            >
              <RefreshCw size={12} /> Refresh Queue
            </button>
          </div>

          {loading ? (
            <div style={{ color: '#9ca3af', fontSize: '0.85rem' }}>Loading authority queue...</div>
          ) : error ? (
            <div style={{ color: '#f87171', fontSize: '0.85rem' }}>{error}</div>
          ) : alerts.length === 0 ? (
            <div style={{ color: '#fbbf24', fontSize: '0.85rem' }}>
              No active alert candidates found in the Authority Queue.
            </div>
          ) : (
            <select
              value={selectedAlertId}
              onChange={(e) => setSelectedAlertId(e.target.value)}
              style={{
                width: '100%',
                padding: '0.6rem',
                backgroundColor: '#1f2937',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                borderRadius: '6px',
                color: '#f3f4f6',
                fontSize: '0.85rem',
              }}
            >
              {alerts.map((a) => (
                <option key={a.alertId} value={a.alertId}>
                  [{a.status}] {a.title} — H3: {a.h3Index.substring(0, 10)}... (Evidence: {a.evidenceScore?.toFixed(2)})
                  {a.assignedTeamName ? ` | Crew: ${a.assignedTeamName}` : ''}
                  {a.verificationResult ? ` | Result: ${a.verificationResult}` : ''}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Real Live Inspection Form */}
        {selectedAlert ? (
          <InspectionForm
            alertId={selectedAlert.alertId}
            alertTitle={selectedAlert.title}
            eventCode={selectedAlert.eventCode}
            h3Index={selectedAlert.h3Index}
            onAssignmentSuccess={() => fetchAlerts()}
            onVerificationSuccess={() => fetchAlerts()}
          />
        ) : (
          !loading && (
            <div style={{
              background: '#111827',
              padding: '2rem',
              borderRadius: '10px',
              textAlign: 'center',
              color: '#9ca3af',
              fontSize: '0.9rem',
              border: '1px dashed #374151',
            }}>
              Please select or create an acknowledged authority alert to proceed with field verification.
            </div>
          )
        )}
      </div>
    </PageContainer>
  );
};

export default InspectionPage;
