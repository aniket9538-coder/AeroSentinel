import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { EvidenceTimeline, defaultEvidenceItems } from '../../components/evidence/EvidenceTimeline';
import { GeminiExplanation } from '../../components/evidence/GeminiExplanation';
import { useApp } from '../../store/AppContext';
import {
  Sparkles,
  Layers,
  MapPin,
  Clock,
  ShieldAlert,
  Flame,
  Wind,
  Satellite,
  Eye,
  Download,
  Share2,
  RefreshCw,
  TrendingUp,
} from 'lucide-react';

export const EvidenceAnalysis: React.FC = () => {
  const { selectedCity, isLive, lastUpdated } = useApp();
  const [selectedEventId, setSelectedEventId] = useState('EVENT-1023');
  const [selectedCell, setSelectedCell] = useState('8860144aa1fffff');

  const events = [
    { id: 'EVENT-1023', h3: '8860144aa1fffff', area: 'Shivajinagar Industrial Node', riskLevel: 'HIGH', score: 0.84, signalsCount: 6 },
    { id: 'EVENT-1024', h3: '8860144aa1bffff', area: 'Hadapsar Corridor', riskLevel: 'HIGH', score: 0.79, signalsCount: 5 },
    { id: 'EVENT-1025', h3: '8860144aa7fffff', area: 'Bhosari MIDC Cluster', riskLevel: 'MODERATE', score: 0.63, signalsCount: 4 },
  ];

  const currentEvent = events.find((e) => e.id === selectedEventId) || events[0];

  return (
    <PageContainer
      title="Evidence Intelligence & Causal Attribution"
      subtitle="Multi-modal cross-sensor corroboration graph and physical atmospheric reasoning"
      actions={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: isLive ? 'var(--aqi-good)' : 'var(--accent-amber)' }} className={isLive ? 'live-indicator-dot' : ''} />
            <span>Telemetry Live ({selectedCity?.name || 'City'})</span>
          </div>
          <Button variant="outline" size="sm" onClick={() => window.print()}>
            <Download size={14} style={{ marginRight: '0.4rem' }} /> Export Dossier
          </Button>
        </div>
      }
    >
      {/* Event Selector & Top Context Ribbon */}
      <div
        style={{
          padding: '1.25rem 1.5rem',
          borderRadius: '16px',
          background: 'var(--bg-card)',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-sm)',
          marginBottom: '1.75rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          <div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Target Investigation Event
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '0.2rem' }}>
              <select
                value={selectedEventId}
                onChange={(e) => {
                  setSelectedEventId(e.target.value);
                  const matched = events.find((ev) => ev.id === e.target.value);
                  if (matched) setSelectedCell(matched.h3);
                }}
                style={{
                  padding: '0.4rem 0.8rem',
                  borderRadius: '8px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-medium)',
                  color: 'var(--text-primary)',
                  fontWeight: 700,
                  fontSize: '0.95rem',
                }}
              >
                {events.map((ev) => (
                  <option key={ev.id} value={ev.id}>
                    {ev.id} — {ev.area} ({ev.riskLevel})
                  </option>
                ))}
              </select>
              <Badge variant="danger">Potential Hotspot: {currentEvent.riskLevel}</Badge>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', paddingLeft: '1rem', borderLeft: '1px solid var(--border-subtle)' }}>
            <MapPin size={16} color="var(--brand-primary)" />
            <div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Spatial Hex Index</div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                {selectedCell}
              </div>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', flexWrap: 'wrap' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Risk Probability</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--accent-rose)', fontFamily: 'var(--font-mono)' }}>
              {currentEvent.score}
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Corroborated Signals</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>
              {currentEvent.signalsCount} Distinct
            </div>
          </div>
        </div>
      </div>

      {/* Two Column Layout: Evidence Timeline (Left) & Gemini WHY Panel (Right) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(340px, 1.15fr) minmax(360px, 1.25fr)', gap: '1.75rem', alignItems: 'start' }}>
        {/* Left Column: Vertical Chronological Evidence Timeline */}
        <div>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Clock size={18} color="var(--brand-primary)" />
                <span>Multi-Source Evidence Timeline</span>
              </div>
            }
            subtitle="Chronological sequence of corroborated physical signals"
            badge={<Badge variant="info">6 Time-Stamped Steps</Badge>}
          >
            <div style={{ marginTop: '1rem' }}>
              <EvidenceTimeline items={defaultEvidenceItems} />
            </div>
          </Card>

          {/* Cross-corroboration Sensor Summary */}
          <div style={{ marginTop: '1.5rem' }}>
            <Card title="Signal Coverage & Quality Index">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginTop: '0.5rem' }}>
                <div style={{ padding: '0.75rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--brand-primary)', fontSize: '0.8rem', fontWeight: 600 }}>
                    <TrendingUp size={14} /> Ground Telemetry
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Continuous CPCB CAAQMS (118 µg/m³)</div>
                </div>

                <div style={{ padding: '0.75rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-teal)', fontSize: '0.8rem', fontWeight: 600 }}>
                    <Wind size={14} /> Surface Microclimate
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>1.1 m/s stagnation (ventilation lock)</div>
                </div>

                <div style={{ padding: '0.75rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-amber)', fontSize: '0.8rem', fontWeight: 600 }}>
                    <Flame size={14} /> NASA Thermal Hotspot
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Active fire signal 1.4km upwind (14.8 MW)</div>
                </div>

                <div style={{ padding: '0.75rem', background: 'var(--bg-surface-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--accent-purple)', fontSize: '0.8rem', fontWeight: 600 }}>
                    <Eye size={14} /> Citizen & Satellite
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Sentinel-5P NO2 + Citizen photo plume</div>
                </div>
              </div>
            </Card>
          </div>
        </div>

        {/* Right Column: Premium Gemini WHY Reasoning Panel */}
        <div>
          <GeminiExplanation
            eventId={currentEvent.id}
            h3Index={selectedCell}
          />
        </div>
      </div>
    </PageContainer>
  );
};
