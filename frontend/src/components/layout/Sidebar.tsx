import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LucideIcon,
  Wind,
  LayoutDashboard,
  CloudSun,
  Hexagon,
  Flame,
  TrendingUp,
  FileSearch,
  Sparkles,
  BellRing,
  ShieldAlert,
  Camera,
  Network,
  Sliders,
} from 'lucide-react';
import { useApp } from '../../store/AppContext';

interface NavSection {
  title: string;
  items: {
    label: string;
    path: string;
    icon: LucideIcon;
    badge?: string;
  }[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: 'MONITOR',
    items: [
      { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
      { label: 'Air Quality', path: '/air-quality', icon: Wind },
      { label: 'Pollution Map', path: '/map', icon: Hexagon },
      { label: 'Weather & Spatial', path: '/weather', icon: CloudSun },
      { label: 'Hotspots', path: '/hotspots', icon: Flame },
      { label: 'Forecast', path: '/forecast', icon: TrendingUp },
    ],
  },
  {
    title: 'INTELLIGENCE',
    items: [
      { label: 'Evidence', path: '/analyst/evidence', icon: FileSearch },
      { label: 'Gemini WHY', path: '/analyst/evidence?tab=gemini', icon: Sparkles },
    ],
  },
  {
    title: 'ACTION',
    items: [
      { label: 'Alerts', path: '/authority/alerts', icon: BellRing, badge: '1' },
      { label: 'Authority', path: '/authority/incidents', icon: ShieldAlert },
      { label: 'Citizen Reports', path: '/citizen/report', icon: Camera },
    ],
  },
  {
    title: 'NETWORK',
    items: [
      { label: 'Federated Network', path: '/federated', icon: Network },
    ],
  },
  {
    title: 'SYSTEM',
    items: [
      { label: 'Settings', path: '/admin/status', icon: Sliders },
    ],
  },
];

export const Sidebar: React.FC = () => {
  const { sidebarOpen } = useApp();

  return (
    <aside
      style={{
        width: sidebarOpen ? '240px' : '0px',
        minWidth: sidebarOpen ? '240px' : '0px',
        background: 'var(--bg-surface)',
        borderRight: sidebarOpen ? '1px solid var(--border-subtle)' : 'none',
        height: '100vh',
        position: 'sticky',
        top: 0,
        display: 'flex',
        flexDirection: 'column',
        overflowY: 'auto',
        overflowX: 'hidden',
        transition: 'width 0.2s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
        zIndex: 90,
      }}
    >
      {/* Brand Header */}
      <div
        style={{
          padding: '1.25rem 1.25rem 1rem 1.25rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          borderBottom: '1px solid var(--border-subtle)',
          minWidth: '240px',
        }}
      >
        <div
          style={{
            width: '34px',
            height: '34px',
            borderRadius: '9px',
            background: 'linear-gradient(135deg, var(--brand-primary), var(--accent-blue))',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 14px rgba(56, 189, 248, 0.35)',
            flexShrink: 0,
          }}
        >
          <Wind size={18} color="#ffffff" />
        </div>
        <div>
          <div
            style={{
              fontSize: '1.1rem',
              fontWeight: 800,
              fontFamily: 'var(--font-heading)',
              letterSpacing: '-0.02em',
              lineHeight: 1.1,
              color: 'var(--text-primary)',
            }}
          >
            AERO<span style={{ color: 'var(--brand-primary)' }}>SENTINEL</span>
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: 500, letterSpacing: '0.04em' }}>
            CLIMATE INTELLIGENCE
          </div>
        </div>
      </div>

      {/* Navigation Sections */}
      <div style={{ padding: '1rem 0.75rem', flex: 1, minWidth: '240px' }}>
        {NAV_SECTIONS.map((section) => (
          <div key={section.title} style={{ marginBottom: '1.25rem' }}>
            <div
              style={{
                fontSize: '0.675rem',
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '0.08em',
                color: 'var(--text-muted)',
                padding: '0.2rem 0.75rem 0.4rem 0.75rem',
              }}
            >
              {section.title}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
              {section.items.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    style={({ isActive }) => ({
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.5rem 0.75rem',
                      borderRadius: '8px',
                      fontSize: '0.85rem',
                      fontWeight: isActive ? 600 : 500,
                      textDecoration: 'none',
                      color: isActive ? 'var(--brand-primary)' : 'var(--text-secondary)',
                      background: isActive ? 'var(--brand-surface)' : 'transparent',
                      borderLeft: isActive ? '3px solid var(--brand-primary)' : '3px solid transparent',
                      transition: 'all 0.15s ease',
                    })}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                      <Icon size={16} />
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span
                        style={{
                          fontSize: '0.65rem',
                          padding: '0.1rem 0.4rem',
                          borderRadius: '9999px',
                          background: 'rgba(245, 158, 11, 0.2)',
                          color: 'var(--accent-amber)',
                          fontWeight: 700,
                        }}
                      >
                        {item.badge}
                      </span>
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Footer Info */}
      <div
        style={{
          padding: '0.85rem 1.25rem',
          borderTop: '1px solid var(--border-subtle)',
          fontSize: '0.72rem',
          color: 'var(--text-muted)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          minWidth: '240px',
        }}
      >
        <span>National Hackathon 2026</span>
        <span style={{ color: 'var(--aqi-good)', fontWeight: 600 }}>● SECURE</span>
      </div>
    </aside>
  );
};
