import React from 'react';
import { Database, Hexagon, Brain, Cog, ArrowRight } from 'lucide-react';

export const IntelligencePipeline: React.FC = () => {
  const stages = [
    {
      stageNum: 'STAGE 01',
      title: 'Multi-Source Streams',
      description:
        'Ground sensors (CPCB/MPCB), NASA FIRMS thermal fires, Sentinel-5P NO2, and IMD weather vector inputs.',
      icon: Database,
      iconBg: '#eff6ff',
      iconColor: '#0284c7',
      numColor: '#0284c7',
      tags: [
        { label: 'Air', bg: 'rgba(56, 189, 248, 0.12)', color: '#0284c7' },
        { label: 'Weather', bg: 'rgba(16, 185, 129, 0.12)', color: '#059669' },
        { label: 'FIRMS', bg: 'rgba(245, 158, 11, 0.12)', color: '#d97706' },
        { label: 'Sentinel-5P', bg: 'rgba(168, 85, 247, 0.12)', color: '#9333ea' },
      ],
    },
    {
      stageNum: 'STAGE 02',
      title: 'H3 Spatial Discretization',
      description:
        'Eliminates urban sensor blind spots by indexing unstructured telemetry into discrete Uber H3 Resolution-8 hexagonal cells.',
      icon: Hexagon,
      iconBg: '#eff6ff',
      iconColor: '#0284c7',
      numColor: '#0284c7',
      tags: [
        { label: 'Hex Resolution 8 (~0.7 km²)', bg: 'rgba(2, 132, 199, 0.1)', color: '#0284c7' },
      ],
    },
    {
      stageNum: 'STAGE 03',
      title: 'AI Hotspot & Forecast',
      description:
        'Trained gradient boosted models flag potential hotspot risks and project 1-6 hour PM2.5 trajectories before hazardous peaks occur.',
      icon: Brain,
      iconBg: '#faf5ff',
      iconColor: '#9333ea',
      numColor: '#9333ea',
      tags: [
        { label: '1-6h Predictive Horizon', bg: 'rgba(245, 158, 11, 0.12)', color: '#d97706' },
      ],
    },
    {
      stageNum: 'STAGE 04',
      title: 'Reasoning & Rapid Action',
      description:
        'Google Gemini synthesizes validated evidence into plain-English briefings. Municipal authorities dispatch targeted anti-smog equipment.',
      icon: Cog,
      iconBg: '#fff7ed',
      iconColor: '#ea580c',
      numColor: '#ea580c',
      tags: [
        { label: 'Field Verification Triage', bg: 'rgba(16, 185, 129, 0.12)', color: '#059669' },
      ],
    },
  ];

  return (
    <section className="onboarding-pipeline-section">
      <div className="onboarding-pipeline-box">
        {/* Header */}
        <div className="onboarding-pipeline-header">
          <div className="onboarding-pipeline-title">
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
            <span>CONTINUOUS CLIMATE INTELLIGENCE PIPELINE</span>
          </div>
          <span className="onboarding-pipeline-pill">
            Multi-Source Cross-Corroboration
          </span>
        </div>

        {/* 4 Pipeline Stages */}
        <div className="onboarding-pipeline-grid">
          {stages.map((stage, idx) => {
            const Icon = stage.icon;
            return (
              <div key={idx} className="onboarding-stage-card">
                <div className="onboarding-stage-top">
                  <div
                    className="onboarding-stage-icon"
                    style={{ background: stage.iconBg, color: stage.iconColor }}
                  >
                    <Icon size={18} strokeWidth={2.2} />
                  </div>
                  <div>
                    <span
                      className="onboarding-stage-num"
                      style={{ color: stage.numColor }}
                    >
                      {stage.stageNum}
                    </span>
                    <h3 className="onboarding-stage-name">{stage.title}</h3>
                  </div>
                </div>

                <p className="onboarding-stage-desc">{stage.description}</p>

                <div className="onboarding-stage-tags">
                  {stage.tags.map((tag, tIdx) => (
                    <span
                      key={tIdx}
                      className="onboarding-stage-tag"
                      style={{ background: tag.bg, color: tag.color }}
                    >
                      {tag.label}
                    </span>
                  ))}
                </div>

                {/* Connecting Arrow for Desktop */}
                {idx < stages.length - 1 && (
                  <div
                    style={{
                      position: 'absolute',
                      right: '-14px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      zIndex: 2,
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: 'var(--bg-surface)',
                      border: '1px solid var(--border-medium)',
                      display: 'none', // shown via media query or kept subtle
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--text-muted)',
                      boxShadow: '0 2px 6px rgba(0,0,0,0.06)',
                    }}
                  >
                    <ArrowRight size={12} />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
