import React from 'react';

interface PageContainerProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

export const PageContainer: React.FC<PageContainerProps> = ({
  title,
  subtitle,
  action,
  actions,
  children,
  className = '',
  style = {},
}) => {
  const headerAction = action || actions;
  return (
    <div
      style={{
        width: '100%',
        maxWidth: '1600px',
        margin: '0 auto',
        padding: '1.75rem 2rem',
        minHeight: 'calc(100vh - 64px)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
        ...style,
      }}
      className={`page-container ${className}`}
    >
      {(title || subtitle || headerAction) && (
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '1rem',
            paddingBottom: '0.75rem',
            borderBottom: '1px solid var(--border-subtle)',
          }}
        >
          <div>
            {typeof title === 'string' ? (
              <h1
                style={{
                  fontSize: '1.65rem',
                  fontWeight: 800,
                  fontFamily: 'var(--font-heading)',
                  color: 'var(--text-primary)',
                  letterSpacing: '-0.025em',
                  lineHeight: 1.2,
                }}
              >
                {title}
              </h1>
            ) : (
              title
            )}
            {subtitle && (
              <div
                style={{
                  fontSize: '0.875rem',
                  color: 'var(--text-secondary)',
                  marginTop: '0.35rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                {subtitle}
              </div>
            )}
          </div>
          {headerAction && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              {headerAction}
            </div>
          )}
        </div>
      )}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {children}
      </div>
    </div>
  );
};
