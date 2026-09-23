import React from 'react';
import { Compass } from 'lucide-react';

interface WindChartProps {
  speed?: number;
  direction?: number;
}

export const WindChart: React.FC<WindChartProps> = ({ speed = 3.4, direction = 210 }) => {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '1.5rem',
      height: '260px',
    }}>
      <div style={{
        position: 'relative',
        width: '120px',
        height: '120px',
        borderRadius: '50%',
        border: '2px dashed rgba(255, 255, 255, 0.15)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '1rem',
      }}>
        <div style={{
          transform: `rotate(${direction}deg)`,
          transition: 'transform 0.5s ease-out',
        }}>
          <Compass size={64} color="#06b6d4" />
        </div>
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#f3f4f6' }}>{speed} m/s</div>
        <div style={{ fontSize: '0.85rem', color: '#9ca3af' }}>Bearing: {direction}° (SW)</div>
      </div>
    </div>
  );
};
