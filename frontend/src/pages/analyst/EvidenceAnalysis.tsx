import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { EvidencePanel } from '../../components/hotspot/EvidencePanel';

export const EvidenceAnalysis: React.FC = () => {
  return (
    <PageContainer
      title="Evidence Fusion & Causal Attribution"
      subtitle="Synthesize physical, chemical, and crowdsourced indicators"
    >
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <EvidencePanel
          aiExplanation={{
            summary: 'Particulate spike in cell 8860144aa1fffff corroborated by high FRP fire anomaly upwind and citizen verified visual emission.',
            signals: ['NASA FIRMS Fire Detection (FRP: 14.5 MW)', 'Surface wind: 1.1 m/s stagnation', 'Tropospheric NO2 column: elevated'],
            recommendedAction: 'Targeted water misting cannon deployment.',
          }}
        />
      </div>
    </PageContainer>
  );
};
