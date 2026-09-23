import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Wind, Shield, BarChart3, AlertTriangle, Cpu } from 'lucide-react';

export const Navbar: React.FC = () => {
  const location = useLocation();

  const navItems = [
    { label: 'Live Map', path: '/map', icon: Wind },
    { label: 'Forecast', path: '/forecast', icon: BarChart3 },
    { label: 'Hotspot Analysis', path: '/analyst/hotspots', icon: AlertTriangle },
    { label: 'Authority Triage', path: '/authority/incidents', icon: Shield },
    { label: 'Federated Network', path: '/federated', icon: Cpu },
  ];

  return (
    <nav style={{
      background: 'rgba(11, 15, 25, 0.85)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
      position: 'sticky',
      top: 0,
      zIndex: 50,
      padding: '0.75rem 2rem',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
    }}>
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none' }}>
        <div style={{
          width: '36px',
          height: '36px',
          borderRadius: '8px',
          background: 'linear-gradient(135deg, #06b6d4, #3b82f6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 15px rgba(6, 182, 212, 0.4)',
        }}>
          <Wind size={20} color="#ffffff" />
        </div>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#f3f4f6' }}>Aero</span>
          <span style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#06b6d4' }}>Sentinel</span>
        </div>
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                textDecoration: 'none',
                fontSize: '0.875rem',
                fontWeight: 500,
                color: isActive ? '#38bdf8' : '#9ca3af',
                padding: '0.5rem 0.75rem',
                borderRadius: '6px',
                transition: 'all 0.15s ease',
                backgroundColor: isActive ? 'rgba(56, 189, 248, 0.1)' : 'transparent',
              }}
            >
              <Icon size={16} />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <Link
          to="/citizen/report"
          style={{
            background: 'linear-gradient(135deg, #06b6d4, #2563eb)',
            color: '#ffffff',
            padding: '0.45rem 1rem',
            borderRadius: '8px',
            fontSize: '0.875rem',
            fontWeight: 600,
            textDecoration: 'none',
            boxShadow: '0 2px 8px rgba(6, 182, 212, 0.3)',
          }}
        >
          Report Emission
        </Link>
      </div>
    </nav>
  );
};
