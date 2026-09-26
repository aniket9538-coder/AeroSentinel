import React from 'react';

export const Loading: React.FC<{ message?: string; subMessage?: string }> = ({
  message = 'Retrieving environmental telemetry...',
  subMessage = 'Querying ground stations, weather sensors and satellite feeds',
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '4rem 1.5rem',
        textAlign: 'center',
      }}
    >
      <div style={{ position: 'relative', width: '48px', height: '48px', marginBottom: '1.25rem' }}>
        <div
          style={{
            position: 'absolute',
            inset: 0,
            border: '3px solid var(--border-subtle)',
            borderRadius: '50%',
          }}
        />
        <div
          style={{
            position: 'absolute',
            inset: 0,
            border: '3px solid transparent',
            borderTopColor: 'var(--brand-primary)',
            borderRadius: '50%',
            animation: 'spin 0.9s cubic-bezier(0.55, 0.25, 0.25, 0.9) infinite',
          }}
        />
      </div>
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <h4 style={{ color: 'var(--text-primary)', fontSize: '0.95rem', fontWeight: 600 }}>{message}</h4>
      {subMessage && (
        <p style={{ marginTop: '0.35rem', color: 'var(--text-muted)', fontSize: '0.8rem', maxWidth: '380px' }}>
          {subMessage}
        </p>
      )}
    </div>
  );
};
