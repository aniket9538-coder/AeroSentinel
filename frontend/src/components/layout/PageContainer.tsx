import React from 'react';

interface PageContainerProps {
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}

export const PageContainer: React.FC<PageContainerProps> = ({
  title,
  subtitle,
  action,
  children,
}) => {
  return (
    <div style={{ padding: '2rem', maxWidth: '1440px', margin: '0 auto', width: '100%' }}>
      {(title || action) && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
          <div>
            {title && <h1 style={{ fontSize: '1.875rem', fontWeight: 700, color: '#f9fafb' }}>{title}</h1>}
            {subtitle && <p style={{ fontSize: '0.95rem', color: '#9ca3af', marginTop: '0.25rem' }}>{subtitle}</p>}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      <div>{children}</div>
    </div>
  );
};
