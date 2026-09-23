import React from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Sparkles, FileText, Flame, Wind, Eye } from 'lucide-react';

interface EvidencePanelProps {
  onGenerateAiBrief?: () => void;
  isLoadingAi?: boolean;
  aiExplanation?: { summary: string; signals: string[]; recommendedAction: string } | null;
}

export const EvidencePanel: React.FC<EvidencePanelProps> = ({
  onGenerateAiBrief,
  isLoadingAi = false,
  aiExplanation,
}) => {
  return (
    <Card
      title="Multi-Source Evidence Synthesis"
      subtitle="Heterogeneous signal corroboration chain"
      action={
        <Button variant="outline" size="sm" onClick={onGenerateAiBrief} isLoading={isLoadingAi}>
          <Sparkles size={14} style={{ marginRight: '0.4rem' }} />
          Gemini AI Brief
        </Button>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <Wind size={18} color="#06b6d4" />
          <div style={{ fontSize: '0.85rem' }}>
            <span style={{ fontWeight: 600 }}>Wind Dispersion:</span> Low ventilation coefficient (1.1 m/s stagnation)
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <Flame size={18} color="#ea580c" />
          <div style={{ fontSize: '0.85rem' }}>
            <span style={{ fontWeight: 600 }}>Thermal Anomaly:</span> NASA FIRMS active fire detection 1.4km upwind
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px' }}>
          <Eye size={18} color="#8b5cf6" />
          <div style={{ fontSize: '0.85rem' }}>
            <span style={{ fontWeight: 600 }}>Citizen Signal:</span> 2 corroborating smoke reports in hexagonal cell
          </div>
        </div>
      </div>

      {aiExplanation && (
        <div style={{
          marginTop: '1.25rem',
          padding: '1rem',
          border: '1px solid rgba(6, 182, 212, 0.3)',
          borderRadius: '8px',
          background: 'rgba(6, 182, 212, 0.05)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#38bdf8', fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.5rem' }}>
            <Sparkles size={16} />
            <span>Google Gemini Incident Reasoning</span>
          </div>
          <p style={{ fontSize: '0.85rem', color: '#e2e8f0', lineHeight: 1.5 }}>
            {aiExplanation.summary}
          </p>
          <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', color: '#94a3b8' }}>
            <strong>Recommended Action:</strong> {aiExplanation.recommendedAction}
          </div>
        </div>
      )}
    </Card>
  );
};
