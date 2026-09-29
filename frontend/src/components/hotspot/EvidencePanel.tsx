import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { EvidenceSummaryResponse } from '../../types/evidence';
import {
  Sparkles,
  ShieldCheck,
  AlertTriangle,
  Wind,
  Flame,
  Layers,
  MapPin,
  Clock,
  Activity,
  CheckCircle2,
  XCircle,
  HelpCircle,
  TrendingUp,
  Cpu,
  Eye,
  RefreshCw,
  FileText,
  Compass,
  Building2,
  Radio,
  ChevronDown,
  ChevronUp,
  Copy,
  Check,
  ArrowRight,
  ExternalLink,
  ShieldAlert,
  Camera,
  Users,
} from 'lucide-react';

interface EvidencePanelProps {
  evidence: EvidenceSummaryResponse | null;
  isLoading?: boolean;
  error?: string | null;
  onRefresh?: () => void;
  selectedH3?: string | null;
}

export const EvidencePanel: React.FC<EvidencePanelProps> = ({
  evidence,
  isLoading = false,
  error = null,
  onRefresh,
  selectedH3,
}) => {
  const [activeSection, setActiveSection] = useState<string>('evidence');
  const [scoreBreakdownOpen, setScoreBreakdownOpen] = useState<boolean>(false);
  const [technicalDetailsOpen, setTechnicalDetailsOpen] = useState<boolean>(false);
  const [safeguardsOpen, setSafeguardsOpen] = useState<boolean>(false);
  const [provenanceOpen, setProvenanceOpen] = useState<boolean>(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const handleCopy = (key: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1800);
  };

  const scrollToAnchor = (id: string) => {
    setActiveSection(id);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  // 1. Loading State
  if (isLoading) {
    return (
      <Card title="Multi-Source Evidence Synthesis & Grounded WHY">
        <div style={{ padding: '3.5rem 1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              border: '3px solid var(--border-medium)',
              borderTopColor: 'var(--brand-primary)',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 1.25rem',
            }}
          />
          <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Synthesizing Evidence & Diagnostics
          </div>
          <div style={{ fontSize: '0.825rem', marginTop: '0.4rem', color: 'var(--text-secondary)' }}>
            Querying F3 hotspot context, locked F4 forecast horizons, and multi-source corroboration signals for{' '}
            <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--brand-primary)' }}>
              {selectedH3 || 'selected cell'}
            </code>...
          </div>
        </div>
      </Card>
    );
  }

  // 2. Error State
  if (error) {
    return (
      <Card title="Multi-Source Evidence Synthesis & Grounded WHY">
        <div
          style={{
            padding: '2.5rem 1.5rem',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '12px',
              background: 'rgba(239, 68, 68, 0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-rose)',
            }}
          >
            <AlertTriangle size={24} />
          </div>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.95rem' }}>
              Failed to Retrieve Evidence Dossier
            </div>
            <div style={{ fontSize: '0.825rem', color: 'var(--accent-rose)', marginTop: '0.25rem' }}>
              {error}
            </div>
          </div>
          {onRefresh && (
            <Button variant="secondary" size="sm" onClick={onRefresh}>
              <RefreshCw size={14} style={{ marginRight: '0.4rem' }} />
              Retry Query
            </Button>
          )}
        </div>
      </Card>
    );
  }

  // 3. Empty / Pending State
  if (!evidence) {
    return (
      <Card title="Multi-Source Evidence Synthesis & Grounded WHY">
        <div
          style={{
            padding: '3rem 1.5rem',
            textAlign: 'center',
            color: 'var(--text-muted)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <Layers size={36} color="var(--border-strong)" />
          <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
            No Cell Selected or Evidence Pending
          </div>
          <div style={{ fontSize: '0.8rem', maxWidth: '320px', lineHeight: 1.5 }}>
            Select an H3 cell on the map or from the ranked potential hotspots list to inspect authoritative evidence and Gemini WHY diagnostics.
          </div>
        </div>
      </Card>
    );
  }

  const { context, observedFacts, modelOutputs, evidence: evData, aiInterpretation, recommendedVerification, provenance } = evidence;

  // Triage state styling
  const triageVariant =
    evData.triageState === 'ALERT_CANDIDATE'
      ? 'danger'
      : evData.triageState === 'MONITOR'
      ? 'warning'
      : 'info';

  const riskVariant =
    modelOutputs.hotspot.riskLevel === 'CRITICAL' || modelOutputs.hotspot.riskLevel === 'HIGH'
      ? 'danger'
      : modelOutputs.hotspot.riskLevel === 'MODERATE'
      ? 'warning'
      : 'success';

  const isAlertCandidate = evData.triageState === 'ALERT_CANDIDATE';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* =========================================================================
          SECTION 6: EVIDENCE CHAIN VISUAL FLOW (ANCHOR NAVIGATION)
          OBSERVED → MODEL OUTPUT → EVIDENCE → AI WHY → VERIFICATION
          ========================================================================= */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.65rem 1rem',
          background: 'var(--bg-card)',
          borderRadius: '12px',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-sm)',
          overflowX: 'auto',
          gap: '0.5rem',
        }}
      >
        {[
          { id: 'section-evidence', label: 'EVIDENCE', icon: Activity },
          { id: 'section-observed', label: 'OBSERVED', icon: Radio },
          { id: 'section-model-output', label: 'MODEL OUTPUT', icon: Cpu },
          { id: 'section-ai-why', label: 'AI WHY', icon: Sparkles },
          { id: 'section-verification', label: 'VERIFICATION', icon: ShieldCheck },
        ].map((step, idx, arr) => {
          const StepIcon = step.icon;
          const isActive = activeSection === step.id;
          return (
            <React.Fragment key={step.id}>
              <button
                type="button"
                onClick={() => scrollToAnchor(step.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.45rem',
                  padding: '0.4rem 0.85rem',
                  borderRadius: '8px',
                  background: isActive ? 'var(--brand-surface)' : 'transparent',
                  border: isActive ? '1px solid var(--brand-border)' : '1px solid transparent',
                  color: isActive ? 'var(--brand-primary)' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  letterSpacing: '0.04em',
                  transition: 'all 0.15s ease',
                  boxShadow: isActive ? '0 0 12px rgba(14, 165, 233, 0.25)' : 'none',
                  whiteSpace: 'nowrap',
                }}
              >
                <StepIcon size={14} color={isActive ? 'var(--brand-primary)' : 'var(--text-muted)'} />
                <span>{step.label}</span>
              </button>
              {idx < arr.length - 1 && (
                <span style={{ color: 'var(--border-medium)', fontSize: '0.85rem', padding: '0 0.15rem' }}>
                  →
                </span>
              )}
            </React.Fragment>
          );
        })}
      </div>

      {/* =========================================================================
          SECTION 4 & 5: EVIDENCE HERO (EVIDENCE STRENGTH + AT-A-GLANCE LINEAGE)
          ========================================================================= */}
      <div id="section-evidence">
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <Activity size={20} color="var(--brand-primary)" />
              <span>Evidence Strength & Traceable Lineage</span>
            </div>
          }
          subtitle="Authoritative normalized score and full causal lineage back to model and event"
          action={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <Badge variant={triageVariant}>
                TRIAGE: {evData.triageState.replace('_', ' ')}
              </Badge>
              {evData.consistency && (
                <Badge variant={evData.consistency === 'consistent' ? 'success' : 'warning'}>
                  {evData.consistency.toUpperCase()}
                </Badge>
              )}
              {isAlertCandidate && (
                <Link to={`/authority/alerts?h3=${encodeURIComponent(context.h3Index)}`} style={{ textDecoration: 'none' }}>
                  <Button variant="primary" size="sm" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <span>Open Authority Alert</span>
                    <ArrowRight size={13} />
                  </Button>
                </Link>
              )}
            </div>
          }
        >
          {/* Main Hero Split: Left Evidence Score / Right Lineage Strip */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'minmax(280px, 1fr) minmax(320px, 1.3fr)',
              gap: '1.25rem',
              alignItems: 'stretch',
            }}
          >
            {/* LEFT: Evidence Strength Card */}
            <div
              style={{
                padding: '1.35rem',
                borderRadius: '12px',
                background: 'linear-gradient(145deg, rgba(6, 182, 212, 0.08), rgba(99, 102, 241, 0.05))',
                border: '1px solid rgba(6, 182, 212, 0.3)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <div style={{ fontSize: '0.725rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--brand-primary)', letterSpacing: '0.05em' }}>
                  Normalized Evidence Score
                </div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginTop: '0.35rem' }}>
                  <span style={{ fontSize: '2.4rem', fontWeight: 800, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', lineHeight: 1 }}>
                    {evData.evidenceScore.toFixed(3)}
                  </span>
                  <span style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>/ 1.000</span>
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.45rem', lineHeight: 1.4 }}>
                  Triage status: <strong style={{ color: 'var(--text-primary)' }}>{evData.triageState}</strong> • Thresholds: Monitor ≥ 0.40, Candidate ≥ 0.55
                </div>
              </div>

              {/* Collapsible Score Breakdown Trigger */}
              <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid rgba(6, 182, 212, 0.2)' }}>
                <button
                  type="button"
                  onClick={() => setScoreBreakdownOpen(!scoreBreakdownOpen)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    width: '100%',
                    background: 'transparent',
                    border: 'none',
                    padding: '0.2rem 0',
                    color: 'var(--brand-primary)',
                    fontSize: '0.78rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  <span>{scoreBreakdownOpen ? 'Hide score breakdown' : 'View score breakdown (4 components)'}</span>
                  {scoreBreakdownOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                </button>

                {scoreBreakdownOpen && evData.scoreBreakdown && (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem', marginTop: '0.65rem' }}>
                    <div style={{ padding: '0.45rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Observation (0.30)</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                        {(evData.scoreBreakdown.observationStrength ?? 0).toFixed(2)}
                      </div>
                    </div>
                    <div style={{ padding: '0.45rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Forecast (0.25)</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                        {(evData.scoreBreakdown.mlForecastSupport ?? 0).toFixed(2)}
                      </div>
                    </div>
                    <div style={{ padding: '0.45rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Multi-Source (0.20)</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                        {(evData.scoreBreakdown.multiSourceAgreement ?? 0).toFixed(2)}
                      </div>
                    </div>
                    <div style={{ padding: '0.45rem 0.6rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Spatial (0.15)</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                        {(evData.scoreBreakdown.spatialConsistency ?? 0).toFixed(2)}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* RIGHT: At-a-glance Traceability Lineage */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(2, 1fr)',
                gap: '0.75rem',
                padding: '1.15rem',
                background: 'var(--bg-surface-elevated)',
                borderRadius: '12px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              {/* Lineage Node 1: H3 Index */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Spatial Hex Cell
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.25rem' }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {context.h3Index}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleCopy('h3', context.h3Index)}
                    style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '0.1rem' }}
                    title="Copy H3 Index"
                  >
                    {copiedKey === 'h3' ? <Check size={12} color="var(--aqi-good)" /> : <Copy size={12} />}
                  </button>
                </div>
              </div>

              {/* Lineage Node 2: Event Code */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Canonical Event
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.25rem' }}>
                  <span style={{ fontSize: '0.825rem', fontWeight: 700, color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>
                    {context.eventCode || 'PENDING'}
                  </span>
                  {context.eventCode && (
                    <button
                      type="button"
                      onClick={() => handleCopy('evtCode', context.eventCode || '')}
                      style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '0.1rem' }}
                      title="Copy Event Code"
                    >
                      {copiedKey === 'evtCode' ? <Check size={12} color="var(--aqi-good)" /> : <Copy size={12} />}
                    </button>
                  )}
                </div>
              </div>

              {/* Lineage Node 3: Event ID */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Event UUID
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.25rem' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
                    {context.eventId ? `${context.eventId.slice(0, 8)}...${context.eventId.slice(-4)}` : 'Unlinked'}
                  </span>
                  {context.eventId && (
                    <button
                      type="button"
                      onClick={() => handleCopy('evtId', context.eventId || '')}
                      style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '0.1rem' }}
                      title={`Copy full UUID: ${context.eventId}`}
                    >
                      {copiedKey === 'evtId' ? <Check size={12} color="var(--aqi-good)" /> : <Copy size={12} />}
                    </button>
                  )}
                </div>
              </div>

              {/* Lineage Node 4: Parent Prediction */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Parent F3 Prediction
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.25rem' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
                    {context.predictionId ? `${context.predictionId.slice(0, 8)}...${context.predictionId.slice(-4)}` : 'Direct Run'}
                  </span>
                  {context.predictionId && (
                    <button
                      type="button"
                      onClick={() => handleCopy('predId', context.predictionId || '')}
                      style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '0.1rem' }}
                      title={`Copy full UUID: ${context.predictionId}`}
                    >
                      {copiedKey === 'predId' ? <Check size={12} color="var(--aqi-good)" /> : <Copy size={12} />}
                    </button>
                  )}
                </div>
              </div>

              {/* Cluster Notice if applicable */}
              {evData.clusterH3Cells && evData.clusterH3Cells.length > 1 && (
                <div
                  style={{
                    gridColumn: '1 / -1',
                    padding: '0.5rem 0.75rem',
                    borderRadius: '6px',
                    background: 'rgba(99, 102, 241, 0.08)',
                    border: '1px solid rgba(99, 102, 241, 0.25)',
                    fontSize: '0.75rem',
                    color: 'var(--text-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.45rem',
                  }}
                >
                  <Compass size={14} color="var(--brand-primary)" />
                  <span>
                    Spatial cluster: {evData.clusterH3Cells.length} contiguous cells ({evData.clusterH3Cells.slice(0, 3).join(', ')}...)
                  </span>
                </div>
              )}
            </div>
          </div>
        </Card>
      </div>

      {/* =========================================================================
          SECTION 7: OBSERVED (PHYSICAL EVIDENCE ONLY - STRICTLY SEPARATED)
          ========================================================================= */}
      <div id="section-observed">
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Radio size={18} color="var(--accent-teal)" />
              <span style={{ color: 'var(--accent-teal)', fontWeight: 800 }}>OBSERVED</span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                — Physical ground telemetry & remote sensing facts
              </span>
            </div>
          }
          badge={<Badge variant="success">Physical Ground Telemetry</Badge>}
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
            {/* Card 1: Air Quality Telemetry */}
            <div style={{ padding: '0.9rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-teal)', fontSize: '0.8rem', fontWeight: 700 }}>
                  <TrendingUp size={14} /> Air Quality
                </div>
                <Badge variant={observedFacts.air?.pm25 !== null && observedFacts.air?.pm25 !== undefined ? 'success' : 'neutral'}>
                  {observedFacts.air?.pm25 !== null && observedFacts.air?.pm25 !== undefined ? 'Active' : 'Unavailable'}
                </Badge>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.825rem' }}>
                <div>
                  PM2.5:{' '}
                  <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', fontSize: '1.05rem' }}>
                    {observedFacts.air?.pm25 !== null && observedFacts.air?.pm25 !== undefined ? `${observedFacts.air.pm25.toFixed(1)} µg/m³` : 'Unavailable'}
                  </strong>
                </div>
                {observedFacts.air?.pm10 !== null && observedFacts.air?.pm10 !== undefined && (
                  <div style={{ color: 'var(--text-secondary)' }}>
                    PM10: {observedFacts.air.pm10.toFixed(1)} µg/m³
                  </div>
                )}
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Station: {observedFacts.air?.stationId || 'Reference Monitor'}
                </div>
              </div>
            </div>

            {/* Card 2: Surface Meteorology */}
            <div style={{ padding: '0.9rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)', fontSize: '0.8rem', fontWeight: 700 }}>
                  <Wind size={14} /> Surface Meteorology
                </div>
                <Badge variant={observedFacts.weather?.temperature !== null && observedFacts.weather?.temperature !== undefined ? 'info' : 'neutral'}>
                  {observedFacts.weather?.temperature !== null && observedFacts.weather?.temperature !== undefined ? 'Synced' : 'Unavailable'}
                </Badge>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.825rem' }}>
                <div>
                  Temperature:{' '}
                  <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {observedFacts.weather?.temperature !== null && observedFacts.weather?.temperature !== undefined ? `${observedFacts.weather.temperature.toFixed(1)} °C` : 'Unavailable'}
                  </strong>
                </div>
                <div>
                  Wind Speed:{' '}
                  <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {observedFacts.weather?.windSpeedMps !== null && observedFacts.weather?.windSpeedMps !== undefined
                      ? `${observedFacts.weather.windSpeedMps.toFixed(1)} m/s`
                      : observedFacts.weather?.windSpeedKmh !== null && observedFacts.weather?.windSpeedKmh !== undefined
                      ? `${observedFacts.weather.windSpeedKmh.toFixed(1)} km/h`
                      : 'Unavailable'}
                  </strong>
                </div>
                {observedFacts.weather?.humidity !== null && observedFacts.weather?.humidity !== undefined && (
                  <div style={{ color: 'var(--text-secondary)' }}>
                    Humidity: {observedFacts.weather.humidity.toFixed(0)}%
                  </div>
                )}
              </div>
            </div>

            {/* Card 3: Sensor Network Proximity */}
            <div style={{ padding: '0.9rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-purple)', fontSize: '0.8rem', fontWeight: 700 }}>
                  <Radio size={14} /> Sensor Proximity
                </div>
                <Badge variant="neutral">Topological</Badge>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.825rem' }}>
                <div>
                  Nearest CAAQMS:{' '}
                  <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {observedFacts.monitoringCoverage?.nearestStationDistanceKm !== null && observedFacts.monitoringCoverage?.nearestStationDistanceKm !== undefined
                      ? `${observedFacts.monitoringCoverage.nearestStationDistanceKm.toFixed(2)} km`
                      : 'Unknown'}
                  </strong>
                </div>
                <div style={{ color: 'var(--text-secondary)' }}>
                  Coverage Gap:{' '}
                  <strong>{observedFacts.monitoringCoverage?.monitoringCoverageGapFlag ? 'YES (Sparse)' : 'NO (Adequate)'}</strong>
                </div>
              </div>
            </div>

            {/* Card 4: Proximity & Remote Sensing */}
            <div style={{ padding: '0.9rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-amber)', fontSize: '0.8rem', fontWeight: 700 }}>
                  <Building2 size={14} /> Remote Sensing
                </div>
                <Badge variant="neutral">Firms / CAMS</Badge>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.825rem' }}>
                <div>
                  Industrial Node Dist:{' '}
                  <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {observedFacts.gisContext?.distToNearestIndustrialKm !== null && observedFacts.gisContext?.distToNearestIndustrialKm !== undefined
                      ? `${observedFacts.gisContext.distToNearestIndustrialKm.toFixed(1)} km`
                      : 'N/A'}
                  </strong>
                </div>
                <div>
                  Active Fires (24h/25km):{' '}
                  <strong style={{ color: observedFacts.gisContext?.fireCount24h25km ? 'var(--accent-rose)' : 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {observedFacts.gisContext?.fireCount24h25km ?? 0} thermal hotspots
                  </strong>
                </div>
              </div>
            </div>
          </div>

          {/* Crowdsourced Citizen Observations (Auxiliary Evidence) */}
          {(() => {
            const citizenSignals = evData.signals?.filter(
              (s) => s.dataSource === 'CITIZEN' || s.sourceType === 'CITIZEN_OBSERVATION'
            ) || [];

            return (
              <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', color: 'var(--brand-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
                    <Users size={16} />
                    <span>Citizen Visual Evidence (Auxiliary Telemetry)</span>
                  </div>
                  <Badge variant={citizenSignals.length > 0 ? 'info' : 'neutral'}>
                    {citizenSignals.length > 0
                      ? `${citizenSignals.length} Active Observation${citizenSignals.length > 1 ? 's' : ''}`
                      : 'No Reports Registered'}
                  </Badge>
                </div>

                {citizenSignals.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {citizenSignals.map((cs, idx) => (
                      <div
                        key={cs.signalId || idx}
                        style={{
                          padding: '1rem',
                          background: 'var(--bg-surface-elevated)',
                          borderRadius: '8px',
                          border: '1px solid var(--border-subtle)',
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '0.5rem',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <Badge variant="warning">UNVERIFIED CITIZEN REPORT</Badge>
                            <Badge variant="neutral">AUXILIARY EVIDENCE</Badge>
                          </div>
                          {cs.confidenceScore !== undefined && cs.confidenceScore !== null && (
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                              Visual Interpretation Confidence: <strong style={{ color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>{(cs.confidenceScore * 100).toFixed(0)}%</strong>
                            </div>
                          )}
                        </div>

                        <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                          {cs.description}
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                          <div>
                            Report ID: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>{cs.sourceRef || cs.signalId}</code>
                          </div>
                          <div>
                            Spatial Anchor: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>{context.h3Index}</code>
                          </div>
                          {cs.timestamp && (
                            <div>
                              Observed: <span>{new Date(cs.timestamp).toLocaleString()}</span>
                            </div>
                          )}
                        </div>

                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontStyle: 'italic', background: 'rgba(245, 158, 11, 0.05)', padding: '0.45rem 0.65rem', borderRadius: '4px', border: '1px solid rgba(245, 158, 11, 0.15)' }}>
                          * Disclaimer: Visual AI interpretation only. Does not confirm ground PM2.5 concentration, source causality, or legal violation.
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ padding: '0.75rem 1rem', background: 'var(--bg-surface-elevated)', borderRadius: '6px', fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No crowdsourced citizen observations currently registered in H3 cell {context.h3Index}.
                  </div>
                )}
              </div>
            );
          })()}
        </Card>
      </div>

      {/* =========================================================================
          SECTION 8: MODEL OUTPUT (F3 HOTSPOT & F4 FORECAST SEPARATED)
          ========================================================================= */}
      <div id="section-model-output">
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Cpu size={18} color="var(--brand-primary)" />
              <span style={{ color: 'var(--brand-primary)', fontWeight: 800 }}>MODEL OUTPUT</span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                — Authoritative ML classifier & multi-horizon regressors
              </span>
            </div>
          }
          badge={<Badge variant={riskVariant}>F3 Hotspot: {modelOutputs.hotspot.riskLevel}</Badge>}
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 1fr) minmax(320px, 1.4fr)', gap: '1.25rem', alignItems: 'stretch' }}>
            {/* Card A: F3 Hotspot Output */}
            <div style={{ padding: '1.15rem', background: 'var(--bg-surface-elevated)', borderRadius: '10px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: '0.725rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--brand-primary)', marginBottom: '0.4rem' }}>
                  F3 Classifier (Calibrated Random Forest)
                </div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
                  <span style={{ fontSize: '2.1rem', fontWeight: 800, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', lineHeight: 1 }}>
                    {modelOutputs.hotspot.riskScore.toFixed(3)}
                  </span>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    (Operational threshold: {modelOutputs.hotspot.operationalThreshold.toFixed(2)})
                  </span>
                </div>
                <div style={{ marginTop: '0.65rem', fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                  <div>Status: <strong style={{ color: modelOutputs.hotspot.isHotspot ? 'var(--accent-rose)' : 'var(--text-primary)' }}>{modelOutputs.hotspot.isHotspot ? 'HOTSPOT CONFIRMED' : 'BELOW THRESHOLD'}</strong></div>
                  <div>Model Version: <code style={{ fontFamily: 'var(--font-mono)' }}>{modelOutputs.hotspot.modelVersion}</code></div>
                  <div>Confidence Breakdown: {(modelOutputs.hotspot.confidence * 100).toFixed(0)}% calibrated probability</div>
                </div>
                <div style={{ marginTop: '0.85rem' }}>
                  <Link to={`/hotspots?h3=${encodeURIComponent(context.h3Index)}`} style={{ textDecoration: 'none' }}>
                    <Button variant="outline" size="sm" style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}>
                      <span>View on Hotspot Map</span> <ExternalLink size={12} style={{ marginLeft: '0.3rem' }} />
                    </Button>
                  </Link>
                </div>
              </div>
            </div>

            {/* Card B: F4 Forecast Horizons */}
            <div style={{ padding: '1.15rem', background: 'var(--bg-surface-elevated)', borderRadius: '10px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: '0.725rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--accent-teal)', marginBottom: '0.5rem' }}>
                  F4 Multi-Horizon PM2.5 Trajectory (Empirical P10/P90 Bounds)
                </div>
                {modelOutputs.forecast && modelOutputs.forecast.horizons && modelOutputs.forecast.horizons.length > 0 ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.65rem' }}>
                    {modelOutputs.forecast.horizons.map((h) => (
                      <div
                        key={h.horizonHours}
                        style={{
                          padding: '0.65rem 0.5rem',
                          background: 'var(--bg-card)',
                          borderRadius: '8px',
                          border: '1px solid var(--border-subtle)',
                          textAlign: 'center',
                        }}
                      >
                        <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                          +{h.horizonHours} Hour Forecast
                        </div>
                        <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', margin: '0.2rem 0' }}>
                          {h.predictedPm25.toFixed(1)}
                        </div>
                        <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                          [{h.lowerBound !== null && h.lowerBound !== undefined ? h.lowerBound.toFixed(0) : '?'} –{' '}
                          {h.upperBound !== null && h.upperBound !== undefined ? h.upperBound.toFixed(0) : '?'}] µg/m³
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ padding: '1.5rem', color: 'var(--text-muted)', fontSize: '0.825rem', textAlign: 'center', background: 'var(--bg-card)', borderRadius: '8px', border: '1px dashed var(--border-medium)' }}>
                    Forecast unavailable for this cell.
                  </div>
                )}
                <div style={{ marginTop: '0.85rem' }}>
                  <Link to={`/forecast?h3=${encodeURIComponent(context.h3Index)}`} style={{ textDecoration: 'none' }}>
                    <Button variant="outline" size="sm" style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}>
                      <span>Open Full Forecast</span> <ExternalLink size={12} style={{ marginLeft: '0.3rem' }} />
                    </Button>
                  </Link>
                </div>
              </div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.6rem', fontStyle: 'italic' }}>
                * F4 forecastConfidence is strictly null per locked specification.
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* =========================================================================
          SECTION 9: AI INTERPRETATION (GROUNDED GEMINI NARRATIVE)
          ========================================================================= */}
      <div id="section-ai-why">
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Sparkles size={18} color="var(--accent-purple)" />
              <span style={{ color: 'var(--accent-purple)', fontWeight: 800 }}>AI INTERPRETATION</span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                — Grounded Gemini explanation of validated evidence
              </span>
            </div>
          }
          badge={
            aiInterpretation ? (
              <Badge variant={aiInterpretation.isGrounded ? 'success' : 'warning'}>
                {aiInterpretation.isGrounded ? 'GROUNDED REASONING' : 'UNVALIDATED'}
              </Badge>
            ) : (
              <Badge variant="neutral">AI UNAVAILABLE</Badge>
            )
          }
        >
          {aiInterpretation ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {/* Grounded Information Badge */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.45rem 0.75rem',
                  borderRadius: '6px',
                  background: 'rgba(168, 85, 247, 0.08)',
                  border: '1px solid rgba(168, 85, 247, 0.25)',
                  fontSize: '0.75rem',
                  color: 'var(--text-secondary)',
                }}
              >
                <ShieldCheck size={14} color="#c084fc" style={{ flexShrink: 0 }} />
                <span>
                  <strong>Grounded Interpretation:</strong> Gemini explains validated evidence. Gemini does not calculate risk, modify forecasts, or replace field verification.
                </span>
              </div>

              {/* PRIMARY: WHY THIS CELL IS FLAGGED */}
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#c084fc', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
                  Why This Cell Is Flagged
                </div>
                <div
                  style={{
                    fontSize: '0.925rem',
                    color: 'var(--text-primary)',
                    lineHeight: 1.6,
                    background: 'var(--bg-surface-elevated)',
                    padding: '1rem 1.15rem',
                    borderRadius: '10px',
                    border: '1px solid var(--border-subtle)',
                  }}
                >
                  {aiInterpretation.summaryPublic || aiInterpretation.summaryAnalyst}
                </div>
              </div>

              {/* Collapsible: Technical Diagnostic Details */}
              {aiInterpretation.summaryAnalyst && (
                <div style={{ border: '1px solid var(--border-subtle)', borderRadius: '8px', overflow: 'hidden' }}>
                  <button
                    type="button"
                    onClick={() => setTechnicalDetailsOpen(!technicalDetailsOpen)}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.65rem 0.85rem',
                      background: 'var(--bg-surface-elevated)',
                      border: 'none',
                      color: 'var(--text-secondary)',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    <span>Technical Analyst Diagnostics</span>
                    {technicalDetailsOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                  </button>
                  {technicalDetailsOpen && (
                    <div style={{ padding: '0.85rem 1rem', fontSize: '0.825rem', color: 'var(--text-secondary)', lineHeight: 1.55, background: 'var(--bg-card)' }}>
                      <div>{aiInterpretation.summaryAnalyst}</div>
                      {aiInterpretation.forecastTrajectory && (
                        <div style={{ marginTop: '0.5rem', color: 'var(--text-muted)' }}>
                          <strong>Trajectory:</strong> {aiInterpretation.forecastTrajectory}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Collapsible: Grounding Safeguards & Exclusions */}
              {aiInterpretation.unsupportedConclusions && aiInterpretation.unsupportedConclusions.length > 0 && (
                <div style={{ border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '8px', overflow: 'hidden' }}>
                  <button
                    type="button"
                    onClick={() => setSafeguardsOpen(!safeguardsOpen)}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.65rem 0.85rem',
                      background: 'rgba(239, 68, 68, 0.05)',
                      border: 'none',
                      color: 'var(--accent-rose)',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    <span>Grounding Safeguards & Excluded Hypotheses ({aiInterpretation.unsupportedConclusions.length})</span>
                    {safeguardsOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                  </button>
                  {safeguardsOpen && (
                    <div style={{ padding: '0.85rem 1rem', background: 'var(--bg-card)', fontSize: '0.78rem' }}>
                      <div style={{ color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                        The following hypotheses were excluded by the F5 Grounding Guard due to insufficient telemetry corroboration:
                      </div>
                      <ul style={{ margin: 0, paddingLeft: '1.2rem', color: 'var(--text-secondary)' }}>
                        {aiInterpretation.unsupportedConclusions.map((uc, i) => (
                          <li key={i}>{uc}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div
              style={{
                padding: '1.5rem',
                color: 'var(--text-muted)',
                fontSize: '0.85rem',
                textAlign: 'center',
                background: 'var(--bg-surface-elevated)',
                borderRadius: '8px',
                border: '1px dashed var(--border-medium)',
              }}
            >
              AI explanation unavailable — showing verified data only.
            </div>
          )}
        </Card>
      </div>

      {/* =========================================================================
          SECTION 10: RECOMMENDED VERIFICATION (ACTIONABLE DIRECTIVES)
          ========================================================================= */}
      {recommendedVerification && (
        <div id="section-verification">
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <ShieldCheck size={18} color="var(--accent-amber)" />
                <span style={{ color: 'var(--accent-amber)', fontWeight: 800 }}>RECOMMENDED VERIFICATION</span>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                  — Targeted municipal and field validation protocols
                </span>
              </div>
            }
            badge={
              <Badge variant={recommendedVerification.priority === 'URGENT' ? 'danger' : 'warning'}>
                PRIORITY: {recommendedVerification.priority}
              </Badge>
            }
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {recommendedVerification.action}
              </div>

              {recommendedVerification.guidelines && recommendedVerification.guidelines.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem', marginTop: '0.2rem' }}>
                  {recommendedVerification.guidelines.map((guide, idx) => (
                    <div
                      key={idx}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.55rem',
                        padding: '0.6rem 0.85rem',
                        background: 'var(--bg-surface-elevated)',
                        borderRadius: '6px',
                        border: '1px solid var(--border-subtle)',
                        fontSize: '0.825rem',
                        color: 'var(--text-secondary)',
                      }}
                    >
                      <CheckCircle2 size={15} color="var(--accent-teal)" style={{ flexShrink: 0, marginTop: '2px' }} />
                      <span>{guide}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Action Banner connecting to Authority Alert Queue */}
              <div
                style={{
                  marginTop: '0.75rem',
                  padding: '0.85rem 1.15rem',
                  borderRadius: '8px',
                  background: isAlertCandidate ? 'rgba(99, 102, 241, 0.08)' : 'var(--bg-surface-elevated)',
                  border: isAlertCandidate ? '1px solid rgba(99, 102, 241, 0.25)' : '1px solid var(--border-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700, color: isAlertCandidate ? 'var(--brand-primary)' : 'var(--text-secondary)' }}>
                    {isAlertCandidate ? 'Operational Authority Alert Available' : 'No Operational Alert Required'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                    {isAlertCandidate
                      ? 'This cell meets the ALERT_CANDIDATE threshold and is queued for municipal acknowledgement and field dispatch.'
                      : 'Evidence currently does not qualify for authority alert. Continued passive monitoring active.'}
                  </div>
                </div>

                {isAlertCandidate && (
                  <Link to={`/authority/alerts?h3=${encodeURIComponent(context.h3Index)}`} style={{ textDecoration: 'none' }}>
                    <Button variant="primary" size="sm" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span>View Authority Alert</span>
                      <ArrowRight size={13} />
                    </Button>
                  </Link>
                )}
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* =========================================================================
          SECTION 11: PROVENANCE & LINEAGE ACCORDION
          ========================================================================= */}
      <div id="section-provenance" style={{ border: '1px solid var(--border-subtle)', borderRadius: '8px', overflow: 'hidden' }}>
        <button
          type="button"
          onClick={() => setProvenanceOpen(!provenanceOpen)}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0.75rem 1rem',
            background: 'var(--bg-surface-elevated)',
            border: 'none',
            color: 'var(--text-muted)',
            fontSize: '0.75rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <FileText size={14} />
            <span>Audit Provenance & Artifact Model Versions</span>
          </div>
          {provenanceOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>

        {provenanceOpen && (
          <div style={{ padding: '1rem', background: 'var(--bg-card)', fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
            <div>F3 Model: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{provenance.f3ModelVersion}</code></div>
            <div>F4 Model: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{provenance.f4ModelVersion}</code></div>
            <div>F5 Engine: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{provenance.f5ScoringVersion}</code></div>
            <div>Gemini Model: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{provenance.geminiModelVersion}</code></div>
            <div>Evaluated At: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{new Date(provenance.evaluatedAt || Date.now()).toISOString()}</code></div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EvidencePanel;
