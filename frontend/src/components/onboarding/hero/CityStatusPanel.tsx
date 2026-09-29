import React from 'react';
import { MapPin } from 'lucide-react';

/**
 * CityStatusPanel: Compact dark telemetry status panel anchored at bottom-right
 * of the hero visual canvas. Represents current operational coverage across 3 cities.
 */
export const CityStatusPanel: React.FC = () => {
  const cities = [
    { name: 'Pune', status: 'Good', badgeClass: 'badge-good', dotColor: '#10b981' },
    { name: 'Mumbai', status: 'Moderate', badgeClass: 'badge-moderate', dotColor: '#f59e0b' },
    { name: 'Delhi', status: 'Poor', badgeClass: 'badge-poor', dotColor: '#ef4444' },
  ];

  return (
    <div className="hero-city-status-panel">
      {/* Header */}
      <div className="city-status-header">
        <span>Active in 3 Cities</span>
        <MapPin size={11} style={{ color: 'var(--brand-primary)', opacity: 0.8 }} />
      </div>

      {/* Mini Geographic Silhouette */}
      <div className="city-status-list">
        {cities.map((city) => (
          <div key={city.name} className="city-status-row">
            <div className="city-status-left">
              <span
                className="city-status-dot"
                style={{ background: city.dotColor, boxShadow: `0 0 6px ${city.dotColor}` }}
              />
              <span className="city-status-name">{city.name}</span>
            </div>
            <span className={`city-status-pill ${city.badgeClass}`}>{city.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
