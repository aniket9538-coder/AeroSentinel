import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { HotspotCard } from '../../components/hotspot/HotspotCard';
import { HotspotDetails } from '../../components/hotspot/HotspotDetails';
import { EvidencePanel } from '../../components/hotspot/EvidencePanel';
import { HotspotPrediction } from '../../types';

export const Hotspots: React.FC = () => {
  const sampleHotspots: HotspotPrediction[] = [
    {
      id: 'h1',
      h3Index: '8860144aa1fffff',
      cityId: 'pune',
      predictedAt: new Date().toISOString(),
      riskScore: 88,
      riskLevel: 'HIGH',
      confidence: 0.89,
      primaryFactors: ['Low wind stagnation', 'PM2.5 rising trend (+32%)', 'Thermal fire 1.4km upwind'],
      modelVersion: 'xgb-hotspot-v1.2',
    },
    {
      id: 'h2',
      h3Index: '8860144ab3fffff',
      cityId: 'pune',
      predictedAt: new Date().toISOString(),
      riskScore: 65,
      riskLevel: 'MEDIUM',
      confidence: 0.79,
      primaryFactors: ['Moderate traffic congestion', 'Industrial SO2 indicator'],
      modelVersion: 'xgb-hotspot-v1.2',
    },
  ];

  const [selectedHotspot, setSelectedHotspot] = useState<HotspotPrediction>(sampleHotspots[0]);
  const [isGeneratingBrief, setIsGeneratingBrief] = useState(false);
  const [aiBrief, setAiBrief] = useState<{ summary: string; signals: string[]; recommendedAction: string } | null>(null);

  const handleGenerateAiBrief = () => {
    setIsGeneratingBrief(true);
    setTimeout(() => {
      setAiBrief({
        summary: 'Elevated particulate concentration detected in Hex 8860144aa1fffff driven by boundary-layer inversion and active crop residue burning upwind.',
        signals: ['PM2.5 spike (+32% in 2h)', 'Wind speed 1.1 m/s (NE to SW)', 'FIRMS Fire Detection (Conf: 92%)'],
        recommendedAction: 'Dispatch Rapid Response Unit for local misting and water-cannon dampening.',
      });
      setIsGeneratingBrief(false);
    }, 1000);
  };

  return (
    <PageContainer
      title="Hyperlocal Hotspot Intelligence"
      subtitle="AI-detected pollution anomalies across Uber H3 hexagonal cells"
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1.5rem' }}>
        <div>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#9ca3af', marginBottom: '1rem' }}>
            Active Hotspot Cells ({sampleHotspots.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {sampleHotspots.map((h) => (
              <HotspotCard
                key={h.id}
                hotspot={h}
                isSelected={h.id === selectedHotspot.id}
                onClick={() => {
                  setSelectedHotspot(h);
                  setAiBrief(null);
                }}
              />
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <HotspotDetails hotspot={selectedHotspot} />
          <EvidencePanel
            onGenerateAiBrief={handleGenerateAiBrief}
            isLoadingAi={isGeneratingBrief}
            aiExplanation={aiBrief}
          />
        </div>
      </div>
    </PageContainer>
  );
};
