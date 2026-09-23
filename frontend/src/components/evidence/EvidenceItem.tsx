import React from 'react';
import { EventEvidence } from '../../types';

interface EvidenceItemProps {
  evidence: EventEvidence;
}

export const EvidenceItem: React.FC<EvidenceItemProps> = ({ evidence }) => {
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '0.6rem 0.75rem',
      backgroundColor: 'rgba(255, 255, 255, 0.03)',
      borderRadius: '6px',
      border: '1px solid rgba(255, 255, 255, 0.06)',
    }}>
      <div>
        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f3f4f6' }}>{evidence.evidenceKey}</div>
        <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>{evidence.evidenceValue}</div>
      </div>
      <div style={{ fontSize: '0.75rem', color: '#38bdf8', fontWeight: 600 }}>
        Weight: {evidence.weight.toFixed(1)}
      </div>
    </div>
  );
};
