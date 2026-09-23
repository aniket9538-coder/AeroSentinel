import React from 'react';
import { Link, useLocation } from 'react-router-dom';

interface SidebarItem {
  label: string;
  path: string;
  icon?: React.ReactNode;
}

interface SidebarProps {
  items: SidebarItem[];
  title?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ items, title }) => {
  const location = useLocation();

  return (
    <aside style={{
      width: '240px',
      borderRight: '1px solid rgba(255, 255, 255, 0.1)',
      background: 'rgba(17, 24, 39, 0.5)',
      padding: '1.5rem 1rem',
      height: 'calc(100vh - 65px)',
      position: 'sticky',
      top: '65px',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
    }}>
      {title && (
        <h4 style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280', paddingLeft: '0.75rem', marginBottom: '0.5rem' }}>
          {title}
        </h4>
      )}
      {items.map((item) => {
        const isActive = location.pathname === item.path;
        return (
          <Link
            key={item.path}
            to={item.path}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.6rem 0.75rem',
              borderRadius: '8px',
              fontSize: '0.875rem',
              fontWeight: 500,
              textDecoration: 'none',
              color: isActive ? '#38bdf8' : '#9ca3af',
              background: isActive ? 'rgba(56, 189, 248, 0.1)' : 'transparent',
            }}
          >
            {item.icon}
            <span>{item.label}</span>
          </Link>
        );
      })}
    </aside>
  );
};
