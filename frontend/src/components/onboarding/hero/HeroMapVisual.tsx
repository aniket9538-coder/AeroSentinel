import React, { useEffect, useState } from 'react';
import { Flame, Cloud, Radio, AlertTriangle, Camera } from 'lucide-react';
import { FloatingDataCard } from './FloatingDataCard';
import { ActiveCitiesCard } from './ActiveCitiesCard';
import { DataConnectionLines } from './DataConnectionLines';
import { HotspotPulse } from './HotspotPulse';

/**
 * HeroMapVisual: Right-side cinematic environmental intelligence visualization.
 * Uses the pristine Mumbai clean satellite map as the visual foundation, with
 * 6 decoupled floating React intelligence cards, expanding hotspot beacon, and
 * animated data-connection particle vectors.
 */
export const HeroMapVisual: React.FC = () => {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    // Staggered entry sequence trigger
    const timer = setTimeout(() => {
      setMounted(true);
    }, 60);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div
      className={`hero-map-visual-container ${mounted ? 'visual-loaded' : 'visual-mounting'}`}
      role="region"
      aria-label="Environmental intelligence map visualization of Mumbai"
    >
      {/* =======================================================================
          LAYER 1: Clean Mumbai Satellite & H3 Environmental Base Map
          ======================================================================= */}
      <div className="hero-clean-map-wrap">
        <img
          src="/assets/mumbai-clean-satellite-map.jpg"
          alt="Satellite telemetry of Mumbai coastal metropolitan area with spatial grid"
          className="hero-clean-map-img"
          loading="eager"
        />
      </div>

      {/* =======================================================================
          LAYER 2: Atmospheric Vignette & Soft Gradient Transition to Left
          ======================================================================= */}
      <div className="hero-clean-map-atmosphere" aria-hidden="true" />

      {/* =======================================================================
          LAYER 5: Animated SVG Telemetry Vectors with Moving Glowing Particles
          ======================================================================= */}
      <DataConnectionLines />

      {/* =======================================================================
          LAYER 7: Central Hotspot Expanding Pulse Beacon (3.0s Dual Ring)
          ======================================================================= */}
      <HotspotPulse
        x="59%"
        y="52%"
        label="Potential Hotspot"
        sublabel="AI detects unusual pollution build-up"
      />

      {/* =======================================================================
          LAYER 6: The 6 Decoupled React Floating Intelligence Cards
          ======================================================================= */}

      {/* Card 1: SATELLITE (Top-Left near satellite) */}
      <FloatingDataCard
        id="card-satellite"
        icon={Flame}
        iconColor="#ef4444"
        iconBg="rgba(239, 68, 68, 0.12)"
        title="Satellite"
        subtitle="Detects fire activity and atmospheric signals"
        className="card-pos-satellite"
        style={{ top: '24px', left: '26%' }}
        floatDelay="0s"
        floatDuration="5.4s"
        thumbnailComponent={
          <div className="intel-thumb-wrap thumb-satellite-bg">
            <Flame size={17} color="#fca5a5" />
          </div>
        }
      />

      {/* Card 2: WEATHER (Top-Right region) */}
      <FloatingDataCard
        id="card-weather"
        icon={Cloud}
        iconColor="#0284c7"
        iconBg="rgba(2, 132, 199, 0.12)"
        title="Weather"
        subtitle="Wind, temperature, humidity, rainfall"
        className="card-pos-weather"
        style={{ top: '22px', right: '3%' }}
        floatDelay="0.5s"
        floatDuration="6.2s"
        thumbnailComponent={
          <div className="intel-thumb-wrap thumb-weather-bg">
            <Cloud size={17} color="#ffffff" />
          </div>
        }
      />

      {/* Card 3: AIR QUALITY (Middle-Left) */}
      <FloatingDataCard
        id="card-air-quality"
        icon={Radio}
        iconColor="#10b981"
        iconBg="rgba(16, 185, 129, 0.12)"
        title="Air Quality"
        subtitle="Real-time PM2.5 from ground sensors"
        className="card-pos-air"
        style={{ top: '35%', left: '2%' }}
        floatDelay="0.8s"
        floatDuration="5.8s"
        thumbnailComponent={
          <div className="intel-thumb-wrap thumb-air-bg">
            <Radio size={17} color="#a7f3d0" />
          </div>
        }
      />

      {/* Card 4: POTENTIAL HOTSPOT (Middle-Right, close to hotspot) */}
      <FloatingDataCard
        id="card-hotspot"
        icon={AlertTriangle}
        iconColor="#ef4444"
        iconBg="rgba(239, 68, 68, 0.14)"
        title="Potential Hotspot"
        subtitle="AI detects unusual pollution build-up"
        className="card-pos-hotspot"
        style={{ top: '44%', right: '2%' }}
        floatDelay="1.2s"
        floatDuration="6.8s"
        thumbnailComponent={
          <div className="intel-thumb-wrap thumb-hotspot-bg">
            <AlertTriangle size={17} color="#fecaca" />
          </div>
        }
      />

      {/* Card 5: CITIZEN REPORTS (Lower-Left / Lower-Middle) */}
      <FloatingDataCard
        id="card-citizen"
        icon={Camera}
        iconColor="#a855f7"
        iconBg="rgba(168, 85, 247, 0.12)"
        title="Citizen Reports"
        subtitle="Local evidence (photos & reports)"
        className="card-pos-citizen"
        style={{ bottom: '9%', left: '16%' }}
        floatDelay="1.5s"
        floatDuration="6.5s"
        thumbnailComponent={
          <div className="intel-thumb-wrap thumb-citizen-bg">
            <Camera size={17} color="#e9d5ff" />
          </div>
        }
      />

      {/* Card 6: ACTIVE CITIES PANEL (Bottom-Right) */}
      <ActiveCitiesCard
        className="card-pos-cities"
        style={{ bottom: '14px', right: '14px' }}
      />
    </div>
  );
};

export default HeroMapVisual;
