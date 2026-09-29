import React from 'react';

/**
 * DataConnectionLines: Renders animated SVG telemetry connection vectors with
 * moving glowing particles along bezier pathways.
 * Communicates: MULTI-SOURCE DATA → SPATIAL FUSION → INTELLIGENCE
 */
export const DataConnectionLines: React.FC = () => {
  return (
    <svg
      className="hero-connection-lines-svg"
      viewBox="0 0 640 520"
      preserveAspectRatio="xMidYMid slice"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        {/* Glow Filters */}
        <filter id="vectorGlowCyan" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="vectorGlowRed" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>

        {/* Vector Linear Gradients */}
        <linearGradient id="gradSatelliteToGrid" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.85" />
          <stop offset="60%" stopColor="#0284c7" stopOpacity="0.5" />
          <stop offset="100%" stopColor="#ef4444" stopOpacity="0.75" />
        </linearGradient>

        <linearGradient id="gradWeatherToGrid" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.8" />
          <stop offset="100%" stopColor="#06b6d4" stopOpacity="0.45" />
        </linearGradient>

        <linearGradient id="gradAirToGrid" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#10b981" stopOpacity="0.85" />
          <stop offset="100%" stopColor="#06b6d4" stopOpacity="0.5" />
        </linearGradient>

        <linearGradient id="gradCitizenToGrid" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#a855f7" stopOpacity="0.85" />
          <stop offset="100%" stopColor="#38bdf8" stopOpacity="0.45" />
        </linearGradient>
      </defs>

      {/* 1. Satellite (Top-Left ~140,80) -> H3 Central Spatial Core (~375,270) */}
      <path
        id="pathSatellite"
        d="M 140,80 C 190,130 270,195 375,270"
        fill="none"
        stroke="url(#gradSatelliteToGrid)"
        strokeWidth="1.6"
        strokeDasharray="4 6"
        className="connection-vector vector-satellite"
      />
      {/* Moving Glowing Particle along Satellite path */}
      <circle r="3" fill="#38bdf8" filter="url(#vectorGlowCyan)">
        <animateMotion dur="4.2s" repeatCount="indefinite" path="M 140,80 C 190,130 270,195 375,270" />
      </circle>

      {/* 2. Weather (Top-Right ~510,95) -> H3 Grid / Hotspot (~375,270) */}
      <path
        id="pathWeather"
        d="M 510,95 C 470,150 435,190 375,270"
        fill="none"
        stroke="url(#gradWeatherToGrid)"
        strokeWidth="1.5"
        strokeDasharray="4 6"
        className="connection-vector vector-weather"
      />
      {/* Moving Particle along Weather path */}
      <circle r="2.8" fill="#06b6d4" filter="url(#vectorGlowCyan)">
        <animateMotion dur="5.0s" repeatCount="indefinite" path="M 510,95 C 470,150 435,190 375,270" />
      </circle>

      {/* 3. Air Quality Ground Sensor (Mid-Left ~180,240) -> Central H3 Hex (~375,270) */}
      <path
        id="pathAirQuality"
        d="M 180,240 C 240,240 300,255 375,270"
        fill="none"
        stroke="url(#gradAirToGrid)"
        strokeWidth="1.5"
        strokeDasharray="4 6"
        className="connection-vector vector-air"
      />
      {/* Moving Particle along Air Quality path */}
      <circle r="3" fill="#10b981" filter="url(#vectorGlowCyan)">
        <animateMotion dur="4.6s" repeatCount="indefinite" path="M 180,240 C 240,240 300,255 375,270" />
      </circle>

      {/* 4. Citizen Reports (Bottom-Left ~260,430) -> H3 Evidence Cluster (~375,270) */}
      <path
        id="pathCitizen"
        d="M 260,430 C 290,370 330,320 375,270"
        fill="none"
        stroke="url(#gradCitizenToGrid)"
        strokeWidth="1.5"
        strokeDasharray="4 6"
        className="connection-vector vector-citizen"
      />
      {/* Moving Particle along Citizen path */}
      <circle r="2.8" fill="#a855f7" filter="url(#vectorGlowCyan)">
        <animateMotion dur="5.4s" repeatCount="indefinite" path="M 260,430 C 290,370 330,320 375,270" />
      </circle>

      {/* 5. Core Fusion Node at Hotspot Coordinates (375, 270) */}
      <circle cx="375" cy="270" r="4.5" fill="#ef4444" filter="url(#vectorGlowRed)">
        <animate attributeName="r" values="3.5;5.5;3.5" dur="2.4s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0.7;1;0.7" dur="2.4s" repeatCount="indefinite" />
      </circle>
    </svg>
  );
};
