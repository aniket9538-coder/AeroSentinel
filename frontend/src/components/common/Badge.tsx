import React from 'react';

export type BadgeVariant =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'neutral'
  | 'good'
  | 'moderate'
  | 'poor'
  | 'very-poor'
  | 'severe';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
  pulse?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'md',
  pulse = false,
}) => {
  const getColors = (): { bg: string; color: string; border: string; dot?: string } => {
    switch (variant) {
      case 'success':
      case 'good':
        return {
          bg: 'rgba(16, 185, 129, 0.12)',
          color: 'var(--aqi-good)',
          border: 'rgba(16, 185, 129, 0.28)',
          dot: 'var(--aqi-good)',
        };
      case 'warning':
      case 'moderate':
        return {
          bg: 'rgba(245, 158, 11, 0.12)',
          color: 'var(--aqi-moderate)',
          border: 'rgba(245, 158, 11, 0.28)',
          dot: 'var(--aqi-moderate)',
        };
      case 'poor':
        return {
          bg: 'rgba(249, 115, 22, 0.12)',
          color: 'var(--aqi-poor)',
          border: 'rgba(249, 115, 22, 0.28)',
          dot: 'var(--aqi-poor)',
        };
      case 'danger':
      case 'very-poor':
        return {
          bg: 'rgba(239, 68, 68, 0.12)',
          color: 'var(--aqi-very-poor)',
          border: 'rgba(239, 68, 68, 0.28)',
          dot: 'var(--aqi-very-poor)',
        };
      case 'severe':
        return {
          bg: 'rgba(136, 19, 55, 0.18)',
          color: 'var(--aqi-severe)',
          border: 'rgba(136, 19, 55, 0.35)',
          dot: 'var(--aqi-severe)',
        };
      case 'info':
        return {
          bg: 'var(--brand-surface)',
          color: 'var(--brand-primary)',
          border: 'var(--brand-border)',
          dot: 'var(--brand-primary)',
        };
      case 'neutral':
      default:
        return {
          bg: 'rgba(148, 163, 184, 0.12)',
          color: 'var(--text-secondary)',
          border: 'var(--border-subtle)',
          dot: 'var(--text-muted)',
        };
    }
  };

  const styleConfig = getColors();

  const sizeStyles = {
    sm: { padding: '0.15rem 0.45rem', fontSize: '0.675rem' },
    md: { padding: '0.2rem 0.65rem', fontSize: '0.75rem' },
  }[size];

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.35rem',
        borderRadius: '9999px',
        fontWeight: 600,
        backgroundColor: styleConfig.bg,
        color: styleConfig.color,
        border: `1px solid ${styleConfig.border}`,
        letterSpacing: '0.02em',
        lineHeight: 1.2,
        ...sizeStyles,
      }}
    >
      {pulse && (
        <span
          style={{
            width: '6px',
            height: '6px',
            borderRadius: '50%',
            backgroundColor: styleConfig.dot,
            display: 'inline-block',
          }}
          className="live-indicator-dot"
        />
      )}
      {children}
    </span>
  );
};
