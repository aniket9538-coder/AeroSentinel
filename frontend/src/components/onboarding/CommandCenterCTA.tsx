import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';

export const CommandCenterCTA: React.FC = () => {
  const navigate = useNavigate();

  return (
    <section className="onboarding-cta-section">
      <div className="onboarding-cta-card">
        {/* Subtle Mountain Silhouette SVG Background */}
        <svg
          className="onboarding-cta-bg-mountains"
          viewBox="0 0 1200 120"
          preserveAspectRatio="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Back Mountain Ridge */}
          <path
            d="M 0,120 L 0,80 L 150,45 L 320,85 L 500,30 L 680,75 L 850,20 L 1020,65 L 1200,35 L 1200,120 Z"
            fill="#38bdf8"
            opacity="0.35"
          />
          {/* Front Mountain Ridge */}
          <path
            d="M 0,120 L 0,95 L 100,75 L 240,100 L 410,60 L 590,90 L 760,50 L 930,85 L 1100,55 L 1200,70 L 1200,120 Z"
            fill="#0284c7"
            opacity="0.5"
          />
        </svg>

        {/* Content */}
        <div className="onboarding-cta-content">
          <h2 className="onboarding-cta-heading">
            From pollution data to early action.
          </h2>
          <p className="onboarding-cta-sub">
            Join municipal authorities, atmospheric researchers, and citizens in building proactive climate resilience.
          </p>

          <button
            className="onboarding-cta-btn"
            onClick={() => navigate('/dashboard')}
            type="button"
          >
            <span>Enter AeroSentinel Command Center</span>
            <ArrowRight size={18} />
          </button>
        </div>
      </div>
    </section>
  );
};
