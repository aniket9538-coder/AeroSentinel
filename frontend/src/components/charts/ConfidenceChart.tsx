import React from 'react';

interface ConfidenceChartProps {
  confidence?: number;
  label?: string;
}

export const ConfidenceChart: React.FC<ConfidenceChartProps> = ({ confidence = 0.88, label = 'Model Certainty' }) => {
  const percentage = Math.round(confidence * 100);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '260px',
      padding: '1rem',
    }}>
      <div style={{
        position: 'relative',
        width: '130px',
        height: '130px',
        borderRadius: '50%',
        background: `conic-gradient(#06b6d4 ${percentage * 3.6}deg, rgba(255, 255, 255, 0.05) 0deg)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        <div style={{
          width: '106px',
          height: '106px',
          borderRadius: '50%',
          background: '#111827',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <span style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f3f4f6' }}>{percentage}%</span>
          <span style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Confidence</span>
        </div>
      </div>
      <p style={{ marginTop: '1.25rem', fontSize: '0.875rem', color: '#9ca3af' }}>{label}</p>
    </div>
  );
};
