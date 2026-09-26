import React from 'react';
import { HOTSPOT_RISK_STYLES } from '../../utils/hotspotColors';
import { HotspotRiskLevel } from '../../types/hotspot';

export const RiskLegend: React.FC = () => {
  const levels: HotspotRiskLevel[] = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'];

  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: '0.75rem',
        padding: '0.65rem 1rem',
        borderRadius: '10px',
        backgroundColor: 'var(--bg-card)',
        border: '1px solid var(--border-subtle)',
        fontSize: '0.8rem',
      }}
    >
      <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>
        Potential Hotspot Risk:
      </span>
      {levels.map((lvl) => {
        const style = HOTSPOT_RISK_STYLES[lvl];
        return (
          <div
            key={lvl}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
            }}
          >
            <span
              style={{
                width: '12px',
                height: '12px',
                borderRadius: '3px',
                backgroundColor: style.color,
                display: 'inline-block',
                boxShadow: `0 0 6px ${style.color}40`,
              }}
            />
            <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
              {style.label}
            </span>
          </div>
        );
      })}
    </div>
  );
};

export default RiskLegend;
