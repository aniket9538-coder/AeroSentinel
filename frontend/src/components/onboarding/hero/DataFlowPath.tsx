import React from 'react';

/**
 * DataFlowPath: Renders calibrated bezier flow vectors connecting multi-source
 * environmental telemetry (Satellite, Weather, Air Quality, Citizen) into the
 * spatial H3 grid and intelligence layer.
 */
export const DataFlowPath: React.FC = () => {
  return (
    <svg
      className="hero-data-flow-svg"
      viewBox="0 0 600 520"
      preserveAspectRatio="xMidYMid slice"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        {/* Stream Gradients */}
        <linearGradient id="flowSatelliteGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.9" />
          <stop offset="60%" stopColor="#0284c7" stopOpacity="0.6" />
          <stop offset="100%" stopColor="#ef4444" stopOpacity="0.8" />
        </linearGradient>

        <linearGradient id="flowWeatherGrad" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.8" />
          <stop offset="100%" stopColor="#06b6d4" stopOpacity="0.4" />
        </linearGradient>

        <linearGradient id="flowAirGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#10b981" stopOpacity="0.9" />
          <stop offset="100%" stopColor="#38bdf8" stopOpacity="0.5" />
        </linearGradient>

        <linearGradient id="flowCitizenGrad" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#a855f7" stopOpacity="0.85" />
          <stop offset="100%" stopColor="#38bdf8" stopOpacity="0.4" />
        </linearGradient>
      </defs>

      {/* Path 1: Satellite (top-left ~120,60) -> H3 cells -> Central Hotspot (~348,245) */}
      <path
        d="M 130,70 C 180,120 250,180 348,245"
        fill="none"
        stroke="url(#flowSatelliteGrad)"
        strokeWidth="1.8"
        strokeDasharray="6 8"
        className="flow-vector flow-satellite"
      />

      {/* Path 2: Weather (top-right ~480,80) -> H3 Eastern corridor (~410,210) */}
      <path
        d="M 480,90 C 450,140 430,170 355,240"
        fill="none"
        stroke="url(#flowWeatherGrad)"
        strokeWidth="1.6"
        strokeDasharray="5 7"
        className="flow-vector flow-weather"
      />

      {/* Path 3: Air Quality Ground Sensor (mid-left ~170,220) -> Central H3 cells (~310,260) */}
      <path
        d="M 170,220 C 230,220 280,240 345,248"
        fill="none"
        stroke="url(#flowAirGrad)"
        strokeWidth="1.6"
        strokeDasharray="5 7"
        className="flow-vector flow-air"
      />

      {/* Path 4: Citizen Reports (bottom-left ~240,410) -> Evidence cluster (~330,310) */}
      <path
        d="M 240,410 C 270,360 300,320 345,255"
        fill="none"
        stroke="url(#flowCitizenGrad)"
        strokeWidth="1.6"
        strokeDasharray="5 7"
        className="flow-vector flow-citizen"
      />

      {/* Location / Sensor Anchor Nodes */}
      {/* Thane Station Anchor */}
      <g transform="translate(460, 150)">
        <circle r="3.5" fill="#38bdf8" />
        <circle r="7" fill="none" stroke="#38bdf8" strokeWidth="0.8" opacity="0.6" />
        <text x="12" y="4" fill="#f1f5f9" fontSize="10.5" fontWeight="700" letterSpacing="0.04em">
          Thane
        </text>
      </g>

      {/* Mumbai Central Anchor */}
      <g transform="translate(340, 315)">
        <circle r="4" fill="#10b981" />
        <circle r="8" fill="none" stroke="#10b981" strokeWidth="0.8" opacity="0.6" />
        <text x="-48" y="4" fill="#f1f5f9" fontSize="11" fontWeight="700" letterSpacing="0.04em">
          Mumbai
        </text>
      </g>

      {/* Navi Mumbai Anchor */}
      <g transform="translate(450, 315)">
        <circle r="3.5" fill="#06b6d4" />
        <text x="12" y="4" fill="#f1f5f9" fontSize="10.5" fontWeight="700" letterSpacing="0.04em">
          Navi Mumbai
        </text>
      </g>
    </svg>
  );
};
