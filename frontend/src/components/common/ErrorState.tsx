import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  retryLabel?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Sensor Telemetry Unreachable',
  message = 'An unexpected timeout occurred while communicating with the atmospheric intelligence service.',
  onRetry,
  retryLabel = 'Retry Request',
}) => {
  return (
    <div
      style={{
        padding: '2.5rem 1.5rem',
        textAlign: 'center',
        border: '1px solid rgba(244, 63, 94, 0.25)',
        borderRadius: '14px',
        background: 'rgba(244, 63, 94, 0.05)',
        maxWidth: '460px',
        margin: '2rem auto',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}
    >
      <div
        style={{
          width: '44px',
          height: '44px',
          borderRadius: '12px',
          background: 'rgba(244, 63, 94, 0.12)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--accent-rose)',
          marginBottom: '1rem',
        }}
      >
        <AlertCircle size={22} />
      </div>
      <h4 style={{ color: 'var(--text-primary)', fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.4rem' }}>
        {title}
      </h4>
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', lineHeight: 1.5, marginBottom: '1.25rem' }}>
        {message}
      </p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          <RefreshCw size={14} style={{ marginRight: '0.35rem' }} /> {retryLabel}
        </Button>
      )}
    </div>
  );
};
