import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Wind,
  Shield,
  ArrowRight,
  Flame,
  TrendingUp,
  Brain,
  Layers,
  MapPin,
  ChevronRight,
  Sparkles,
  Compass,
  Radio,
  Eye,
  CheckCircle2,
} from 'lucide-react';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';

export const Home: React.FC = () => {
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState<number>(0);

  const pipelineSteps = [
    {
      phase: 'OBSERVE',
      tag: '01 / Ingestion',
      title: 'Multi-Source Data Fusion',
      description: 'Continuous ingestion from CAAQMS ground sensors, IMD microclimate feeds, NASA FIRMS thermal hotspots, and Sentinel-5P tropospheric gas columns.',
      badges: ['CPCB Ground Sensors', 'NASA FIRMS Fires', 'Sentinel-5P NO2', 'IMD Weather Vectors', 'Citizen Reports'],
      accent: 'var(--brand-primary)',
    },
    {
      phase: 'INTELLIGENCE',
      tag: '02 / Discretization & ML',
      title: 'H3 Spatial Discretization & Predictive Risk',
      description: 'Raw telemetry is mapped onto Uber H3 Resolution-8 hexagonal cells (~0.7 km²). XGBoost ensemble models evaluate spatial risk and project PM2.5 1–6 hours ahead.',
      badges: ['H3 Hexagonal Grid (Res 8)', 'XGBoost Hotspot Classifier', 'Temporal Gradient Boosting', 'Spatial Lag Matrices'],
      accent: 'var(--accent-teal)',
    },
    {
      phase: 'EXPLAIN',
      tag: '03 / Multimodal Reasoning',
      title: 'Evidence Corroboration & Gemini WHY',
      description: 'Synthesizes corroborating cross-sensor signals into an evidence timeline. Google Gemini 2.5 Flash produces plain-English physical diagnostic briefs for analysts.',
      badges: ['Evidence Graph Triangulation', 'Gemini 2.5 Diagnostic Briefs', 'Uncertainty Disclosures', 'Gemini Vision Inspection'],
      accent: 'var(--accent-purple)',
    },
    {
      phase: 'ACT',
      tag: '04 / Municipal Action',
      title: 'Targeted Early Action & Enforcement Triage',
      description: 'Automated alert routing directly to municipal pollution control boards. Dispatches field inspection teams and anti-smog equipment hours before severe exposure peaks.',
      badges: ['Automated Threshold Alerts', 'Inspection Team Dispatch', 'Anti-Smog Mobilization', 'Federated City Node Sync'],
      accent: 'var(--accent-amber)',
    },
  ];

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', position: 'relative', overflow: 'hidden' }}>
      {/* Ambient Atmospheric Background Graphic */}
      <div
        style={{
          position: 'absolute',
          top: '-15%',
          left: '50%',
          transform: 'translateX(-50%)',
          width: '1200px',
          height: '700px',
          background: 'radial-gradient(ellipse at center, rgba(56, 189, 248, 0.12) 0%, rgba(16, 185, 129, 0.05) 45%, transparent 70%)',
          filter: 'blur(80px)',
          pointerEvents: 'none',
          zIndex: 0,
        }}
      />

      {/* Main Container */}
      <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '3rem 2rem 5rem 2rem', position: 'relative', zIndex: 1 }}>
        {/* =========================================================================
            HERO SECTION
            ========================================================================= */}
        <div style={{ textAlign: 'center', maxWidth: '960px', margin: '0 auto', padding: '2rem 0 3.5rem 0' }}>
          {/* Top Pill */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.6rem',
              padding: '0.4rem 1.1rem',
              borderRadius: '9999px',
              background: 'var(--brand-surface)',
              border: '1px solid var(--brand-border)',
              color: 'var(--brand-primary)',
              fontSize: '0.825rem',
              fontWeight: 700,
              letterSpacing: '0.04em',
              marginBottom: '1.75rem',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <Sparkles size={15} />
            <span>AI-POWERED HYPERLOCAL POLLUTION INTELLIGENCE</span>
          </div>

          {/* Hero Heading */}
          <h1
            style={{
              fontSize: 'clamp(2.75rem, 6vw, 4.25rem)',
              fontWeight: 800,
              fontFamily: 'var(--font-heading)',
              letterSpacing: '-0.035em',
              lineHeight: 1.08,
              color: 'var(--text-primary)',
              marginBottom: '1.5rem',
            }}
          >
            Detect emerging pollution risk.{' '}
            <span
              style={{
                background: 'linear-gradient(135deg, var(--brand-primary), var(--accent-teal))',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              Understand the evidence.
            </span>{' '}
            Act before the spike.
          </h1>

          {/* Subtitle */}
          <p
            style={{
              fontSize: '1.2rem',
              color: 'var(--text-secondary)',
              lineHeight: 1.6,
              maxWidth: '780px',
              margin: '0 auto 2.5rem auto',
            }}
          >
            AeroSentinel transforms fragmented environmental signals into predictive sub-kilometer
            intelligence. Discretized into Uber H3 spatial hexes, powered by gradient-boosted
            forecasting and Google Gemini multimodal reasoning for proactive municipal action.
          </p>

          {/* Hero Action Buttons */}
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap' }}>
            <Button
              size="lg"
              variant="primary"
              onClick={() => navigate('/dashboard')}
              style={{
                padding: '0.85rem 2rem',
                fontSize: '1.05rem',
                boxShadow: '0 4px 20px rgba(56, 189, 248, 0.4)',
              }}
            >
              <span>GET STARTED</span>
              <ArrowRight size={18} style={{ marginLeft: '0.35rem' }} />
            </Button>

            <Button
              size="lg"
              variant="secondary"
              onClick={() => navigate('/map')}
              style={{ padding: '0.85rem 1.8rem', fontSize: '1.05rem' }}
            >
              <Compass size={18} style={{ marginRight: '0.5rem', color: 'var(--brand-primary)' }} />
              <span>EXPLORE LIVE INTELLIGENCE</span>
            </Button>
          </div>
        </div>

        {/* =========================================================================
            HERO VISUAL: ATMOSPHERIC & SPATIAL PIPELINE ARCHITECTURE
            ========================================================================= */}
        <div
          style={{
            margin: '2rem auto 4.5rem auto',
            padding: '2rem',
            borderRadius: '20px',
            background: 'var(--bg-card)',
            border: '1px solid var(--border-medium)',
            boxShadow: 'var(--shadow-lg)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            maxWidth: '1200px',
          }}
        >
          {/* Header of Visual */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--aqi-good)' }} className="live-indicator-dot" />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, letterSpacing: '0.04em', color: 'var(--text-primary)' }}>
                CONTINUOUS CLIMATE INTELLIGENCE PIPELINE
              </span>
            </div>
            <Badge variant="info">Multi-Source Cross-Corroboration</Badge>
          </div>

          {/* Interactive Flow Diagram */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '1.25rem',
              position: 'relative',
            }}
          >
            {/* Step 1: Raw Signals */}
            <div
              style={{
                padding: '1.25rem',
                borderRadius: '14px',
                background: 'var(--bg-surface-elevated)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Stage 01
              </div>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.3rem' }}>
                Multi-Source Streams
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.5rem', lineHeight: 1.45 }}>
                Ground sensors (CPCB/MPCB), NASA FIRMS thermal fires, Sentinel-5P NO2, and IMD weather vector inputs.
              </div>
              <div style={{ marginTop: '0.85rem', display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', background: 'rgba(56, 189, 248, 0.1)', color: 'var(--brand-primary)', fontWeight: 600 }}>Air</span>
                <span style={{ fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--accent-teal)', fontWeight: 600 }}>Weather</span>
                <span style={{ fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', background: 'rgba(245, 158, 11, 0.1)', color: 'var(--accent-amber)', fontWeight: 600 }}>FIRMS</span>
                <span style={{ fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', background: 'rgba(168, 85, 247, 0.1)', color: 'var(--accent-purple)', fontWeight: 600 }}>Sentinel-5P</span>
              </div>
            </div>

            {/* Step 2: H3 Discretization */}
            <div
              style={{
                padding: '1.25rem',
                borderRadius: '14px',
                background: 'var(--bg-surface-elevated)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ fontSize: '0.7rem', color: 'var(--brand-primary)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Stage 02
              </div>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.3rem' }}>
                H3 Spatial Discretization
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.5rem', lineHeight: 1.45 }}>
                Eliminates urban sensor blind spots by indexing unstructured telemetry into discrete Uber H3 Resolution-8 hexagonal cells.
              </div>
              <div style={{ marginTop: '0.85rem' }}>
                <Badge variant="info">Hex Resolution 8 (~0.7 km²)</Badge>
              </div>
            </div>

            {/* Step 3: AI Detection */}
            <div
              style={{
                padding: '1.25rem',
                borderRadius: '14px',
                background: 'var(--bg-surface-elevated)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ fontSize: '0.7rem', color: 'var(--accent-teal)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Stage 03
              </div>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.3rem' }}>
                AI Hotspot & Forecast
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.5rem', lineHeight: 1.45 }}>
                Trained gradient boosted models flag potential hotspot risks and project 1–6 hour PM2.5 trajectories before hazardous peaks occur.
              </div>
              <div style={{ marginTop: '0.85rem' }}>
                <Badge variant="warning">1–6h Predictive Horizon</Badge>
              </div>
            </div>

            {/* Step 4: Early Action */}
            <div
              style={{
                padding: '1.25rem',
                borderRadius: '14px',
                background: 'var(--bg-surface-elevated)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ fontSize: '0.7rem', color: 'var(--accent-amber)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Stage 04
              </div>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.3rem' }}>
                Reasoning & Rapid Action
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.5rem', lineHeight: 1.45 }}>
                Google Gemini synthesizes validated evidence into plain-English briefings. Municipal authorities dispatch targeted anti-smog equipment.
              </div>
              <div style={{ marginTop: '0.85rem' }}>
                <Badge variant="success">Field Verification Triage</Badge>
              </div>
            </div>
          </div>
        </div>

        {/* =========================================================================
            THREE PRIMARY FEATURE CARDS
            [ DETECT ] | [ FORECAST ] | [ EXPLAIN ]
            ========================================================================= */}
        <div style={{ marginBottom: '5rem' }}>
          <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '2.2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
              Three Pillars of Climate Action
            </h2>
            <p style={{ fontSize: '1rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
              Built specifically for environmental analysts, municipal authorities, and informed citizens.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '1.75rem',
            }}
          >
            {/* Feature 1: DETECT */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'rgba(239, 68, 68, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-rose)' }}>
                    <Flame size={18} />
                  </div>
                  <span style={{ fontSize: '1.15rem', fontWeight: 700 }}>DETECT</span>
                </div>
              }
              badge={<Badge variant="danger">Spatial Intelligence</Badge>}
            >
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0.5rem 0 0.35rem 0' }}>
                Potential Pollution Hotspots
              </div>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1.25rem' }}>
                Cross-references spatial clusters, weather stagnation, and thermal anomalies on H3 cells.
                Identifies high-risk emergence zones without making unfounded source claims.
              </p>
              <div style={{ padding: '0.85rem', borderRadius: '10px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Risk Detection Precision</span>
                  <strong style={{ color: 'var(--text-primary)' }}>84% Corroborated</strong>
                </div>
              </div>
              <Link to="/hotspots" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)', fontWeight: 600, fontSize: '0.85rem', marginTop: '1rem' }}>
                Explore Hotspot Engine <ChevronRight size={14} />
              </Link>
            </Card>

            {/* Feature 2: FORECAST */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--brand-primary)' }}>
                    <TrendingUp size={18} />
                  </div>
                  <span style={{ fontSize: '1.15rem', fontWeight: 700 }}>FORECAST</span>
                </div>
              }
              badge={<Badge variant="info">1–6 Hour Horizon</Badge>}
            >
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0.5rem 0 0.35rem 0' }}>
                Short-Term PM2.5 Prediction
              </div>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1.25rem' }}>
                Gradient boosted ensemble projects hourly PM2.5 trajectories. Informs municipal leadership
                hours ahead so interventions can be deployed before pollution peaks.
              </p>
              <div style={{ padding: '0.85rem', borderRadius: '10px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Lead-Time Window</span>
                  <strong style={{ color: 'var(--text-primary)' }}>2 to 4 Hours Advance</strong>
                </div>
              </div>
              <Link to="/forecast" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)', fontWeight: 600, fontSize: '0.85rem', marginTop: '1rem' }}>
                View Forecast Models <ChevronRight size={14} />
              </Link>
            </Card>

            {/* Feature 3: EXPLAIN */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'rgba(168, 85, 247, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-purple)' }}>
                    <Brain size={18} />
                  </div>
                  <span style={{ fontSize: '1.15rem', fontWeight: 700 }}>EXPLAIN</span>
                </div>
              }
              badge={<Badge variant="warning">Gemini Multimodal</Badge>}
            >
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0.5rem 0 0.35rem 0' }}>
                Evidence + Gemini WHY
              </div>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1.25rem' }}>
                Gemini 2.5 synthesizes cross-sensor evidence graphs into clear, physical explanations of
                atmospheric dispersion and thermal drivers without replacing scientific calculations.
              </p>
              <div style={{ padding: '0.85rem', borderRadius: '10px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Reasoning Architecture</span>
                  <strong style={{ color: 'var(--text-primary)' }}>Evidence-Grounded</strong>
                </div>
              </div>
              <Link to="/analyst/evidence" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)', fontWeight: 600, fontSize: '0.85rem', marginTop: '1rem' }}>
                Inspect Evidence Synthesis <ChevronRight size={14} />
              </Link>
            </Card>
          </div>
        </div>

        {/* =========================================================================
            PRODUCT FLOW INTERACTIVE STORYTELLING
            OBSERVE → INTELLIGENCE → EXPLAIN → ACT
            ========================================================================= */}
        <div style={{ marginBottom: '5rem' }}>
          <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '2.2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
              How AeroSentinel Operates
            </h2>
            <p style={{ fontSize: '1rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
              A closed-loop workflow connecting raw atmospheric observation to municipal field execution.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: '0.5rem',
              background: 'var(--bg-surface-elevated)',
              padding: '0.4rem',
              borderRadius: '12px',
              border: '1px solid var(--border-subtle)',
              marginBottom: '1.5rem',
            }}
          >
            {pipelineSteps.map((step, idx) => (
              <button
                key={step.phase}
                onClick={() => setActiveStep(idx)}
                style={{
                  padding: '0.75rem 1rem',
                  borderRadius: '8px',
                  background: activeStep === idx ? 'var(--bg-surface)' : 'transparent',
                  border: activeStep === idx ? '1px solid var(--border-medium)' : '1px solid transparent',
                  color: activeStep === idx ? 'var(--text-primary)' : 'var(--text-muted)',
                  fontWeight: activeStep === idx ? 700 : 500,
                  fontSize: '0.875rem',
                  cursor: 'pointer',
                  textAlign: 'center',
                  transition: 'all 0.15s ease',
                  boxShadow: activeStep === idx ? 'var(--shadow-sm)' : 'none',
                }}
              >
                {step.phase}
              </button>
            ))}
          </div>

          {/* Active Flow Detail Card */}
          <div
            style={{
              padding: '2rem',
              borderRadius: '16px',
              background: 'var(--bg-card)',
              border: '1px solid var(--border-medium)',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: pipelineSteps[activeStep].accent, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                {pipelineSteps[activeStep].tag}
              </span>
              <Badge variant="neutral">Automated Step</Badge>
            </div>
            <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.75rem' }}>
              {pipelineSteps[activeStep].title}
            </h3>
            <p style={{ fontSize: '1rem', color: 'var(--text-secondary)', lineHeight: 1.6, maxWidth: '840px', marginBottom: '1.5rem' }}>
              {pipelineSteps[activeStep].description}
            </p>
            <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}>
              {pipelineSteps[activeStep].badges.map((b) => (
                <span
                  key={b}
                  style={{
                    padding: '0.35rem 0.75rem',
                    borderRadius: '6px',
                    background: 'var(--bg-surface-elevated)',
                    border: '1px solid var(--border-subtle)',
                    color: 'var(--text-primary)',
                    fontSize: '0.8rem',
                    fontWeight: 500,
                  }}
                >
                  ✓ {b}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* =========================================================================
            FINAL CALL TO ACTION
            "From pollution data to early action."
            ========================================================================= */}
        <div
          style={{
            padding: '3.5rem 2rem',
            borderRadius: '24px',
            background: 'linear-gradient(135deg, rgba(56, 189, 248, 0.12), rgba(16, 185, 129, 0.08))',
            border: '1px solid var(--brand-border)',
            textAlign: 'center',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', letterSpacing: '-0.025em' }}>
            From pollution data to early action.
          </h2>
          <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', maxWidth: '600px', margin: '0.85rem auto 2rem auto', lineHeight: 1.5 }}>
            Join municipal authorities, atmospheric researchers, and citizens in building proactive climate resilience.
          </p>

          <Button
            size="lg"
            variant="primary"
            onClick={() => navigate('/dashboard')}
            style={{
              padding: '0.9rem 2.5rem',
              fontSize: '1.1rem',
              boxShadow: '0 4px 25px rgba(56, 189, 248, 0.45)',
            }}
          >
            Enter AeroSentinel Command Center
            <ArrowRight size={18} style={{ marginLeft: '0.4rem' }} />
          </Button>
        </div>
      </div>
    </div>
  );
};
