import React, { useState, useEffect } from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { inspectionApi } from '../../services/inspectionApi';
import { FieldTeam, InspectionResponse, VerificationResult } from '../../types/inspection';
import { Users, CheckCircle, AlertTriangle, Clock, MapPin, FileText, Shield, ArrowRight, X } from 'lucide-react';

interface InspectionFormProps {
  alertId?: string;
  alertTitle?: string;
  eventCode?: string;
  h3Index?: string;
  currentAssignment?: InspectionResponse | null;
  onAssignmentSuccess?: (inspection: InspectionResponse) => void;
  onVerificationSuccess?: (inspection: InspectionResponse) => void;
  onScheduleInspection?: (data: any) => void;
  isLoading?: boolean;
  onClose?: () => void;
}

export const InspectionForm: React.FC<InspectionFormProps> = ({
  alertId,
  alertTitle,
  eventCode,
  h3Index,
  currentAssignment: initialAssignment = null,
  onAssignmentSuccess,
  onVerificationSuccess,
  onScheduleInspection,
  isLoading = false,
  onClose,
}) => {
  // State for Teams
  const [teams, setTeams] = useState<FieldTeam[]>([]);
  const [loadingTeams, setLoadingTeams] = useState<boolean>(true);
  const [teamError, setTeamError] = useState<string | null>(null);

  // Active Assignment state
  const [assignment, setAssignment] = useState<InspectionResponse | null>(initialAssignment);
  const [loadingAssignment, setLoadingAssignment] = useState<boolean>(false);

  // Form inputs for Assignment
  const [selectedTeamId, setSelectedTeamId] = useState<string>('');
  const [scheduledAt, setScheduledAt] = useState<string>(new Date().toISOString().slice(0, 16));
  const [assignmentNotes, setAssignmentNotes] = useState<string>('');

  // Form inputs for Verification
  const [verificationResult, setVerificationResult] = useState<VerificationResult>('CONFIRMED');
  const [observedConditions, setObservedConditions] = useState<string>('');
  const [inspectorNotes, setInspectorNotes] = useState<string>('');
  const [evidenceReferences, setEvidenceReferences] = useState<string>('');
  const [verifiedBy, setVerifiedBy] = useState<string>('');

  // Submission / Action states
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Fetch teams and active assignment on mount
  useEffect(() => {
    let isMounted = true;

    const fetchPrerequisites = async () => {
      setLoadingTeams(true);
      setTeamError(null);
      try {
        const teamList = await inspectionApi.getFieldTeams();
        if (isMounted) {
          setTeams(teamList);
          if (teamList.length > 0) {
            setSelectedTeamId(teamList[0].id);
          }
        }
      } catch (err: any) {
        if (isMounted) {
          setTeamError(err?.response?.data?.message || err?.message || 'Failed to load municipal field teams');
        }
      } finally {
        if (isMounted) setLoadingTeams(false);
      }

      if (alertId) {
        setLoadingAssignment(true);
        try {
          const active = await inspectionApi.getAlertAssignment(alertId);
          if (isMounted && active) {
            setAssignment(active);
            if (active.teamName && !verifiedBy) {
              setVerifiedBy(active.teamName);
            }
          }
        } catch (err: any) {
          // If no assignment found (404), active remains null
        } finally {
          if (isMounted) setLoadingAssignment(false);
        }
      }
    };

    fetchPrerequisites();
    return () => {
      isMounted = false;
    };
  }, [alertId]);

  // Handle Team Assignment Submission
  const handleAssignTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTeamId) {
      setErrorMessage('Please select a valid municipal field team.');
      return;
    }

    if (!alertId) {
      setErrorMessage('Cannot assign team: No alert candidate selected.');
      return;
    }

    setActionLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const resp = await inspectionApi.assignFieldTeam(alertId, {
        teamId: selectedTeamId,
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : new Date().toISOString(),
        notes: assignmentNotes.trim() || undefined,
      });
      setAssignment(resp);
      setSuccessMessage(`Field team ${resp.teamName || resp.teamCode} assigned successfully!`);
      if (onAssignmentSuccess) onAssignmentSuccess(resp);
      if (onScheduleInspection) onScheduleInspection({ assignedTeam: resp.teamName, alertId });
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to assign field team';
      setErrorMessage(msg);
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Start Inspection Transition
  const handleStartInspection = async () => {
    if (!assignment?.id) return;

    setActionLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const resp = await inspectionApi.startInspection(assignment.id);
      setAssignment(resp);
      setSuccessMessage('Field inspection started. Team is actively assessing the site.');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to start inspection';
      setErrorMessage(msg);
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Verification Submission
  const handleSubmitVerification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignment?.id) return;

    if (!observedConditions.trim()) {
      setErrorMessage('Observed field conditions cannot be empty. Please enter physical observations.');
      return;
    }

    setActionLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const resp = await inspectionApi.submitVerification(assignment.id, {
        verificationResult,
        observedConditions: observedConditions.trim(),
        inspectorNotes: inspectorNotes.trim() || undefined,
        evidenceReferences: evidenceReferences.trim() || undefined,
        verifiedBy: verifiedBy.trim() || undefined,
        inspectedAt: new Date().toISOString(),
      });
      setAssignment(resp);
      setSuccessMessage(`Field verification recorded as ${verificationResult}! Lineage preserved.`);
      if (onVerificationSuccess) onVerificationSuccess(resp);
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to submit verification';
      setErrorMessage(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const isAssigned = assignment && ['SCHEDULED', 'ASSIGNED'].includes(assignment.status);
  const isInProgress = assignment && assignment.status === 'IN_PROGRESS';
  const isCompleted = assignment && assignment.status === 'COMPLETED';

  return (
    <Card
      title="Field Team Assignment & Verification"
      subtitle="Operational physical ground inspection workflow"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
        {/* Header Metadata */}
        <div style={{
          background: '#0b0f17',
          padding: '0.75rem',
          borderRadius: '8px',
          border: '1px solid #1f2937',
          fontSize: '0.8rem',
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '0.5rem',
        }}>
          <div>
            <span style={{ color: '#6b7280', display: 'block' }}>Alert Reference</span>
            <span style={{ color: '#f3f4f6', fontWeight: 600 }}>{alertTitle || (alertId ? alertId.slice(0, 12) : 'No Alert Selected')}</span>
          </div>
          <div>
            <span style={{ color: '#6b7280', display: 'block' }}>Cell H3</span>
            <span style={{ color: '#60a5fa', fontFamily: 'monospace' }}>{h3Index || 'N/A'}</span>
          </div>
          {eventCode && (
            <div>
              <span style={{ color: '#6b7280', display: 'block' }}>Event Code</span>
              <span style={{ color: '#9ca3af', fontFamily: 'monospace' }}>{eventCode}</span>
            </div>
          )}
          <div>
            <span style={{ color: '#6b7280', display: 'block' }}>Workflow Status</span>
            <span style={{
              fontWeight: 700,
              color: isCompleted ? '#34d399' : isInProgress ? '#fbbf24' : isAssigned ? '#60a5fa' : '#9ca3af',
            }}>
              {assignment ? assignment.status : 'UNASSIGNED'}
            </span>
          </div>
        </div>

        {/* Notifications / Alerts */}
        {errorMessage && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.15)',
            border: '1px solid #ef4444',
            color: '#fca5a5',
            padding: '0.65rem 0.85rem',
            borderRadius: '6px',
            fontSize: '0.8rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}>
            <AlertTriangle size={16} />
            <span>{errorMessage}</span>
          </div>
        )}

        {successMessage && (
          <div style={{
            background: 'rgba(16, 185, 129, 0.15)',
            border: '1px solid #10b981',
            color: '#86efac',
            padding: '0.65rem 0.85rem',
            borderRadius: '6px',
            fontSize: '0.8rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}>
            <CheckCircle size={16} />
            <span>{successMessage}</span>
          </div>
        )}

        {/* PHASE 1: ASSIGN FIELD TEAM (When unassigned or starting new) */}
        {!assignment && (
          <form onSubmit={handleAssignTeam} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Select Municipal Field Team
              </label>
              {loadingTeams ? (
                <div style={{ color: '#9ca3af', fontSize: '0.8rem' }}>Loading verified field teams...</div>
              ) : teamError ? (
                <div style={{ color: '#f87171', fontSize: '0.8rem' }}>{teamError}</div>
              ) : teams.length === 0 ? (
                <div style={{ color: '#fbbf24', fontSize: '0.8rem', padding: '0.5rem', background: '#1f2937', borderRadius: '6px' }}>
                  No municipal field teams registered in system.
                </div>
              ) : (
                <select
                  value={selectedTeamId}
                  onChange={(e) => setSelectedTeamId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.55rem',
                    backgroundColor: '#1f2937',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#f3f4f6',
                    fontSize: '0.85rem',
                  }}
                >
                  {teams.map((t) => (
                    <option key={t.id} value={t.id}>
                      [{t.teamCode}] {t.teamName} ({t.status}) {t.leaderName ? `— Leader: ${t.leaderName}` : ''}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Scheduled Inspection Time
              </label>
              <input
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.55rem',
                  backgroundColor: '#1f2937',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '6px',
                  color: '#f3f4f6',
                  fontSize: '0.85rem',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Operational Notes / Dispatch Instructions
              </label>
              <textarea
                value={assignmentNotes}
                onChange={(e) => setAssignmentNotes(e.target.value)}
                placeholder="Specific guidance (e.g. check chimney stack scrubbers at industrial corridor)"
                rows={2}
                style={{
                  width: '100%',
                  padding: '0.55rem',
                  backgroundColor: '#1f2937',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '6px',
                  color: '#f3f4f6',
                  fontSize: '0.85rem',
                  resize: 'vertical',
                }}
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              disabled={actionLoading || loadingTeams || teams.length === 0}
              isLoading={actionLoading}
            >
              Assign Field Team
            </Button>
          </form>
        )}

        {/* PHASE 2: ASSIGNED -> READY TO START INSPECTION */}
        {isAssigned && (
          <div style={{
            background: 'rgba(59, 130, 246, 0.08)',
            border: '1px solid rgba(59, 130, 246, 0.3)',
            borderRadius: '8px',
            padding: '1rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#60a5fa', fontWeight: 600, fontSize: '0.85rem' }}>
              <Users size={16} /> Assigned Crew: {assignment.teamName || assignment.teamCode || 'Municipal Response Squad'}
            </div>
            <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
              Assigned At: {assignment.assignedAt ? new Date(assignment.assignedAt).toLocaleString() : 'Just now'}
            </div>
            {assignment.notes && (
              <div style={{ fontSize: '0.8rem', color: '#cbd5e1', background: '#111827', padding: '0.5rem', borderRadius: '4px' }}>
                <span style={{ color: '#6b7280' }}>Notes:</span> {assignment.notes}
              </div>
            )}
            <Button
              onClick={handleStartInspection}
              variant="secondary"
              disabled={actionLoading}
              isLoading={actionLoading}
            >
              Start Ground Inspection
            </Button>
          </div>
        )}

        {/* PHASE 3: IN PROGRESS -> SUBMIT VERIFICATION */}
        {isInProgress && (
          <form onSubmit={handleSubmitVerification} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            <div style={{
              background: 'rgba(251, 191, 36, 0.08)',
              border: '1px solid rgba(251, 191, 36, 0.3)',
              borderRadius: '6px',
              padding: '0.65rem 0.85rem',
              fontSize: '0.8rem',
              color: '#fef3c7',
            }}>
              <strong>Inspection Active:</strong> Field team is on-site. Record physical ground evidence below.
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Field Verification Result
              </label>
              <select
                value={verificationResult}
                onChange={(e) => setVerificationResult(e.target.value as VerificationResult)}
                style={{
                  width: '100%',
                  padding: '0.55rem',
                  backgroundColor: '#1f2937',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '6px',
                  color: '#f3f4f6',
                  fontSize: '0.85rem',
                }}
              >
                <option value="CONFIRMED">CONFIRMED — Field evidence supports pollution event</option>
                <option value="REJECTED">REJECTED — Field evidence does not support event (false alarm)</option>
                <option value="NEEDS_FOLLOW_UP">NEEDS_FOLLOW_UP — Source inaccessible / further investigation required</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Observed Field Conditions <span style={{ color: '#f87171' }}>*</span> (OBSERVED FIELD EVIDENCE)
              </label>
              <textarea
                value={observedConditions}
                onChange={(e) => setObservedConditions(e.target.value)}
                placeholder="Describe actual ground observations (e.g. dense black smoke from boiler stack, open biomass burning, odor)"
                rows={3}
                required
                style={{
                  width: '100%',
                  padding: '0.55rem',
                  backgroundColor: '#1f2937',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '6px',
                  color: '#f3f4f6',
                  fontSize: '0.85rem',
                  resize: 'vertical',
                }}
              />
              <span style={{ fontSize: '0.7rem', color: '#6b7280', marginTop: '0.2rem', display: 'block' }}>
                Observed conditions are classified as OBSERVED FIELD EVIDENCE only. Never ML prediction.
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                  Inspector Name / ID
                </label>
                <input
                  type="text"
                  value={verifiedBy}
                  onChange={(e) => setVerifiedBy(e.target.value)}
                  placeholder="e.g. Inspector A. Deshmukh"
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    backgroundColor: '#1f2937',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#f3f4f6',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                  Evidence References (Photos/Docs)
                </label>
                <input
                  type="text"
                  value={evidenceReferences}
                  onChange={(e) => setEvidenceReferences(e.target.value)}
                  placeholder="e.g. PHOTO-PUN-01, REPORT-42"
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    backgroundColor: '#1f2937',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#f3f4f6',
                    fontSize: '0.85rem',
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
                Regulatory Notes / Actions Taken
              </label>
              <textarea
                value={inspectorNotes}
                onChange={(e) => setInspectorNotes(e.target.value)}
                placeholder="Action taken (e.g. Issued stop-work notice under Air Act Sec 31A)"
                rows={2}
                style={{
                  width: '100%',
                  padding: '0.55rem',
                  backgroundColor: '#1f2937',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '6px',
                  color: '#f3f4f6',
                  fontSize: '0.85rem',
                  resize: 'vertical',
                }}
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              disabled={actionLoading || !observedConditions.trim()}
              isLoading={actionLoading}
            >
              Submit Field Verification
            </Button>
          </form>
        )}

        {/* PHASE 4: COMPLETED VERIFICATION DOSSIER */}
        {isCompleted && (
          <div style={{
            background: 'rgba(16, 185, 129, 0.08)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: '8px',
            padding: '1rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem',
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#34d399', fontWeight: 700, fontSize: '0.9rem' }}>
                <CheckCircle size={18} /> Field Verification Completed
              </div>
              <span style={{
                padding: '0.2rem 0.6rem',
                borderRadius: '9999px',
                fontSize: '0.75rem',
                fontWeight: 700,
                background: assignment.latestVerification?.verificationResult === 'CONFIRMED'
                  ? 'rgba(16, 185, 129, 0.2)'
                  : assignment.latestVerification?.verificationResult === 'REJECTED'
                  ? 'rgba(239, 68, 68, 0.2)'
                  : 'rgba(245, 158, 11, 0.2)',
                color: assignment.latestVerification?.verificationResult === 'CONFIRMED'
                  ? '#34d399'
                  : assignment.latestVerification?.verificationResult === 'REJECTED'
                  ? '#f87171'
                  : '#fbbf24',
              }}>
                {assignment.latestVerification?.verificationResult || 'VERIFIED'}
              </span>
            </div>

            {assignment.latestVerification?.observedConditions && (
              <div>
                <span style={{ color: '#6b7280', fontSize: '0.75rem', display: 'block', textTransform: 'uppercase' }}>
                  Observed Field Evidence
                </span>
                <p style={{ color: '#e5e7eb', fontSize: '0.85rem', margin: '0.25rem 0 0 0' }}>
                  {assignment.latestVerification.observedConditions}
                </p>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem', fontSize: '0.75rem', color: '#9ca3af' }}>
              <div>
                <span>Verified By:</span> <strong style={{ color: '#d1d5db' }}>{assignment.latestVerification?.verifiedBy || assignment.teamName || 'Officer'}</strong>
              </div>
              <div>
                <span>Completed At:</span> <strong style={{ color: '#d1d5db' }}>{assignment.completedAt ? new Date(assignment.completedAt).toLocaleString() : 'Recent'}</strong>
              </div>
            </div>
          </div>
        )}

        {/* Modal / Form Close */}
        {onClose && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <button
              onClick={onClose}
              style={{
                background: 'transparent',
                border: '1px solid #374151',
                borderRadius: '6px',
                color: '#9ca3af',
                padding: '0.4rem 0.8rem',
                fontSize: '0.8rem',
                cursor: 'pointer',
              }}
            >
              Close
            </button>
          </div>
        )}
      </div>
    </Card>
  );
};
