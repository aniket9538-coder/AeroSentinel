import React from 'react';
import { Card } from '../common/Card';
import { Sparkles } from 'lucide-react';

interface GeminiExplanationProps {
  summary: string;
  signals?: string[];
  recommendedAction?: string;
}

export const GeminiExplanation: React.FC<GeminiExplanationProps> = ({
  summary,
  signals = [],
  recommendedAction,
}) => {
  return (
    <Card>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#06b6d4', marginBottom: '0.75rem' }}>
        <Sparkles size={18} />
        <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f3f4f6' }}>Gemini Reasoning Engine</h4>
      </div>
      <p style={{ fontSize: '0.875rem', color: '#e5e7eb', lineHeight: 1.5 }}>
        {summary}
      </p>
      {signals.length > 0 && (
        <div style={{ marginTop: '0.75rem' }}>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af', fontWeight: 600 }}>Corroborating Telemetry:</div>
          <ul style={{ paddingLeft: '1.25rem', marginTop: '0.25rem', fontSize: '0.8rem', color: '#d1d5db' }}>
            {signals.map((sig, i) => (
              <li key={i}>{sig}</li>
            ))}
          </ul>
        </div>
      )}
      {recommendedAction && (
        <div style={{ marginTop: '0.75rem', padding: '0.5rem', background: 'rgba(6, 182, 212, 0.08)', borderRadius: '6px', fontSize: '0.8rem', color: '#38bdf8' }}>
          <strong>Field Recommendation:</strong> {recommendedAction}
        </div>
      )}
    </Card>
  );
};
