import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import {
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  ShieldCheck,
  Send,
  FileCheck,
  Info,
} from 'lucide-react';

interface GeminiExplanationProps {
  eventId?: string;
  h3Index?: string;
  summary?: string;
  contributingSignals?: string[];
  limitations?: string;
  recommendedAction?: string;
  onDispatchAction?: () => void;
}

export const GeminiExplanation: React.FC<GeminiExplanationProps> = ({
  eventId = 'EVENT-1023',
  h3Index = '8860144aa1fffff',
  summary = 'A localized particulate spike in this cell is driven by stagnant surface winds (1.1 m/s) preventing atmospheric dispersion, compounded by an active thermal anomaly (NASA FIRMS 14.8 MW) 1.4 km upwind. A concurrent citizen report confirms visible dense smoke emissions within the immediate micro-shed.',
  contributingSignals = [
    'PM2.5 increased rapidly (+25.5% in past 30 minutes at Shivajinagar CAAQMS)',
    'Low surface wind (1.1 m/s stagnation) severely reduces horizontal ventilation',
    'Nearby NASA FIRMS thermal anomaly indicates possible open biomass or waste burning',
    'Sentinel-5P satellite observations reflect localized elevated tropospheric NO2 column',
    'Corroborating citizen visual report flagged dense black smoke plume with high confidence',
  ],
  limitations = 'Ground sensor density in this sector relies on one continuous CAAQMS 1.2 km away. Satellite observation represents the latest orbital overpass (10:25 AM). Plume trajectory model assumes steady wind direction over the next 60 minutes.',
  recommendedAction = 'Field verification recommended. Mobilize municipal inspection squad to sector 4 and alert regional anti-smog water misting unit upwind.',
  onDispatchAction,
}) => {
  const [isDispatched, setIsDispatched] = useState(false);

  const handleAction = () => {
    setIsDispatched(true);
    if (onDispatchAction) onDispatchAction();
  };

  return (
    <div
      style={{
        borderRadius: '16px',
        background: 'linear-gradient(145deg, rgba(168, 85, 247, 0.08), rgba(56, 189, 248, 0.05))',
        border: '1px solid rgba(168, 85, 247, 0.35)',
        padding: '1.5rem',
        boxShadow: 'var(--shadow-md)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <div
              style={{
                width: '28px',
                height: '28px',
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #a855f7, #6366f1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
                boxShadow: '0 2px 8px rgba(168, 85, 247, 0.4)',
              }}
            >
              <Sparkles size={16} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0, fontFamily: 'var(--font-heading)' }}>
              Gemini WHY
            </h3>
            <span
              style={{
                fontSize: '0.7rem',
                fontWeight: 700,
                padding: '0.15rem 0.5rem',
                borderRadius: '4px',
                background: 'rgba(168, 85, 247, 0.15)',
                color: '#c084fc',
                border: '1px solid rgba(168, 85, 247, 0.3)',
              }}
            >
              Gemini 2.5 Flash
            </span>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
            "Why is AeroSentinel flagging this location?"
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Badge variant="warning">Event ID: {eventId}</Badge>
        </div>
      </div>

      {/* Mandatory Regulatory / Engineering Disclaimer */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '0.5rem 0.85rem',
          borderRadius: '6px',
          background: 'rgba(168, 85, 247, 0.08)',
          border: '1px solid rgba(168, 85, 247, 0.2)',
          fontSize: '0.75rem',
          color: '#e9d5ff',
        }}
      >
        <Info size={14} style={{ flexShrink: 0, color: '#c084fc' }} />
        <span>
          <strong>AI-generated explanation based on validated evidence.</strong> Gemini synthesizes cross-sensor telemetry into physical insights; it is not the numerical prediction engine.
        </span>
      </div>

      {/* SECTION 1: SUMMARY */}
      <div>
        <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#c084fc', letterSpacing: '0.05em', marginBottom: '0.4rem' }}>
          Physical Diagnostic Summary
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
          {summary}
        </div>
      </div>

      {/* SECTION 2: CONTRIBUTING SIGNALS */}
      <div>
        <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#c084fc', letterSpacing: '0.05em', marginBottom: '0.4rem' }}>
          Validated Contributing Signals
        </div>
        <div
          style={{
            background: 'var(--bg-surface-elevated)',
            padding: '0.85rem 1.15rem',
            borderRadius: '10px',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <ul style={{ margin: 0, paddingLeft: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.45rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            {contributingSignals.map((sig, idx) => (
              <li key={idx} style={{ lineHeight: 1.45 }}>
                <span style={{ color: 'var(--text-primary)' }}>{sig}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* SECTION 3: CONFIDENCE / LIMITATIONS */}
      <div>
        <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em', marginBottom: '0.4rem' }}>
          Confidence & Observational Limitations
        </div>
        <div
          style={{
            fontSize: '0.8rem',
            color: 'var(--text-secondary)',
            lineHeight: 1.5,
            background: 'var(--bg-surface)',
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
          }}
        >
          {limitations}
        </div>
      </div>

      {/* SECTION 4: RECOMMENDED NEXT STEP */}
      <div
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '12px',
          background: 'rgba(16, 185, 129, 0.08)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.75rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckCircle2 size={18} color="var(--accent-teal)" />
            <strong style={{ fontSize: '0.875rem', color: 'var(--accent-teal)' }}>
              Recommended Next Step
            </strong>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Authority Action Protocol</span>
        </div>

        <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', margin: 0, lineHeight: 1.5 }}>
          {recommendedAction}
        </p>

        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.25rem', flexWrap: 'wrap' }}>
          {isDispatched ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-teal)', fontSize: '0.85rem', fontWeight: 600 }}>
              <FileCheck size={16} />
              <span>Inspection Dispatch Ticket #DISP-9103 Created ✓</span>
            </div>
          ) : (
            <Button
              variant="primary"
              size="sm"
              onClick={handleAction}
              style={{
                background: 'linear-gradient(135deg, #10b981, #059669)',
                borderColor: '#10b981',
              }}
            >
              <Send size={14} style={{ marginRight: '0.4rem' }} />
              Field Verification Recommended — Dispatch Inspection Team
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
