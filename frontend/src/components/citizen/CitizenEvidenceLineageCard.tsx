import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { CitizenReport } from '../../types';
import { EvidenceSummaryResponse } from '../../types/evidence';
import {
  GitCommit,
  Layers,
  MapPin,
  FileText,
  Activity,
  ArrowRight,
  Database,
  ShieldAlert,
  Info,
  ExternalLink,
  CheckCircle2,
  Clock,
} from 'lucide-react';

interface CitizenEvidenceLineageCardProps {
  report: CitizenReport;
  evidenceSummary?: EvidenceSummaryResponse | null;
  isLoadingEvidence?: boolean;
}

export const CitizenEvidenceLineageCard: React.FC<CitizenEvidenceLineageCardProps> = ({
  report,
  evidenceSummary,
  isLoadingEvidence = false,
}) => {
  const navigate = useNavigate();

  const reportRef =
    report.reportId || (report.id ? `CR-${report.id.slice(0, 8).toUpperCase()}` : 'CR-LOCAL');
  const h3Index = report.h3Index || '';
  const observedTime = report.observedAt || report.submittedAt || new Date().toISOString();

  // Matched event details from live evidenceSummary or report context
  const matchedEventId = evidenceSummary?.context?.eventId;
  const matchedEventCode = evidenceSummary?.context?.eventCode;
  const evidenceScore = evidenceSummary?.evidence?.evidenceScore;
  const triageState = evidenceSummary?.evidence?.triageState;

  // Find if citizen evidence signal is registered in live evidence signals
  const citizenSignal = evidenceSummary?.evidence?.signals?.find(
    (s) =>
      s.dataSource === 'CITIZEN' ||
      s.sourceType === 'CITIZEN_OBSERVATION' ||
      s.sourceRef === report.id
  );

  const hasMatchedEvent = Boolean(matchedEventId || matchedEventCode || citizenSignal);

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '8px',
              background: 'rgba(56, 189, 248, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--brand-primary)',
            }}
          >
            <GitCommit size={18} />
          </div>
          <div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Citizen Evidence & Event Lineage
            </div>
            <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 400 }}>
              Spatial binding to F5 environmental intelligence pipeline
            </div>
          </div>
        </div>
      }
      subtitle="Auxiliary ground corroboration linked to authoritative H3 spatial grid"
      badge={
        hasMatchedEvent ? (
          <Badge variant="info">EVENT MATCHED</Badge>
        ) : (
          <Badge variant="neutral">STORED AUXILIARY</Badge>
        )
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '0.75rem' }}>
        {/* =========================================================================
            SECTION 8: CITIZEN EVIDENCE SPECIFICATION
            ========================================================================= */}
        <div
          style={{
            background: 'var(--bg-surface-elevated, #ffffff)',
            borderRadius: '10px',
            border: '1px solid var(--border-subtle, #e2e8f0)',
            padding: '1.25rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.85rem',
          }}
        >
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderBottom: '1px solid var(--border-subtle, #e2e8f0)',
              paddingBottom: '0.6rem',
            }}
          >
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Citizen Visual Evidence
            </span>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                padding: '0.2rem 0.55rem',
                borderRadius: '4px',
                background: report.status === 'VERIFIED' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                color: report.status === 'VERIFIED' ? 'var(--accent-teal, #10b981)' : 'var(--accent-amber, #f59e0b)',
                border: report.status === 'VERIFIED' ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid rgba(245, 158, 11, 0.3)',
              }}
            >
              {report.status === 'VERIFIED' ? 'VERIFIED' : 'UNVERIFIED CITIZEN REPORT'}
            </span>
          </div>

          {/* Evidence Attributes Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '0.85rem',
            }}
          >
            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Report Reference</div>
              <strong style={{ fontSize: '0.875rem', color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>
                {reportRef}
              </strong>
            </div>

            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>H3 Spatial Cell (Res 8)</div>
              <strong style={{ fontSize: '0.875rem', color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                {h3Index || 'Derived on server ingestion'}
              </strong>
            </div>

            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Data Source</div>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  padding: '0.15rem 0.45rem',
                  borderRadius: '4px',
                  background: 'rgba(56, 189, 248, 0.12)',
                  color: 'var(--brand-primary)',
                  display: 'inline-block',
                  marginTop: '0.15rem',
                }}
              >
                CITIZEN
              </span>
            </div>

            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Relevance Tier</div>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  padding: '0.15rem 0.45rem',
                  borderRadius: '4px',
                  background: 'rgba(168, 85, 247, 0.12)',
                  color: 'var(--accent-purple, #a855f7)',
                  display: 'inline-block',
                  marginTop: '0.15rem',
                }}
                title="Auxiliary evidence corroborates sensor trends but cannot independently trigger an alert"
              >
                AUXILIARY
              </span>
            </div>

            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Visual Category</div>
              <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                {report.visionAnalysis?.detectedCategory || report.category || 'UNKNOWN'}
              </strong>
            </div>

            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Observation Confidence</div>
              <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                {report.visionAnalysis?.confidence !== undefined
                  ? `${Math.round(report.visionAnalysis.confidence * 100)}%`
                  : 'N/A'}
              </strong>
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Observation Timestamp</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.15rem' }}>
                <Clock size={12} />
                <span>{new Date(observedTime).toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>

        {/* =========================================================================
            SECTION 9: EVENT LINEAGE (WHEN MATCHED EVENT EXISTS)
            ========================================================================= */}
        {hasMatchedEvent ? (
          <div
            style={{
              background: 'rgba(56, 189, 248, 0.05)',
              borderRadius: '10px',
              border: '1px solid rgba(56, 189, 248, 0.25)',
              padding: '1.25rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '1rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Layers size={16} style={{ color: 'var(--brand-primary)' }} />
                <span style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Active Environmental Lineage
                </span>
              </div>
              {matchedEventCode && (
                <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--brand-primary)', fontWeight: 600 }}>
                  {matchedEventCode}
                </span>
              )}
            </div>

            {/* Stepped Progression Flow */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.5rem',
                padding: '0.75rem',
                background: 'var(--bg-surface-elevated, #ffffff)',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle, #e2e8f0)',
                fontSize: '0.78rem',
              }}
            >
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>STEP 1</span>
                <strong style={{ color: 'var(--brand-primary)' }}>Citizen Report</strong>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>{reportRef}</span>
              </div>

              <ArrowRight size={14} style={{ color: 'var(--text-muted)', margin: '0 0.25rem' }} />

              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>STEP 2</span>
                <strong style={{ color: 'var(--text-primary)' }}>H3 Cell</strong>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                  {h3Index ? `${h3Index.slice(0, 10)}...` : 'Derived on ingestion'}
                </span>
              </div>

              <ArrowRight size={14} style={{ color: 'var(--text-muted)', margin: '0 0.25rem' }} />

              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>STEP 3</span>
                <strong style={{ color: 'var(--text-primary)' }}>Pollution Event</strong>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>{matchedEventCode ? matchedEventCode.slice(0, 12) + '...' : 'ACTIVE'}</span>
              </div>

              <ArrowRight size={14} style={{ color: 'var(--text-muted)', margin: '0 0.25rem' }} />

              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>STEP 4</span>
                <strong style={{ color: 'var(--accent-purple, #a855f7)' }}>Event Evidence</strong>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                  {citizenSignal?.signalId || `sig-citizen-${report.id.slice(0, 8)}`}
                </span>
              </div>

              <ArrowRight size={14} style={{ color: 'var(--text-muted)', margin: '0 0.25rem' }} />

              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>STEP 5</span>
                <strong style={{ color: 'var(--accent-teal, #10b981)' }}>F5 Evaluation</strong>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                  Score: {evidenceScore !== undefined ? evidenceScore.toFixed(3) : 'Live'}
                </span>
              </div>
            </div>

            {/* Lineage Details */}
            <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.45 }}>
              This citizen observation is attached as <strong>AUXILIARY</strong> corroboration to active event{' '}
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{matchedEventCode || matchedEventId}</span>.
              {triageState && (
                <span> Current F5 Triage State: <strong>{triageState}</strong>.</span>
              )}
            </div>

            {/* Direct Link to F5 Evidence & WHY experience */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '0.25rem' }}>
              <Button
                variant="outline"
                size="sm"
                disabled={!h3Index}
                onClick={() => h3Index && navigate(`/analyst/evidence?h3=${encodeURIComponent(h3Index)}`)}
                style={{ fontSize: '0.78rem' }}
              >
                Inspect F5 Evidence & WHY Dossier <ExternalLink size={13} style={{ marginLeft: '0.35rem' }} />
              </Button>
            </div>
          </div>
        ) : (
          /* =========================================================================
              SECTION 10: NO MATCH STATE (NO FABRICATED EVENT)
              ========================================================================= */
          <div
            style={{
              background: 'var(--bg-surface-elevated, #f8fafc)',
              borderRadius: '10px',
              border: '1px solid var(--border-subtle, #e2e8f0)',
              padding: '1.25rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.6rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)' }}>
              <Database size={16} />
              <strong style={{ fontSize: '0.875rem' }}>Citizen evidence stored</strong>
            </div>

            <div style={{ fontSize: '0.825rem', color: 'var(--text-primary)', fontWeight: 500 }}>
              No matching pollution event is currently available for this spatial/temporal context.
            </div>

            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', lineHeight: 1.45 }}>
              Your observation is securely registered with H3 cell{' '}
              <span style={{ fontFamily: 'var(--font-mono)' }}>{h3Index || 'Derived on server ingestion'}</span>.
              The evidence remains stored for future contextual evaluation. When stationary air quality monitors
              or satellite telemetry detect an anomaly in this spatial sector, this record will be automatically evaluated
              as auxiliary evidence.
            </div>

            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', fontStyle: 'italic', marginTop: '0.25rem' }}>
              Locked Safety Invariant: Citizen observations alone never fabricate a synthetic event or elevate an alert candidate state.
            </div>
          </div>
        )}
      </div>
    </Card>
  );
};
