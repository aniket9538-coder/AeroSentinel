import React, { useMemo } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
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
import { useAuthorityQueue } from '../../hooks/useAuthorityQueue';

export interface NavSection {
  title: string;
  items: {
    label: string;
    path: string;
    icon: LucideIcon;
    badge?: string;
  }[];
}

export const getNavSections = (activeAlertCount: number = 0): NavSection[] => [
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
      { label: 'Evidence & WHY', path: '/analyst/evidence', icon: Sparkles },
    ],
  },
  {
    title: 'ACTION',
    items: [
      {
        label: 'Alerts',
        path: '/authority/alerts',
        icon: BellRing,
        badge: activeAlertCount > 0 ? String(activeAlertCount) : undefined,
      },
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
  const location = useLocation();

  // Retrieve live authority queue items safely without crashing if API fails
  const { items } = useAuthorityQueue();
  const activeAlertCount = useMemo(() => {
    if (!Array.isArray(items)) return 0;
    // Semantics matching Alerts authority queue: active non-resolved alert candidates (OPEN / ACKNOWLEDGED)
    return items.filter((item) => item.status && item.status !== 'RESOLVED').length;
  }, [items]);

  const navSections = useMemo(() => getNavSections(activeAlertCount), [activeAlertCount]);

  return (
    <aside
      className="app-sidebar"
      style={{
        width: sidebarOpen ? '240px' : '0px',
        minWidth: sidebarOpen ? '240px' : '0px',
        borderRight: sidebarOpen ? '1px solid var(--border-subtle)' : 'none',
        visibility: sidebarOpen ? 'visible' : 'hidden',
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
          flexShrink: 0,
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
      <div style={{ padding: '1rem 0.75rem', flex: 1, minWidth: '240px', overflowY: 'auto', overflowX: 'hidden' }}>
        {navSections.map((section) => (
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
                const isCurrentActive =
                  item.path === '/analyst/evidence'
                    ? location.pathname === '/analyst/evidence' || location.pathname === '/gemini-why'
                    : location.pathname === item.path;

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    style={({ isActive }) => {
                      const active = isCurrentActive || isActive;
                      return {
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.5rem 0.75rem',
                        borderRadius: '8px',
                        fontSize: '0.85rem',
                        fontWeight: active ? 600 : 500,
                        textDecoration: 'none',
                        color: active ? 'var(--brand-primary)' : 'var(--text-secondary)',
                        background: active ? 'var(--brand-surface)' : 'transparent',
                        borderLeft: active ? '3px solid var(--brand-primary)' : '3px solid transparent',
                        transition: 'all 0.15s ease',
                      };
                    }}
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
          flexShrink: 0,
        }}
      >
        <span>National Hackathon 2026</span>
        <span style={{ color: 'var(--aqi-good)', fontWeight: 600 }}>● SECURE</span>
      </div>
    </aside>
  );
};
