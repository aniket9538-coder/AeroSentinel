import React from 'react';
import { Link } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { Wind, Shield, Eye, Cpu, ArrowRight } from 'lucide-react';

export const Home: React.FC = () => {
  return (
    <PageContainer>
      <div style={{ textAlign: 'center', padding: '4rem 1rem 3rem 1rem' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '0.35rem 0.9rem',
          borderRadius: '9999px',
          background: 'rgba(6, 182, 212, 0.1)',
          border: '1px solid rgba(6, 182, 212, 0.25)',
          color: '#38bdf8',
          fontSize: '0.8rem',
          fontWeight: 600,
          marginBottom: '1.5rem',
        }}>
          <Wind size={14} /> AI-Powered Climate Intelligence Platform
        </div>
        <h1 style={{ fontSize: '3rem', fontWeight: 800, letterSpacing: '-0.03em', lineHeight: 1.15, maxWidth: '850px', margin: '0 auto', color: '#f9fafb' }}>
          Hyperlocal Pollution Hotspot Detection & Early Action
        </h1>
        <p style={{ fontSize: '1.15rem', color: '#9ca3af', maxWidth: '680px', margin: '1.5rem auto 2rem auto', lineHeight: 1.6 }}>
          AeroSentinel combines ground sensors, NASA FIRMS fire detections, Sentinel-5P satellite indicators, and citizen evidence into Uber H3 spatial hexes—enabling 1–6 hour predictive forecasts and rapid municipal response.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem' }}>
          <Link to="/map">
            <Button size="lg" variant="primary">
              Explore Live Map <ArrowRight size={16} style={{ marginLeft: '0.5rem' }} />
            </Button>
          </Link>
          <Link to="/analyst/hotspots">
            <Button size="lg" variant="secondary">
              Analyst Hotspots
            </Button>
          </Link>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem', marginTop: '2rem' }}>
        <Card title="H3 Spatial Discretization" subtitle="Sub-kilometer precision">
          <p style={{ fontSize: '0.875rem', color: '#9ca3af', lineHeight: 1.5 }}>
            Transforms unstructured urban space into discrete hexagonal cells, aggregating cross-source pollution signals and eliminating blind spots.
          </p>
        </Card>

        <Card title="Short-Term (1–6h) Forecaster" subtitle="Proactive, not historical">
          <p style={{ fontSize: '0.875rem', color: '#9ca3af', lineHeight: 1.5 }}>
            Gradient boosted ensemble forecasting projects near-future PM2.5 trajectories hours before severe localized exposure occurs.
          </p>
        </Card>

        <Card title="Google Gemini Reasoning" subtitle="Multimodal evidence synthesis">
          <p style={{ fontSize: '0.875rem', color: '#9ca3af', lineHeight: 1.5 }}>
            Processes citizen emission imagery with Gemini Vision and produces human-readable diagnostic briefs explaining the physical drivers of each hotspot.
          </p>
        </Card>

        <Card title="Multi-City Federated Node" subtitle="Privacy-preserving learning">
          <p style={{ fontSize: '0.875rem', color: '#9ca3af', lineHeight: 1.5 }}>
            Coordinates decentralized model training across Pune, Mumbai, and Delhi without centralizing raw citizen or municipal logs.
          </p>
        </Card>
      </div>
    </PageContainer>
  );
};
