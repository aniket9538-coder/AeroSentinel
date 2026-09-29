import React from 'react';
import { AlertTriangle } from 'lucide-react';

interface HotspotPulseProps {
  x?: string | number;
  y?: string | number;
  label?: string;
  sublabel?: string;
}

/**
 * HotspotPulse: Renders a central Potential Hotspot marker with dual expanding
 * transparent rings pulsing on a 3-second gentle loop.
 */
export const HotspotPulse: React.FC<HotspotPulseProps> = ({
  x = '58%',
  y = '47%',
  label = 'Potential Hotspot',
  sublabel = 'AI detects unusual pollution build-up',
}) => {
  return (
    <div
      className="hero-hotspot-container"
      style={{
        left: typeof x === 'number' ? `${x}px` : x,
        top: typeof y === 'number' ? `${y}px` : y,
      }}
    >
      {/* Primary Expanding Pulse Ring 1 */}
      <div className="hotspot-pulse-ring ring-1" />

      {/* Secondary Expanding Pulse Ring 2 (Delayed) */}
      <div className="hotspot-pulse-ring ring-2" />

      {/* Central Glowing Core Marker */}
      <div className="hotspot-pulse-core">
        <AlertTriangle size={14} strokeWidth={2.6} className="hotspot-core-icon" />
      </div>

      {/* Interactive Micro Card Over Core */}
      <div className="hotspot-beacon-card">
        <div className="hotspot-card-icon-badge">
          <AlertTriangle size={13} color="#ef4444" strokeWidth={2.4} />
        </div>
        <div className="hotspot-card-content">
          <div className="hotspot-card-title">{label}</div>
          <div className="hotspot-card-sub">{sublabel}</div>
        </div>
      </div>
    </div>
  );
};
