import React from 'react';
import { MapPin } from 'lucide-react';

interface ActiveCitiesCardProps {
  className?: string;
  style?: React.CSSProperties;
}

/**
 * ActiveCitiesCard: Floating dark telemetry status panel anchored at bottom-right
 * of the hero map visual. Highlights current municipal intelligence deployment.
 */
export const ActiveCitiesCard: React.FC<ActiveCitiesCardProps> = ({
  className = '',
  style = {},
}) => {
  const cities = [
    { name: 'Pune', status: 'Good', badgeClass: 'badge-good', dotColor: '#10b981' },
    { name: 'Mumbai', status: 'Moderate', badgeClass: 'badge-moderate', dotColor: '#f59e0b' },
    { name: 'Delhi', status: 'Poor', badgeClass: 'badge-poor', dotColor: '#ef4444' },
  ];

  return (
    <div
      className={`floating-active-cities-card ${className}`}
      style={style}
      role="region"
      aria-label="Active cities monitoring coverage"
    >
      <div className="active-cities-header">
        <span className="active-cities-title">Active in 3 Cities</span>
        <MapPin size={12} className="active-cities-icon" />
      </div>

      <div className="active-cities-list">
        {cities.map((city) => (
          <div key={city.name} className="active-city-row">
            <div className="active-city-name-wrap">
              <span
                className="active-city-dot"
                style={{ background: city.dotColor, boxShadow: `0 0 7px ${city.dotColor}` }}
              />
              <span className="active-city-name">{city.name}</span>
            </div>
            <span className={`active-city-pill ${city.badgeClass}`}>{city.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
