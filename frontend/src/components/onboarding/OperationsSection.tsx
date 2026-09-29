import React, { useState } from 'react';
import {
  Layers,
  Brain,
  FileText,
  Send,
  Check,
  Radio,
  Satellite,
  Cloud,
  Camera,
} from 'lucide-react';

export const OperationsSection: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'OBSERVE' | 'INTELLIGENCE' | 'EXPLAIN' | 'ACT'>('OBSERVE');

  const tabs = [
    { id: 'OBSERVE' as const, label: 'OBSERVE', icon: Layers },
    { id: 'INTELLIGENCE' as const, label: 'INTELLIGENCE', icon: Brain },
    { id: 'EXPLAIN' as const, label: 'EXPLAIN', icon: FileText },
    { id: 'ACT' as const, label: 'ACT', icon: Send },
  ];

  const contentMap = {
    OBSERVE: {
      tag: '01 / INGESTION',
      title: 'Multi-Source Data Fusion',
      badge: 'Automated Step',
      description:
        'Continuous ingestion from CAAQMS ground sensors, IMD microclimate feeds, NASA FIRMS thermal hotspots, and Sentinel-5P tropospheric gas columns.',
      chips: [
        'CPCB Ground Sensors',
        'NASA FIRMS Fires',
        'Sentinel-5P NO2',
        'IMD Weather Vectors',
        'Citizen Reports',
      ],
    },
    INTELLIGENCE: {
      tag: '02 / DISCRETIZATION & PREDICTION',
      title: 'H3 Hexagonal Discretization & ML Forecasting',
      badge: 'Spatial Processing',
      description:
        'Eliminates urban sensor blind spots by projecting heterogeneous environmental feeds onto deterministic Uber H3 Resolution-8 hexagonal cells (~0.7 km²).',
      chips: [
        'Uber H3 Resolution-8 Grid',
        'Spatial Stagnation Scoring',
        'Gradient Boosted Models (Planned)',
        'Temporal Lag Ingestion',
      ],
    },
    EXPLAIN: {
      tag: '03 / MULTIMODAL REASONING',
      title: 'Evidence Triangulation & Gemini WHY',
      badge: 'Evidence Synthesis',
      description:
        'Synthesizes cross-sensor evidence graphs into clear, physical explanations of atmospheric dispersion and thermal drivers without replacing scientific calculations.',
      chips: [
        'Evidence Graph Triangulation',
        'Atmospheric Physics Context',
        'Gemini Multimodal Briefs (Planned)',
        'Confidence Scoring',
      ],
    },
    ACT: {
      tag: '04 / MUNICIPAL DISPATCH',
      title: 'Targeted Early Intervention & Anti-Smog Mobilization',
      badge: 'Operational Triage',
      description:
        'Automated threshold alerts route directly to municipal pollution control boards to dispatch field inspection teams and anti-smog equipment hours before severe exposure peaks.',
      chips: [
        'Automated Alert Routing',
        'Anti-Smog Gun Mobilization',
        'Field Inspection Queues',
        'Federated Network Broadcast',
      ],
    },
  };

  const current = contentMap[activeTab];

  return (
    <section id="how-aerosentinel-operates" className="onboarding-ops-section">
      {/* Title & Subtitle */}
      <div className="onboarding-section-title-wrap">
        <h2 className="onboarding-section-title">How AeroSentinel Operates</h2>
        <p className="onboarding-section-sub">
          A closed-loop workflow connecting raw atmospheric observation to municipal field execution.
        </p>
      </div>

      {/* Tabs */}
      <div className="onboarding-ops-tabs">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              className={`onboarding-ops-tab ${isActive ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
              type="button"
            >
              <Icon size={16} strokeWidth={isActive ? 2.5 : 2} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Panel */}
      <div className="onboarding-ops-panel">
        {/* Left Side: Text and Chips */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span className="onboarding-ops-tag">{current.tag}</span>
            <span
              style={{
                fontSize: '0.6875rem',
                fontWeight: 600,
                padding: '0.2rem 0.6rem',
                borderRadius: '6px',
                background: 'var(--brand-surface)',
                color: 'var(--brand-primary)',
              }}
            >
              {current.badge}
            </span>
          </div>

          <h3 className="onboarding-ops-title">{current.title}</h3>
          <p className="onboarding-ops-desc">{current.description}</p>

          <div className="onboarding-source-chips">
            {current.chips.map((chip, idx) => (
              <div key={idx} className="onboarding-source-chip">
                <Check size={14} style={{ color: '#0284c7' }} />
                <span>{chip}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Right Side: Animated Data Fusion Illustration */}
        <div className="onboarding-fusion-graphic">
          <svg
            viewBox="0 0 360 220"
            style={{ width: '100%', height: '100%' }}
            xmlns="http://www.w3.org/2000/svg"
          >
            <defs>
              <linearGradient id="streamGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#10b981" />
                <stop offset="100%" stopColor="#38bdf8" />
              </linearGradient>
              <linearGradient id="streamGrad2" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#0284c7" />
                <stop offset="100%" stopColor="#38bdf8" />
              </linearGradient>
              <linearGradient id="streamGrad3" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#a855f7" />
                <stop offset="100%" stopColor="#38bdf8" />
              </linearGradient>
              <linearGradient id="streamGrad4" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#38bdf8" />
                <stop offset="100%" stopColor="#0284c7" />
              </linearGradient>
            </defs>

            {/* Connecting Flow Lines to Central Hub */}
            {/* Stream 1: Ground Sensors (Top Left) */}
            <path
              d="M 60,45 C 120,45 130,110 180,110"
              fill="none"
              stroke="url(#streamGrad1)"
              strokeWidth="2"
              strokeDasharray="4 3"
            />
            {/* Stream 2: Satellite (Top Right) */}
            <path
              d="M 300,45 C 240,45 230,110 180,110"
              fill="none"
              stroke="url(#streamGrad2)"
              strokeWidth="2"
              strokeDasharray="4 3"
            />
            {/* Stream 3: Weather (Center Right) */}
            <path
              d="M 310,120 C 260,120 230,110 180,110"
              fill="none"
              stroke="url(#streamGrad4)"
              strokeWidth="2"
            />
            {/* Stream 4: Citizen Reports (Bottom Right) */}
            <path
              d="M 300,180 C 240,180 230,110 180,110"
              fill="none"
              stroke="url(#streamGrad3)"
              strokeWidth="2"
              strokeDasharray="4 3"
            />

            {/* Central Processing Stack (Layered 3D Discs) */}
            <g transform="translate(180, 110)">
              {/* Disc 3 (Bottom) */}
              <ellipse cx="0" cy="18" rx="30" ry="10" fill="#0369a1" />
              <path d="M -30,18 C -30,24 30,24 30,18 L 30,26 C 30,32 -30,32 -30,26 Z" fill="#0284c7" opacity="0.8" />
              
              {/* Disc 2 (Middle) */}
              <ellipse cx="0" cy="6" rx="28" ry="9" fill="#0ea5e9" />
              <path d="M -28,6 C -28,12 28,12 28,6 L 28,14 C 28,20 -28,20 -28,14 Z" fill="#38bdf8" opacity="0.8" />

              {/* Disc 1 (Top) */}
              <ellipse cx="0" cy="-6" rx="26" ry="8" fill="#38bdf8" />
              <circle cx="0" cy="-6" r="4" fill="#ffffff">
                <animate attributeName="opacity" values="0.5;1;0.5" dur="2s" repeatCount="indefinite" />
              </circle>
            </g>

            {/* Source Node 1: Ground Sensors */}
            <g transform="translate(45, 45)">
              <circle r="16" fill="#064e3b" stroke="#10b981" strokeWidth="1.5" />
              <text x="22" y="-2" fill="#e2e8f0" fontSize="9" fontWeight="700">Ground CAAQMS</text>
              <text x="22" y="10" fill="#94a3b8" fontSize="8">PM2.5, AQI</text>
            </g>

            {/* Source Node 2: Satellite */}
            <g transform="translate(315, 45)">
              <circle r="16" fill="#0c4a6e" stroke="#38bdf8" strokeWidth="1.5" />
              <text x="-75" y="-2" fill="#e2e8f0" fontSize="9" fontWeight="700">Satellite</text>
              <text x="-75" y="10" fill="#94a3b8" fontSize="8">Fires, NO2</text>
            </g>

            {/* Source Node 3: Weather */}
            <g transform="translate(325, 120)">
              <circle r="14" fill="#075985" stroke="#0284c7" strokeWidth="1.5" />
              <text x="-65" y="-2" fill="#e2e8f0" fontSize="9" fontWeight="700">Weather</text>
              <text x="-65" y="10" fill="#94a3b8" fontSize="8">Wind, Humidity</text>
            </g>

            {/* Source Node 4: Citizen Reports */}
            <g transform="translate(315, 180)">
              <circle r="16" fill="#4c1d95" stroke="#a855f7" strokeWidth="1.5" />
              <text x="-85" y="-2" fill="#e2e8f0" fontSize="9" fontWeight="700">Citizen Reports</text>
              <text x="-85" y="10" fill="#94a3b8" fontSize="8">Photos, Location</text>
            </g>
          </svg>
        </div>
      </div>
    </section>
  );
};
