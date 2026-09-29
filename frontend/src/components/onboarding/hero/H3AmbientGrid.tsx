import React from 'react';

/**
 * H3AmbientGrid: Renders a vector spatial discretization grid over the Mumbai region.
 * Purely visual representation of Uber H3 hexagonal cells with slow, subtle illumination.
 */
export const H3AmbientGrid: React.FC = () => {
  return (
    <svg
      className="hero-h3-grid-svg"
      viewBox="0 0 600 520"
      preserveAspectRatio="xMidYMid slice"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        {/* Spatial Hexagon Pattern */}
        <pattern id="h3HexMesh" width="46" height="79.674" patternUnits="userSpaceOnUse">
          <path
            d="M 46 0 L 23 13.279 L 0 0 L 0 26.558 L 23 39.837 L 46 26.558 Z M 0 39.837 L 23 53.116 L 0 66.395 L 0 92.953 L 23 106.232 L 46 92.953 L 46 66.395 L 23 53.116 Z"
            fill="none"
            stroke="rgba(56, 189, 248, 0.18)"
            strokeWidth="0.85"
          />
        </pattern>

        {/* Glow Filters */}
        <filter id="hexGlowCyan" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="hexGlowGreen" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="hexGlowAmber" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      {/* Hex Grid Background Area across the urban corridor */}
      <rect x="180" y="40" width="420" height="480" fill="url(#h3HexMesh)" opacity="0.45" />

      {/* High-Resolution Individual H3 Hexagonal Cell Overlays */}
      {/* South Mumbai Coastal — Emerald (Good air quality zone) */}
      <polygon
        points="320,270 338,280 338,302 320,312 302,302 302,280"
        fill="rgba(16, 185, 129, 0.22)"
        stroke="#10b981"
        strokeWidth="1.2"
        filter="url(#hexGlowGreen)"
        className="h3-pulse-cell slow-1"
      />
      <polygon
        points="356,290 374,300 374,322 356,332 338,322 338,300"
        fill="rgba(16, 185, 129, 0.18)"
        stroke="#10b981"
        strokeWidth="1.1"
      />

      {/* Central Island Corridor — Cyan / Blue */}
      <polygon
        points="284,250 302,260 302,282 284,292 266,282 266,260"
        fill="rgba(6, 182, 212, 0.2)"
        stroke="#06b6d4"
        strokeWidth="1.2"
        filter="url(#hexGlowCyan)"
        className="h3-pulse-cell slow-2"
      />
      <polygon
        points="428,210 446,220 446,242 428,252 410,242 410,220"
        fill="rgba(56, 189, 248, 0.2)"
        stroke="#38bdf8"
        strokeWidth="1.2"
      />

      {/* Thane / Eastern Bay — Cyan / Teal */}
      <polygon
        points="464,170 482,180 482,202 464,212 446,202 446,180"
        fill="rgba(20, 184, 166, 0.24)"
        stroke="#14b8a6"
        strokeWidth="1.2"
        className="h3-pulse-cell slow-3"
      />
      <polygon
        points="428,150 446,160 446,182 428,192 410,182 410,160"
        fill="rgba(6, 182, 212, 0.18)"
        stroke="#06b6d4"
        strokeWidth="1"
      />

      {/* Navi Mumbai Industrial Corridor — Amber / Stagnation */}
      <polygon
        points="392,270 410,280 410,302 392,312 374,302 374,280"
        fill="rgba(245, 158, 11, 0.25)"
        stroke="#f59e0b"
        strokeWidth="1.3"
        filter="url(#hexGlowAmber)"
        className="h3-pulse-cell slow-1"
      />
      <polygon
        points="428,270 446,280 446,302 428,312 410,302 410,280"
        fill="rgba(245, 158, 11, 0.2)"
        stroke="#fbbf24"
        strokeWidth="1.1"
      />

      {/* Hotspot Spatial Cell (Adjacent to central marker) */}
      <polygon
        points="392,230 410,240 410,262 392,272 374,262 374,240"
        fill="rgba(239, 68, 68, 0.3)"
        stroke="#ef4444"
        strokeWidth="1.5"
        className="h3-pulse-cell hot-cell"
      />
    </svg>
  );
};
