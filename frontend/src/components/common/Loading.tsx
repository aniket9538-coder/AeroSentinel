import React from 'react';

export const Loading: React.FC<{ message?: string }> = ({ message = 'Loading environmental intelligence...' }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 1rem' }}>
      <div style={{
        width: '40px',
        height: '40px',
        border: '3px solid rgba(6, 182, 212, 0.2)',
        borderTopColor: '#06b6d4',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
      }} />
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <p style={{ marginTop: '1rem', color: '#9ca3af', fontSize: '0.875rem' }}>{message}</p>
    </div>
  );
};
