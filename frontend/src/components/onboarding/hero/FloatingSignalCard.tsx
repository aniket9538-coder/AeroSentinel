import React from 'react';
import { LucideIcon } from 'lucide-react';

export interface FloatingSignalCardProps {
  id: string;
  icon: LucideIcon;
  iconColor: string;
  iconBg: string;
  title: string;
  subtitle: string;
  thumbnailComponent?: React.ReactNode;
  style?: React.CSSProperties;
  floatVariant?: 'float-1' | 'float-2' | 'float-3' | 'float-4';
}

/**
 * FloatingSignalCard: Renders a floating translucent information card with
 * gentle independent micro-floating animation (±4-8px, 4-7s loop).
 */
export const FloatingSignalCard: React.FC<FloatingSignalCardProps> = ({
  icon: Icon,
  iconColor,
  iconBg,
  title,
  subtitle,
  thumbnailComponent,
  style = {},
  floatVariant = 'float-1',
}) => {
  return (
    <div
      className={`hero-signal-card ${floatVariant}`}
      style={style}
    >
      {/* Icon Badge */}
      <div
        className="signal-card-icon-badge"
        style={{ background: iconBg, color: iconColor }}
      >
        <Icon size={14} strokeWidth={2.4} />
      </div>

      {/* Text Info */}
      <div className="signal-card-text">
        <div className="signal-card-title">{title}</div>
        <div className="signal-card-sub">{subtitle}</div>
      </div>

      {/* Mini Visual Thumbnail */}
      {thumbnailComponent && (
        <div className="signal-card-thumb-wrap">
          {thumbnailComponent}
        </div>
      )}
    </div>
  );
};
