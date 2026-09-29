import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Badge, BadgeVariant } from '../common/Badge';
import { VisionAnalysisSummary } from '../../types';
import {
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  Clock,
  Cpu,
  FileText,
  ShieldCheck,
  Maximize2,
  X,
  Eye,
  Info,
} from 'lucide-react';

interface GeminiVisionCardProps {
  photoUrl?: string | null;
  visionAnalysis?: VisionAnalysisSummary | null;
  category?: string;
  reportRef?: string;
}

export const GeminiVisionCard: React.FC<GeminiVisionCardProps> = ({
  photoUrl,
  visionAnalysis,
  category,
  reportRef,
}) => {
  const [isZoomed, setIsZoomed] = useState(false);

  // Model provider classification: strictly derived from backend metadata
  const modelVersion = visionAnalysis?.modelVersion || '';
  const status = visionAnalysis?.analysisStatus?.toUpperCase() || (visionAnalysis ? 'ANALYZED' : 'UNAVAILABLE');

  const isRealGemini = modelVersion.toLowerCase().startsWith('gemini');
  const isFallback =
    modelVersion.toLowerCase().includes('fallback') ||
    status === 'FALLBACK' ||
    status === 'DETERMINISTIC_FALLBACK';
  const isUnavailable = !visionAnalysis || status === 'UNAVAILABLE' || status === 'FAILED';

  // Semantic provider badge text & styling
  let providerTitle = 'AI visual analysis unavailable';
  let providerBadgeVariant: BadgeVariant = 'neutral';
  let providerBadgeText = 'AI UNAVAILABLE';

  if (isRealGemini) {
    providerTitle = 'Analyzed by Gemini Vision';
    providerBadgeVariant = 'success';
    providerBadgeText = 'REAL GEMINI VISION';
  } else if (isFallback) {
    providerTitle = 'Deterministic fallback analysis';
    providerBadgeVariant = 'warning';
    providerBadgeText = 'DETERMINISTIC FALLBACK';
  }

  const confidencePercent =
    visionAnalysis?.confidence !== undefined && visionAnalysis?.confidence !== null
      ? Math.round(visionAnalysis.confidence * 100)
      : null;

  const detectedCategory =
    visionAnalysis?.detectedCategory || category || 'UNKNOWN';

  const observations = visionAnalysis?.observations && visionAnalysis.observations.length > 0
    ? visionAnalysis.observations
    : null;

  const uncertainty = visionAnalysis?.uncertainty && visionAnalysis.uncertainty.length > 0
    ? visionAnalysis.uncertainty
    : null;

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '8px',
              background: isRealGemini
                ? 'rgba(168, 85, 247, 0.15)'
                : isFallback
                ? 'rgba(245, 158, 11, 0.15)'
                : 'rgba(148, 163, 184, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: isRealGemini
                ? 'var(--accent-purple, #a855f7)'
                : isFallback
                ? 'var(--accent-amber, #f59e0b)'
                : 'var(--text-muted, #94a3b8)',
            }}
          >
            <Sparkles size={18} />
          </div>
          <div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Gemini Vision Interpretation
            </div>
            <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 400 }}>
              {providerTitle}
            </div>
          </div>
        </div>
      }
      subtitle="Multimodal AI interpretation of ground-level citizen photo evidence"
      badge={<Badge variant={providerBadgeVariant}>{providerBadgeText}</Badge>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '0.75rem' }}>
        {/* =========================================================================
            MANDATORY SEMANTIC DISTINCTION BANNER (SECTION 5)
            AI visual interpretation ≠ numeric pollution measurement ≠ causal source attribution
            ========================================================================= */}
        <div
          style={{
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            background: 'rgba(56, 189, 248, 0.08)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.6rem',
            fontSize: '0.78rem',
            lineHeight: 1.45,
            color: 'var(--text-secondary)',
          }}
        >
          <Info size={16} style={{ color: 'var(--brand-primary)', flexShrink: 0, marginTop: '2px' }} />
          <div>
            <strong style={{ color: 'var(--brand-primary)', display: 'block', marginBottom: '0.2rem' }}>
              Semantic Boundary Notice
            </strong>
            AI visual interpretation &ne; numeric pollution measurement &ne; causal source attribution.
            Visual condition models evaluate visible ground plumes as auxiliary signals to corroborate physical sensor networks.
          </div>
        </div>

        {/* =========================================================================
            PHOTO + ANALYSIS TWO-COLUMN COMPOSITION (SECTION 7)
            LEFT: Citizen photo | RIGHT: Gemini Vision interpretation
            ========================================================================= */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '1.25rem',
            alignItems: 'start',
          }}
        >
          {/* -------------------------------------------------------------
              LEFT COLUMN: CITIZEN PHOTO
              ------------------------------------------------------------- */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '0.5rem',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                fontSize: '0.75rem',
                fontWeight: 600,
                color: 'var(--text-muted)',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
              }}
            >
              <span>Citizen Photo Evidence</span>
              {reportRef && <span style={{ fontFamily: 'var(--font-mono)' }}>{reportRef}</span>}
            </div>

            <div
              style={{
                position: 'relative',
                borderRadius: '10px',
                overflow: 'hidden',
                background: 'var(--bg-surface-elevated, #f8fafc)',
                border: '1px solid var(--border-subtle, #e2e8f0)',
                minHeight: '220px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {photoUrl ? (
                <>
                  <img
                    src={photoUrl}
                    alt={`Citizen environmental observation evidence for ${reportRef || 'report'}`}
                    style={{
                      width: '100%',
                      height: '240px',
                      objectFit: 'cover',
                      display: 'block',
                      cursor: 'zoom-in',
                    }}
                    onClick={() => setIsZoomed(true)}
                    onError={(e) => {
                      // Fallback if image URL cannot be loaded
                      (e.target as HTMLElement).style.display = 'none';
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setIsZoomed(true)}
                    style={{
                      position: 'absolute',
                      bottom: '8px',
                      right: '8px',
                      background: 'rgba(15, 23, 42, 0.75)',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '6px',
                      padding: '4px 8px',
                      fontSize: '0.72rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                      cursor: 'pointer',
                      backdropFilter: 'blur(4px)',
                    }}
                    aria-label="Expand citizen photo"
                  >
                    <Maximize2 size={12} /> Expand
                  </button>
                </>
              ) : (
                <div
                  style={{
                    padding: '2rem 1rem',
                    textAlign: 'center',
                    color: 'var(--text-muted)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '0.5rem',
                  }}
                >
                  <Eye size={28} style={{ opacity: 0.4 }} />
                  <span style={{ fontSize: '0.825rem' }}>No photo attached to this submission</span>
                </div>
              )}
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textAlign: 'right' }}>
              Raw ground imagery ingested under tamper-evident audit control
            </div>
          </div>

          {/* -------------------------------------------------------------
              RIGHT COLUMN: GEMINI INTERPRETATION
              ------------------------------------------------------------- */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '1rem',
              background: 'var(--bg-surface-elevated, #ffffff)',
              padding: '1.25rem',
              borderRadius: '10px',
              border: '1px solid var(--border-subtle, #e2e8f0)',
            }}
          >
            {/* Condition Header & Confidence */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Detected Condition:
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: '0.15rem' }}>
                  {detectedCategory}
                </div>
              </div>

              {confidencePercent !== null && (
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                    Confidence:
                  </div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: confidencePercent >= 70 ? 'var(--accent-teal, #10b981)' : 'var(--accent-amber, #f59e0b)' }}>
                    {confidencePercent}%
                  </div>
                </div>
              )}
            </div>

            {/* Confidence Bar */}
            {confidencePercent !== null && (
              <div>
                <div
                  style={{
                    height: '6px',
                    width: '100%',
                    background: 'var(--bg-canvas, #f1f5f9)',
                    borderRadius: '9999px',
                    overflow: 'hidden',
                  }}
                  role="progressbar"
                  aria-valuenow={confidencePercent}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label="Gemini model visual confidence score"
                >
                  <div
                    style={{
                      height: '100%',
                      width: `${Math.min(100, Math.max(5, confidencePercent))}%`,
                      background: confidencePercent >= 70 ? 'var(--accent-teal, #10b981)' : 'var(--accent-amber, #f59e0b)',
                      borderRadius: '9999px',
                      transition: 'width 0.4s ease',
                    }}
                  />
                </div>
              </div>
            )}

            {/* Structured Observations */}
            <div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '0.4rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <CheckCircle2 size={14} style={{ color: 'var(--accent-teal, #10b981)' }} />
                <span>Visual Observations</span>
              </div>
              {observations ? (
                <ul
                  style={{
                    margin: 0,
                    paddingLeft: '1.2rem',
                    fontSize: '0.825rem',
                    color: 'var(--text-primary)',
                    lineHeight: 1.5,
                  }}
                >
                  {observations.map((obs, idx) => (
                    <li key={idx} style={{ marginBottom: '0.25rem' }}>{obs}</li>
                  ))}
                </ul>
              ) : (
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                  {isUnavailable ? 'No visual observations recorded.' : 'Visual features within baseline threshold.'}
                </div>
              )}
            </div>

            {/* Uncertainty / Limitations Box */}
            <div
              style={{
                padding: '0.75rem',
                borderRadius: '8px',
                background: 'rgba(245, 158, 11, 0.08)',
                border: '1px solid rgba(245, 158, 11, 0.25)',
                fontSize: '0.78rem',
                color: 'var(--text-secondary)',
                lineHeight: 1.45,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontWeight: 700, color: 'var(--accent-amber, #d97706)', marginBottom: '0.25rem' }}>
                <AlertTriangle size={14} />
                <span>Uncertainty & Analytical Limitations</span>
              </div>
              {uncertainty && uncertainty.length > 0 ? (
                uncertainty.map((u, i) => (
                  <div key={i} style={{ marginTop: i > 0 ? '0.2rem' : 0 }}>
                    {u}
                  </div>
                ))
              ) : (
                <div>
                  Image alone cannot determine numerical pollutant concentration. Image alone cannot establish regulatory source causality.
                </div>
              )}
            </div>
          </div>
        </div>

        {/* =========================================================================
            METADATA & AUDIT PROVENANCE FOOTER
            ========================================================================= */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '0.75rem',
            paddingTop: '0.75rem',
            borderTop: '1px solid var(--border-subtle, #e2e8f0)',
            fontSize: '0.72rem',
            color: 'var(--text-muted)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Cpu size={12} />
              Model: <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{modelVersion || 'N/A'}</strong>
            </span>
            {visionAnalysis?.promptVersion && (
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <FileText size={12} />
                Prompt: <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{visionAnalysis.promptVersion}</strong>
              </span>
            )}
            {visionAnalysis?.analyzedAt && (
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <Clock size={12} />
                Analyzed: <strong style={{ color: 'var(--text-primary)' }}>{new Date(visionAnalysis.analyzedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</strong>
              </span>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'var(--accent-teal, #10b981)' }}>
            <ShieldCheck size={14} />
            <span>Audit-Verified Multimodal Pipeline</span>
          </div>
        </div>
      </div>

      {/* Zoom Modal */}
      {isZoomed && photoUrl && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 9999,
            background: 'rgba(15, 23, 42, 0.85)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2rem',
          }}
          onClick={() => setIsZoomed(false)}
        >
          <div
            style={{
              position: 'relative',
              maxWidth: '90vw',
              maxHeight: '90vh',
              borderRadius: '12px',
              overflow: 'hidden',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={photoUrl}
              alt="Enlarged citizen observation photo"
              style={{
                maxWidth: '100%',
                maxHeight: '85vh',
                objectFit: 'contain',
                display: 'block',
              }}
            />
            <button
              type="button"
              onClick={() => setIsZoomed(false)}
              style={{
                position: 'absolute',
                top: '12px',
                right: '12px',
                background: 'rgba(15, 23, 42, 0.8)',
                color: '#ffffff',
                border: '1px solid rgba(255, 255, 255, 0.3)',
                borderRadius: '50%',
                width: '36px',
                height: '36px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
              aria-label="Close enlarged photo"
            >
              <X size={18} />
            </button>
          </div>
        </div>
      )}
    </Card>
  );
};
