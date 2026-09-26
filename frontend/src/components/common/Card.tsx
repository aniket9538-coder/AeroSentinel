import React from 'react';

export interface CardProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  action?: React.ReactNode;
  style?: React.CSSProperties;
  badge?: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  children,
  className = '',
  action,
  style = {},
  badge,
}) => {
  return (
    <div
      style={{
        background: 'var(--bg-card)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        border: '1px solid var(--border-subtle)',
        borderRadius: '14px',
        padding: '1.25rem',
        boxShadow: 'var(--shadow-md)',
        position: 'relative',
        ...style,
      }}
      className={`glass-panel ${className}`}
    >
      {(title || subtitle || action || badge) && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            marginBottom: '1rem',
            gap: '0.75rem',
          }}
        >
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              {typeof title === 'string' ? (
                <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  {title}
                </h3>
              ) : (
                title
              )}
              {badge}
            </div>
            {subtitle && (
              <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                {subtitle}
              </p>
            )}
          </div>
          {action && <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>{action}</div>}
        </div>
      )}
      <div>{children}</div>
    </div>
  );
};
