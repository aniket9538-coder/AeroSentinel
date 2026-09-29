import React from 'react';
import { LucideIcon } from 'lucide-react';

export interface FloatingDataCardProps {
  id: string;
  icon: LucideIcon;
  iconColor: string;
  iconBg: string;
  title: string;
  subtitle: string;
  className?: string;
  style?: React.CSSProperties;
  floatDelay?: string;
  floatDuration?: string;
  thumbnailComponent?: React.ReactNode;
}

/**
 * FloatingDataCard: High-precision translucent floating intelligence card.
 * Enters with smooth fade/scale transition and floats with non-synchronous gentle oscillation.
 */
export const FloatingDataCard: React.FC<FloatingDataCardProps> = ({
  icon: Icon,
  iconColor,
  iconBg,
  title,
  subtitle,
  className = '',
  style = {},
  floatDelay = '0s',
  floatDuration = '5.5s',
  thumbnailComponent,
}) => {
  return (
    <div
      className={`floating-intel-card ${className}`}
      style={{
        ...style,
        animationDelay: floatDelay,
        animationDuration: floatDuration,
      }}
      role="article"
      aria-label={`${title}: ${subtitle}`}
    >
      {/* Icon Badge */}
      <div
        className="intel-card-icon-badge"
        style={{ background: iconBg, color: iconColor }}
        aria-hidden="true"
      >
        <Icon size={14} strokeWidth={2.4} />
      </div>

      {/* Text Info */}
      <div className="intel-card-text">
        <h4 className="intel-card-title">{title}</h4>
        <p className="intel-card-sub">{subtitle}</p>
      </div>

      {/* Mini Visual Thumbnail */}
      {thumbnailComponent && (
        <div className="intel-card-thumb-wrap" aria-hidden="true">
          {thumbnailComponent}
        </div>
      )}
    </div>
  );
};
