import React from 'react';
import { calculateQuorumProgress } from '../../utils/federatedUtils';
import { CheckCircle2, AlertCircle } from 'lucide-react';

interface RoundQuorumProgressProps {
  receivedCount: number;
  minQuorum: number;
}

export const RoundQuorumProgress: React.FC<RoundQuorumProgressProps> = ({
  receivedCount,
  minQuorum,
}) => {
  const quorum = calculateQuorumProgress(receivedCount, minQuorum);

  const barColor = quorum.isQuorumMet ? '#10b981' : '#f59e0b';
  const barBg = quorum.isQuorumMet ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)';

  return (
    <div style={{ marginTop: '0.75rem' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '0.8rem',
          marginBottom: '0.4rem',
        }}
      >
        <span style={{ color: '#9ca3af', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
          {quorum.isQuorumMet ? (
            <CheckCircle2 size={14} color="#10b981" />
          ) : (
            <AlertCircle size={14} color="#f59e0b" />
          )}
          <span>Consensus Quorum:</span>
          <strong style={{ color: quorum.isQuorumMet ? '#10b981' : '#f59e0b' }}>
            {quorum.isQuorumMet ? 'Satisfied' : 'Pending Updates'}
          </strong>
        </span>
        <span style={{ color: '#e5e7eb', fontWeight: 600 }}>{quorum.label}</span>
      </div>

      <div
        style={{
          width: '100%',
          height: '8px',
          background: barBg,
          borderRadius: '9999px',
          overflow: 'hidden',
          border: `1px solid ${barColor}33`,
        }}
      >
        <div
          style={{
            width: `${quorum.percentage}%`,
            height: '100%',
            background: barColor,
            borderRadius: '9999px',
            transition: 'width 0.4s ease-in-out',
            boxShadow: quorum.isQuorumMet ? '0 0 8px rgba(16, 185, 129, 0.5)' : undefined,
          }}
        />
      </div>
    </div>
  );
};

export default RoundQuorumProgress;
