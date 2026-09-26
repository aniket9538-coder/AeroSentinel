import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import {
  Sun,
  Moon,
  Bell,
  ChevronDown,
  Menu,
  X,
  MapPin,
  ShieldCheck,
  Radio,
} from 'lucide-react';
import { useApp } from '../../store/AppContext';

export const Navbar: React.FC = () => {
  const location = useLocation();
  const {
    theme,
    toggleTheme,
    selectedCity,
    setSelectedCity,
    availableCities,
    isLive,
    isOnline,
    toggleSidebar,
    sidebarOpen,
    backendStatus,
    backendHealth,
  } = useApp();

  const [cityDropdownOpen, setCityDropdownOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  // Derive page context title from route
  const getPageTitle = (path: string): string => {
    if (path === '/' || path === '/dashboard') return 'Overview';
    if (path === '/air-quality') return 'Air Quality';
    if (path === '/weather') return 'Weather & Spatial';
    if (path === '/map' || path === '/spatial') return 'Spatial Grid';
    if (path.includes('hotspot')) return 'Hotspot Intelligence';
    if (path.includes('forecast')) return 'Predictive Forecast';
    if (path.includes('evidence')) return 'Gemini Evidence';
    if (path.includes('alerts')) return 'Active Alerts';
    if (path.includes('authority') || path.includes('incident')) return 'Authority Command';
    if (path.includes('federated')) return 'Federated Network';
    if (path.includes('admin') || path.includes('status')) return 'System Diagnostics';
    if (path.includes('citizen')) return 'Citizen Telemetry';
    return 'Command Center';
  };

  return (
    <header
      style={{
        height: '64px',
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border-subtle)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
        padding: '0 1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      {/* Left: Mobile Toggle & Page Context */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button
          onClick={toggleSidebar}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--text-secondary)',
            cursor: 'pointer',
            padding: '0.4rem',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
        >
          {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
        </button>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.6rem' }}>
          <span
            style={{
              fontSize: '1.05rem',
              fontWeight: 700,
              fontFamily: 'var(--font-heading)',
              color: 'var(--text-primary)',
            }}
          >
            {getPageTitle(location.pathname)}
          </span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>
            v2.4.0
          </span>
        </div>
      </div>

      {/* Center-Left: City Selector */}
      <div style={{ position: 'relative' }}>
        <button
          onClick={() => setCityDropdownOpen(!cityDropdownOpen)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.4rem 0.85rem',
            background: 'var(--bg-surface-elevated)',
            border: '1px solid var(--border-medium)',
            borderRadius: '10px',
            color: 'var(--text-primary)',
            fontSize: '0.875rem',
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
        >
          <MapPin size={15} color="var(--brand-primary)" />
          <span>{selectedCity?.name || 'Select City'}</span>
          {selectedCity && (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              ({selectedCity.state})
            </span>
          )}
          <ChevronDown
            size={14}
            style={{
              transform: cityDropdownOpen ? 'rotate(180deg)' : 'none',
              transition: 'transform 0.15s ease',
              color: 'var(--text-secondary)',
            }}
          />
        </button>

        {cityDropdownOpen && (
          <div
            style={{
              position: 'absolute',
              top: 'calc(100% + 6px)',
              left: 0,
              width: '210px',
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-medium)',
              borderRadius: '12px',
              boxShadow: 'var(--shadow-lg)',
              overflow: 'hidden',
              zIndex: 200,
              padding: '0.4rem',
            }}
          >
            <div
              style={{
                fontSize: '0.7rem',
                fontWeight: 700,
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
                padding: '0.4rem 0.6rem',
                letterSpacing: '0.05em',
              }}
            >
              Select Operating City
            </div>
            {availableCities.map((c) => (
              <button
                key={c.id}
                onClick={() => {
                  setSelectedCity(c);
                  setCityDropdownOpen(false);
                }}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.5rem 0.65rem',
                  background:
                    selectedCity?.id === c.id ? 'var(--brand-surface)' : 'transparent',
                  border: 'none',
                  borderRadius: '8px',
                  color:
                    selectedCity?.id === c.id
                      ? 'var(--brand-primary)'
                      : 'var(--text-primary)',
                  fontSize: '0.85rem',
                  fontWeight: selectedCity?.id === c.id ? 600 : 500,
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <span>{c.name}</span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                  {c.state}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Right Controls: Live Status, Theme, Notifications, User */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
        {/* F0 Backend Connectivity Indicator */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.45rem',
            padding: '0.35rem 0.75rem',
            borderRadius: '9999px',
            background:
              !isOnline
                ? 'rgba(239, 68, 68, 0.12)'
                : backendStatus === 'CONNECTED'
                ? 'rgba(16, 185, 129, 0.12)'
                : backendStatus === 'CONNECTING'
                ? 'rgba(245, 158, 11, 0.12)'
                : 'rgba(239, 68, 68, 0.12)',
            border:
              !isOnline
                ? '1px solid rgba(239, 68, 68, 0.35)'
                : backendStatus === 'CONNECTED'
                ? '1px solid rgba(16, 185, 129, 0.35)'
                : backendStatus === 'CONNECTING'
                ? '1px solid rgba(245, 158, 11, 0.35)'
                : '1px solid rgba(239, 68, 68, 0.35)',
            color:
              !isOnline
                ? 'var(--accent-rose)'
                : backendStatus === 'CONNECTED'
                ? 'var(--aqi-good)'
                : backendStatus === 'CONNECTING'
                ? 'var(--accent-amber)'
                : 'var(--accent-rose)',
            fontSize: '0.75rem',
            fontWeight: 700,
            letterSpacing: '0.04em',
          }}
          title={
            !isOnline
              ? 'Browser is offline. Check your network or internet connection.'
              : backendStatus === 'CONNECTED'
              ? `Backend: ${backendHealth?.service} (Status: ${backendHealth?.status}, Time: ${backendHealth?.timestamp})`
              : backendStatus === 'CONNECTING'
              ? 'Connecting to AeroSentinel backend (/api/v1/health)...'
              : 'Backend unavailable (/api/v1/health unreachable)'
          }
        >
          <span
            style={{
              width: '7px',
              height: '7px',
              borderRadius: '50%',
              backgroundColor:
                !isOnline
                  ? 'var(--accent-rose)'
                  : backendStatus === 'CONNECTED'
                  ? 'var(--aqi-good)'
                  : backendStatus === 'CONNECTING'
                  ? 'var(--accent-amber)'
                  : 'var(--accent-rose)',
            }}
            className={isOnline && backendStatus === 'CONNECTED' ? 'live-indicator-dot' : ''}
          />
          <span>
            {!isOnline
              ? 'NETWORK: OFFLINE'
              : backendStatus === 'CONNECTED'
              ? 'SYSTEM: CONNECTED'
              : backendStatus === 'CONNECTING'
              ? 'CONNECTING...'
              : 'BACKEND: OFFLINE'}
          </span>
        </div>

        {/* Theme Toggle Button */}
        <button
          onClick={toggleTheme}
          style={{
            width: '36px',
            height: '36px',
            borderRadius: '10px',
            background: 'var(--bg-surface-elevated)',
            border: '1px solid var(--border-medium)',
            color: 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} mode`}
          aria-label="Toggle Theme"
        >
          {theme === 'dark' ? (
            <Sun size={17} color="#fbbf24" />
          ) : (
            <Moon size={17} color="var(--brand-primary)" />
          )}
        </button>

        {/* Notifications Icon with popover */}
        <div style={{ position: 'relative' }}>
          <button
            onClick={() => setNotificationsOpen(!notificationsOpen)}
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              background: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-medium)',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              position: 'relative',
            }}
            title="System Notifications"
            aria-label="Notifications"
          >
            <Bell size={17} />
            <span
              style={{
                position: 'absolute',
                top: '6px',
                right: '6px',
                width: '7px',
                height: '7px',
                borderRadius: '50%',
                backgroundColor: 'var(--accent-amber)',
              }}
            />
          </button>

          {notificationsOpen && (
            <div
              style={{
                position: 'absolute',
                top: 'calc(100% + 8px)',
                right: 0,
                width: '300px',
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-medium)',
                borderRadius: '14px',
                boxShadow: 'var(--shadow-lg)',
                padding: '0.85rem',
                zIndex: 200,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '0.6rem',
                  paddingBottom: '0.4rem',
                  borderBottom: '1px solid var(--border-subtle)',
                }}
              >
                <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Active Alerts ({selectedCity?.name || 'City'})
                </span>
                <span style={{ fontSize: '0.7rem', color: 'var(--accent-amber)', fontWeight: 600 }}>
                  1 Action Required
                </span>
              </div>
              <div
                style={{
                  fontSize: '0.78rem',
                  color: 'var(--text-secondary)',
                  lineHeight: 1.4,
                  padding: '0.4rem 0',
                }}
              >
                <strong>Industrial Zone Spike:</strong> Elevated PM2.5 threshold reached in Hadapsar (91 µg/m³).
              </div>
            </div>
          )}
        </div>

        {/* User / Profile Area */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.35rem 0.75rem',
            borderRadius: '10px',
            background: 'var(--bg-surface-elevated)',
            border: '1px solid var(--border-medium)',
          }}
        >
          <div
            style={{
              width: '24px',
              height: '24px',
              borderRadius: '6px',
              background: 'linear-gradient(135deg, var(--brand-primary), var(--accent-blue))',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff',
            }}
          >
            <ShieldCheck size={14} />
          </div>
          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Admin
          </span>
        </div>
      </div>
    </header>
  );
};
