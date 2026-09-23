import React from 'react';
import { Button } from './Button';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Failed to load telemetry',
  message = 'An unexpected error occurred while communicating with the backend.',
  onRetry,
}) => {
  return (
    <div style={{
      padding: '2rem',
      textAlign: 'center',
      border: '1px solid rgba(244, 63, 94, 0.2)',
      borderRadius: '12px',
      background: 'rgba(244, 63, 94, 0.05)',
      maxWidth: '450px',
      margin: '2rem auto',
    }}>
      <h4 style={{ color: '#fb7185', fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.5rem' }}>{title}</h4>
      <p style={{ color: '#9ca3af', fontSize: '0.875rem', marginBottom: '1.25rem' }}>{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Retry Connection
        </Button>
      )}
    </div>
  );
};
