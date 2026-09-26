import React from 'react';
import { Inbox, LucideIcon } from 'lucide-react';
import { Button } from './Button';

interface EmptyStateProps {
  title?: string;
  message?: string;
  icon?: LucideIcon;
  actionLabel?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No Telemetry Available',
  message = 'There are currently no active data streams recorded for this filter or region.',
  icon: Icon = Inbox,
  actionLabel,
  onAction,
}) => {
  return (
    <div
      style={{
        padding: '3rem 1.5rem',
        textAlign: 'center',
        border: '1px dashed var(--border-medium)',
        borderRadius: '14px',
        background: 'var(--bg-glass)',
        maxWidth: '480px',
        margin: '2rem auto',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}
    >
      <div
        style={{
          width: '48px',
          height: '48px',
          borderRadius: '12px',
          background: 'var(--brand-surface)',
          border: '1px solid var(--brand-border)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--brand-primary)',
          marginBottom: '1rem',
        }}
      >
        <Icon size={24} />
      </div>
      <h4 style={{ color: 'var(--text-primary)', fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.4rem' }}>
        {title}
      </h4>
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5, marginBottom: onAction ? '1.25rem' : 0 }}>
        {message}
      </p>
      {actionLabel && onAction && (
        <Button variant="outline" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
