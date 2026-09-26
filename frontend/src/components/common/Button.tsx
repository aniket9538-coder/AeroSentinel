import React from 'react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  className = '',
  disabled,
  style = {},
  ...props
}) => {
  const sizeStyles = {
    sm: { padding: '0.35rem 0.75rem', fontSize: '0.775rem', borderRadius: '6px' },
    md: { padding: '0.5rem 1rem', fontSize: '0.875rem', borderRadius: '8px' },
    lg: { padding: '0.7rem 1.4rem', fontSize: '1rem', borderRadius: '10px' },
  }[size];

  const getVariantStyles = () => {
    switch (variant) {
      case 'primary':
        return {
          background: 'linear-gradient(135deg, var(--brand-primary), var(--accent-blue))',
          color: '#ffffff',
          border: 'none',
          boxShadow: '0 2px 10px rgba(56, 189, 248, 0.25)',
        };
      case 'secondary':
        return {
          background: 'var(--bg-surface-elevated)',
          color: 'var(--text-primary)',
          border: '1px solid var(--border-medium)',
        };
      case 'outline':
        return {
          background: 'var(--brand-surface)',
          color: 'var(--brand-primary)',
          border: '1px solid var(--brand-border)',
        };
      case 'danger':
        return {
          background: 'linear-gradient(135deg, var(--accent-rose), #be123c)',
          color: '#ffffff',
          border: 'none',
          boxShadow: '0 2px 8px rgba(244, 63, 94, 0.3)',
        };
      case 'ghost':
        return {
          background: 'transparent',
          color: 'var(--text-secondary)',
          border: 'none',
        };
      default:
        return {};
    }
  };

  return (
    <button
      disabled={disabled || isLoading}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.5rem',
        fontWeight: 600,
        fontFamily: 'inherit',
        cursor: disabled || isLoading ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.5 : 1,
        transition: 'all 0.15s ease',
        ...sizeStyles,
        ...getVariantStyles(),
        ...style,
      }}
      className={`aero-button ${className}`}
      {...props}
    >
      {isLoading && (
        <span
          style={{
            width: '14px',
            height: '14px',
            border: '2px solid rgba(255, 255, 255, 0.3)',
            borderTopColor: '#ffffff',
            borderRadius: '50%',
            display: 'inline-block',
            animation: 'spin 0.8s linear infinite',
          }}
        />
      )}
      {children}
    </button>
  );
};
