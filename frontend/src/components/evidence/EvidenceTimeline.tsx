import React from 'react';
import { EvidenceItem } from './EvidenceItem';
import { EventEvidence } from '../../types';

interface EvidenceTimelineProps {
  evidenceList?: EventEvidence[];
}

export const EvidenceTimeline: React.FC<EvidenceTimelineProps> = ({ evidenceList = [] }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
      {evidenceList.map((e) => (
        <EvidenceItem key={e.id} evidence={e} />
      ))}
    </div>
  );
};
