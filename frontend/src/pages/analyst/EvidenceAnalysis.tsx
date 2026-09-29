import React, { useState, useEffect } from 'react';
import { useSearchParams, useLocation, Link } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { EvidencePanel } from '../../components/hotspot/EvidencePanel';
import { useEvidence } from '../../hooks/useEvidence';
import { useApp } from '../../store/AppContext';
import {
  Sparkles,
  MapPin,
  RefreshCw,
  TrendingUp,
  Search,
  ExternalLink,
  ShieldAlert,
} from 'lucide-react';

// Authoritative monitoring locations with known H3 cell associations
const PRESET_CELLS = [
  {
    h3Index: '88608850e5fffff',
    cityName: 'Pune',
    areaName: 'Shivajinagar',
    stationName: 'Shivajinagar CAAQMS (PUN-001)',
    stationId: 'PUN-001',
    isPrimary: true,
  },
  {
    h3Index: '88608852c1fffff',
    cityName: 'Pune',
    areaName: 'Katraj',
    stationName: 'Katraj Air Station (PUN-002)',
    stationId: 'PUN-002',
    isPrimary: false,
  },
  {
    h3Index: '8860885357fffff',
    cityName: 'Pune',
    areaName: 'Hadapsar',
    stationName: 'Hadapsar Industrial Zone (PUN-003)',
    stationId: 'PUN-003',
    isPrimary: false,
  },
  {
    h3Index: '88608b56b3fffff',
    cityName: 'Mumbai',
    areaName: 'Kurla',
    stationName: 'Kurla Station (MUM-001)',
    stationId: 'MUM-001',
    isPrimary: false,
  },
  {
    h3Index: '883da11505fffff',
    cityName: 'Delhi',
    areaName: 'R K Puram',
    stationName: 'R K Puram Station (DEL-001)',
    stationId: 'DEL-001',
    isPrimary: false,
  },
];

export const EvidenceAnalysis: React.FC = () => {
  const { selectedCity, isLive } = useApp();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();

  // Resolve initial H3 with strict F3/F4 continuity priority:
  // 1. URL search parameter ?h3=...
  // 2. React Router location state: location.state?.h3Index
  // 3. Session storage preserved from F3/F4 selection
  // 4. Default to Pune Shivajinagar baseline cell
  const getInitialH3 = (): string => {
    const urlH3 = searchParams.get('h3');
    if (urlH3 && /^88[0-9a-f]{13}$/i.test(urlH3.trim())) {
      return urlH3.trim();
    }
    const stateH3 = (location.state as { h3Index?: string })?.h3Index;
    if (stateH3 && /^88[0-9a-f]{13}$/i.test(stateH3.trim())) {
      return stateH3.trim();
    }
    const storedH3 =
      typeof sessionStorage !== 'undefined'
        ? sessionStorage.getItem('aerosentinel_selected_h3')
        : null;
    if (storedH3 && /^88[0-9a-f]{13}$/i.test(storedH3.trim())) {
      return storedH3.trim();
    }
    return '88608850e5fffff';
  };

  const [selectedH3, setSelectedH3] = useState<string>(getInitialH3);
  const [customH3Input, setCustomH3Input] = useState<string>('');

  // Call real Evidence API via useEvidence hook with automatic cancellation and stale-response guard
  const { evidence, loading, error, refresh } = useEvidence(selectedH3);

  // Synchronize when URL ?h3=... parameter changes externally
  const urlH3 = searchParams.get('h3');
  useEffect(() => {
    if (urlH3 && /^88[0-9a-f]{13}$/i.test(urlH3.trim())) {
      setSelectedH3(urlH3.trim());
      sessionStorage.setItem('aerosentinel_selected_h3', urlH3.trim());
    }
  }, [urlH3]);

  // Persist selectedH3 across session
  useEffect(() => {
    if (selectedH3) {
      sessionStorage.setItem('aerosentinel_selected_h3', selectedH3);
    }
  }, [selectedH3]);

  const handleCellSelect = (h3: string) => {
    setSelectedH3(h3);
    setSearchParams({ h3 });
  };

  const handleCustomH3Submit = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = customH3Input.trim().toLowerCase();
    if (/^88[0-9a-f]{13}$/.test(clean)) {
      handleCellSelect(clean);
      setCustomH3Input('');
    }
  };

  const currentPreset = PRESET_CELLS.find((p) => p.h3Index === selectedH3);
  const activeCityName = currentPreset?.cityName || evidence?.context?.cityName || selectedCity?.name || 'Pune';
  const activeEventCode = evidence?.context?.eventCode || (evidence?.context?.eventId ? `${evidence.context.eventId.slice(0, 8)}...` : 'N/A');
  const formattedTimestamp = evidence?.context?.generatedAt
    ? new Date(evidence.context.generatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : 'Realtime';

  return (
    <PageContainer
      title="Evidence & WHY"
      subtitle="Why this H3 cell is being investigated"
      actions={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: isLive ? 'var(--aqi-good)' : 'var(--accent-amber)',
              }}
              className={isLive ? 'live-indicator-dot' : ''}
            />
            <span>Live ({activeCityName})</span>
          </div>
          <Button variant="outline" size="sm" onClick={() => refresh()} disabled={loading}>
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} style={{ marginRight: '0.4rem' }} />
            Refresh
          </Button>
          <Link to={`/forecast?h3=${encodeURIComponent(selectedH3)}`} style={{ textDecoration: 'none' }}>
            <Button variant="outline" size="sm">
              <TrendingUp size={14} style={{ marginRight: '0.4rem' }} />
              Forecast
            </Button>
          </Link>
          <Link to="/hotspots" style={{ textDecoration: 'none' }}>
            <Button variant="outline" size="sm">
              <ShieldAlert size={14} style={{ marginRight: '0.4rem' }} />
              Hotspot
            </Button>
          </Link>
          {evidence?.evidence?.triageState === 'ALERT_CANDIDATE' && (
            <Link to={`/authority/alerts?h3=${encodeURIComponent(selectedH3)}`} style={{ textDecoration: 'none' }}>
              <Button variant="primary" size="sm">
                <Sparkles size={14} style={{ marginRight: '0.4rem' }} />
                View Alert
              </Button>
            </Link>
          )}
        </div>
      }
    >
      {/* Target Cell Context Strip */}
      <div
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '12px',
          background: 'var(--bg-card)',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-sm)',
          marginBottom: '1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap' }}>
          {/* Target Cell Selector */}
          <div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Selected H3 Cell
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.2rem' }}>
              <select
                value={PRESET_CELLS.some((p) => p.h3Index === selectedH3) ? selectedH3 : 'custom'}
                onChange={(e) => {
                  if (e.target.value !== 'custom') {
                    handleCellSelect(e.target.value);
                  }
                }}
                style={{
                  padding: '0.4rem 0.75rem',
                  borderRadius: '6px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-medium)',
                  color: 'var(--text-primary)',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                }}
              >
                {PRESET_CELLS.map((cell) => (
                  <option key={cell.h3Index} value={cell.h3Index}>
                    {cell.cityName} — {cell.areaName} ({cell.stationId})
                  </option>
                ))}
                {!PRESET_CELLS.some((p) => p.h3Index === selectedH3) && (
                  <option value="custom">Custom ({selectedH3.slice(0, 8)}...)</option>
                )}
              </select>
            </div>
          </div>

          {/* Active H3 Hex */}
          <div style={{ paddingLeft: '1rem', borderLeft: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>H3 Index</div>
            <div style={{ fontSize: '0.825rem', fontWeight: 700, color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>
              {selectedH3}
            </div>
          </div>

          {/* City */}
          <div style={{ paddingLeft: '1rem', borderLeft: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>City</div>
            <div style={{ fontSize: '0.825rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {activeCityName}
            </div>
          </div>

          {/* Canonical Event */}
          <div style={{ paddingLeft: '1rem', borderLeft: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Event</div>
            <div style={{ fontSize: '0.825rem', fontWeight: 600, color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
              {activeEventCode}
            </div>
          </div>

          {/* Timestamp */}
          <div style={{ paddingLeft: '1rem', borderLeft: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Timestamp</div>
            <div style={{ fontSize: '0.825rem', fontWeight: 500, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
              {formattedTimestamp}
            </div>
          </div>
        </div>

        {/* Compact Custom H3 Hex Input */}
        <form onSubmit={handleCustomH3Submit} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <input
            type="text"
            placeholder="Search custom H3 (8860...)"
            value={customH3Input}
            onChange={(e) => setCustomH3Input(e.target.value)}
            style={{
              padding: '0.4rem 0.65rem',
              borderRadius: '6px',
              border: '1px solid var(--border-medium)',
              background: 'var(--bg-surface-elevated)',
              color: 'var(--text-primary)',
              fontSize: '0.8rem',
              fontFamily: 'var(--font-mono)',
              width: '200px',
            }}
          />
          <Button type="submit" size="sm" variant="outline" disabled={!/^88[0-9a-f]{13}$/i.test(customH3Input.trim())}>
            <Search size={13} style={{ marginRight: '0.25rem' }} /> Hex
          </Button>
        </form>
      </div>

      {/* Authoritative Evidence Panel (Strict 4-Tier Separation & Lineage) */}
      <EvidencePanel
        evidence={evidence}
        isLoading={loading}
        error={error}
        onRefresh={refresh}
        selectedH3={selectedH3}
      />
    </PageContainer>
  );
};

export default EvidenceAnalysis;
