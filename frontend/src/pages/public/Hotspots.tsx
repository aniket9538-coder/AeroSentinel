import React, { useEffect } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { PollutionMap } from '../../components/map/PollutionMap';
import { RiskLegend } from '../../components/hotspot/RiskLegend';
import { HotspotCellDetailsCard } from '../../components/hotspot/HotspotCellDetailsCard';
import { useApp } from '../../store/AppContext';
import { useHotspots } from '../../hooks/useHotspots';
import { getRiskStyle } from '../../utils/hotspotColors';
import {
  Flame,
  Hexagon,
  TrendingUp,
  AlertTriangle,
  RefreshCw,
  Cpu,
  Layers,
  Activity,
  CheckCircle2,
} from 'lucide-react';

export const Hotspots: React.FC = () => {
  const { selectedCity, stations } = useApp();
  const {
    overview,
    loading,
    error,
    selectedH3Index,
    selectedCell,
    loadingSelectedCell,
    refresh,
    selectCell,
  } = useHotspots(selectedCity?.id);

  // Auto-select first cell if none selected
  useEffect(() => {
    if (!selectedH3Index && overview?.cells && overview.cells.length > 0) {
      selectCell(overview.cells[0].h3Index);
    }
  }, [overview, selectedH3Index, selectCell]);

  const cells = overview?.cells || [];

  const formattedGeneratedAt = (() => {
    if (loading) return 'Loading...';
    if (!overview?.generatedAt) return 'Pending ingestion...';
    try {
      return new Date(overview.generatedAt).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });
    } catch {
      return overview.generatedAt;
    }
  })();

  return (
    <PageContainer
      title="POTENTIAL HOTSPOTS & SPATIAL RISK"
      subtitle="Early environmental risk clustering powered by the F2 H3 spatial engine and multi-criteria atmospheric modeling."
      action={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Button
            size="sm"
            variant="outline"
            onClick={() => refresh()}
            disabled={loading}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} style={{ marginRight: '0.35rem' }} />
            <span>Refresh</span>
          </Button>
          <Badge variant={overview?.freshness === 'LIVE' ? 'success' : 'warning'}>
            {loading ? 'CONNECTING' : (overview?.freshness || 'CONNECTING')}
          </Badge>
        </div>
      }
    >
      {/* Top Advisory Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          padding: '0.75rem 1.25rem',
          borderRadius: '10px',
          background: 'rgba(245, 158, 11, 0.08)',
          border: '1px solid rgba(245, 158, 11, 0.25)',
          fontSize: '0.825rem',
          color: 'var(--text-secondary)',
        }}
      >
        <AlertTriangle size={18} color="var(--accent-amber)" style={{ flexShrink: 0 }} />
        <span>
          <strong>Operational Protocol:</strong> Hotspots represent statistical risk clusters derived
          from cross-sensor telemetry and atmospheric stagnation. AeroSentinel identifies <em>potential</em> hotspots
          for prioritized municipal inspection without making unverified source attribution claims.
        </span>
      </div>

      {/* =========================================================================
          SUMMARY METRICS ROW
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
        }}
      >
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(14, 165, 233, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Hexagon size={22} color="var(--brand-primary)" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Monitored H3 Cells</div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {loading ? '...' : (overview?.totalCells || 0)}
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(239, 68, 68, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Flame size={22} color="#ef4444" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>High-Risk Sectors</div>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ef4444' }}>
                {loading ? '...' : (overview?.highRiskCells || 0)}
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(16, 185, 129, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Cpu size={22} color="#10b981" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Engine Version</div>
              <div
                style={{
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  fontFamily: 'var(--font-mono)',
                  color: 'var(--text-primary)',
                  marginTop: '0.2rem',
                }}
              >
                {loading ? 'Loading...' : (overview?.modelVersion || 'hotspot-baseline-v1')}
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(245, 158, 11, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Activity size={22} color="#f59e0b" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Generated At</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {formattedGeneratedAt}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* =========================================================================
          MAIN MAP + SELECTED CELL PANEL (Side by Side)
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 2.3fr) minmax(340px, 1fr)',
          gap: '1.5rem',
          alignItems: 'stretch',
        }}
      >
        {/* Left Column: Legend + H3 Risk Map */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', minHeight: '560px' }}>
          <RiskLegend />
          <div style={{ flex: 1, minHeight: '500px' }}>
            <PollutionMap
              center={[selectedCity?.latitude ?? 18.5204, selectedCity?.longitude ?? 73.8567]}
              zoom={selectedCity?.defaultZoom || 12}
              cityId={selectedCity?.id}
              stations={stations}
              hotspots={cells}
              showHotspots={true}
              height="500px"
              onSelectH3Cell={(cell) => selectCell(cell.h3Index)}
              selectedCellId={selectedH3Index || undefined}
            />
          </div>
        </div>

        {/* Right Column: Selected Cell Details Card */}
        <div>
          <HotspotCellDetailsCard cell={selectedCell} isLoading={loading || loadingSelectedCell} />
        </div>
      </div>

      {/* =========================================================================
          POTENTIAL HOTSPOTS TABLE (Sorted by Risk Score Descending)
          ========================================================================= */}
      <Card
        title="Ranked Potential Hotspot Sectors"
        subtitle="H3 hexagonal spatial cells ordered by composite risk score"
        action={
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Showing {cells.length} cells in {selectedCity?.name || 'City'}
          </span>
        }
      >
        {loading ? (
          <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading potential hotspots from real production feature layer...
          </div>
        ) : error ? (
          <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--accent-red)' }}>
            {error}
          </div>
        ) : cells.length === 0 ? (
          <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            No hotspot data available for {selectedCity?.name || 'this city'}.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="aero-table">
              <thead>
                <tr>
                  <th>H3 Cell ID</th>
                  <th>Risk Level</th>
                  <th>Risk Score</th>
                  <th>Confidence</th>
                  <th>Freshness</th>
                  <th>Predicted At</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {cells.map((cell) => {
                  const isSelected = selectedH3Index === cell.h3Index;
                  const style = getRiskStyle(cell.riskLevel);
                  return (
                    <tr
                      key={cell.h3Index}
                      onClick={() => selectCell(cell.h3Index)}
                      style={{
                        cursor: 'pointer',
                        background: isSelected ? 'var(--brand-surface)' : 'transparent',
                      }}
                    >
                      <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--brand-primary)' }}>
                        {cell.h3Index}
                      </td>

                      <td>
                        <span
                          style={{
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            background: style.bg,
                            color: style.color,
                            border: `1px solid ${style.border}`,
                          }}
                        >
                          {style.label.toUpperCase()}
                        </span>
                      </td>

                      <td style={{ fontWeight: 700, color: style.color, fontSize: '0.95rem' }}>
                        {(cell.riskScore * 100).toFixed(1)}%
                      </td>

                      <td style={{ color: 'var(--text-secondary)' }}>
                        {(cell.confidence * 100).toFixed(0)}%
                      </td>

                      <td>
                        <Badge variant={cell.freshness === 'LIVE' ? 'success' : 'warning'}>
                          {cell.freshness}
                        </Badge>
                      </td>

                      <td style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {new Date(cell.predictedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>

                      <td>
                        <Button
                          size="sm"
                          variant={isSelected ? 'primary' : 'outline'}
                          onClick={(e) => {
                            e.stopPropagation();
                            selectCell(cell.h3Index);
                          }}
                        >
                          {isSelected ? 'Selected' : 'Inspect'}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </PageContainer>
  );
};

export default Hotspots;
