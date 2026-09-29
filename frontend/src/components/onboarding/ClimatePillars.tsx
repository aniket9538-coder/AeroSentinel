import React from 'react';
import { Link } from 'react-router-dom';
import { Flame, TrendingUp, Brain, ChevronRight } from 'lucide-react';

export const ClimatePillars: React.FC = () => {
  const pillars = [
    {
      icon: Flame,
      iconBg: '#fef2f2',
      iconColor: '#ef4444',
      name: 'DETECT',
      badge: 'Spatial Intelligence',
      badgeBg: 'rgba(239, 68, 68, 0.1)',
      badgeColor: '#dc2626',
      title: 'Potential Pollution Hotspots',
      description:
        'Cross-references spatial clusters, weather stagnation, and thermal anomalies on H3 cells. Identifies high-risk emergence zones without making unfounded source claims.',
      metricLabel: 'Risk Detection Precision',
      metricValue: 'Analysis-Ready',
      linkText: 'Explore Hotspot Engine',
      linkTo: '/hotspots',
    },
    {
      icon: TrendingUp,
      iconBg: '#eff6ff',
      iconColor: '#0284c7',
      name: 'FORECAST',
      badge: '1–6 Hour Horizon',
      badgeBg: 'rgba(2, 132, 199, 0.1)',
      badgeColor: '#0284c7',
      title: 'Short-Term PM2.5 Prediction',
      description:
        'Gradient boosted ensemble projects hourly PM2.5 trajectories. Informs municipal leadership hours ahead so interventions can be deployed before pollution peaks.',
      metricLabel: 'Lead-Time Window',
      metricValue: '2 to 4 Hours (Target)',
      linkText: 'View Forecast Models',
      linkTo: '/forecast',
    },
    {
      icon: Brain,
      iconBg: '#faf5ff',
      iconColor: '#9333ea',
      name: 'EXPLAIN',
      badge: 'Gemini Multimodal',
      badgeBg: 'rgba(245, 158, 11, 0.12)',
      badgeColor: '#b45309',
      title: 'Evidence + Gemini WHY',
      description:
        'Gemini 2.5 synthesizes cross-sensor evidence graphs into clear, physical explanations of atmospheric dispersion and thermal drivers without replacing scientific calculations.',
      metricLabel: 'Reasoning Architecture',
      metricValue: 'Evidence-Grounded',
      linkText: 'Inspect Evidence Synthesis',
      linkTo: '/analyst/evidence',
    },
  ];

  return (
    <section className="onboarding-pillars-section">
      {/* Title & Subtitle */}
      <div className="onboarding-section-title-wrap">
        <h2 className="onboarding-section-title">Three Pillars of Climate Action</h2>
        <p className="onboarding-section-sub">
          Built specifically for environmental analysts, municipal authorities, and informed citizens.
        </p>
      </div>

      {/* 3 Pillars Grid */}
      <div className="onboarding-pillars-grid">
        {pillars.map((pillar, idx) => {
          const Icon = pillar.icon;
          return (
            <div key={idx} className="onboarding-pillar-card">
              {/* Header */}
              <div className="onboarding-pillar-top">
                <div className="onboarding-pillar-badge-group">
                  <div
                    className="onboarding-pillar-icon"
                    style={{ background: pillar.iconBg, color: pillar.iconColor }}
                  >
                    <Icon size={18} strokeWidth={2.2} />
                  </div>
                  <span className="onboarding-pillar-tag-name" style={{ color: pillar.iconColor }}>
                    {pillar.name}
                  </span>
                </div>
                <span
                  className="onboarding-pillar-pill"
                  style={{ background: pillar.badgeBg, color: pillar.badgeColor }}
                >
                  {pillar.badge}
                </span>
              </div>

              {/* Title & Description */}
              <h3 className="onboarding-pillar-heading">{pillar.title}</h3>
              <p className="onboarding-pillar-text">{pillar.description}</p>

              {/* Metric Box */}
              <div className="onboarding-pillar-metric-box">
                <span className="onboarding-pillar-metric-lbl">{pillar.metricLabel}</span>
                <span className="onboarding-pillar-metric-val">{pillar.metricValue}</span>
              </div>

              {/* Navigation Action */}
              <Link to={pillar.linkTo} className="onboarding-pillar-link">
                <span>{pillar.linkText}</span>
                <ChevronRight size={15} />
              </Link>
            </div>
          );
        })}
      </div>
    </section>
  );
};
