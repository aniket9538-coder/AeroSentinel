import React from 'react';
import { Building2, Radio, Cloud, Satellite } from 'lucide-react';

export const HeroMetrics: React.FC = () => {
  const metrics = [
    {
      icon: Building2,
      value: '3',
      label: 'Cities',
      iconBg: 'rgba(168, 85, 247, 0.12)',
      iconColor: '#9333ea',
    },
    {
      icon: Radio,
      value: '1,200+',
      label: 'Monitoring Points',
      iconBg: 'rgba(20, 184, 166, 0.12)',
      iconColor: '#0d9488',
    },
    {
      icon: Cloud,
      value: 'Live',
      label: 'Weather Data',
      iconBg: 'rgba(2, 132, 199, 0.12)',
      iconColor: '#0284c7',
    },
    {
      icon: Satellite,
      value: 'Satellite',
      label: 'Fire Detections',
      iconBg: 'rgba(236, 72, 153, 0.12)',
      iconColor: '#db2777',
    },
  ];

  return (
    <div className="hero-metrics-strip" role="region" aria-label="System coverage metrics">
      {metrics.map((item, idx) => {
        const Icon = item.icon;
        return (
          <div key={idx} className="hero-metric-item">
            <div
              className="hero-metric-icon-wrap"
              style={{ background: item.iconBg, color: item.iconColor }}
              aria-hidden="true"
            >
              <Icon size={17} strokeWidth={2.2} />
            </div>
            <div className="hero-metric-text">
              <span className="hero-metric-val">{item.value}</span>
              <span className="hero-metric-lbl">{item.label}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
};
