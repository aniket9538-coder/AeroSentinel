import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loading } from '../../components/common/Loading';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { MonitoringRecommendationDetailCard } from '../../components/monitoring/MonitoringRecommendationDetailCard';
import { PollutionMap } from '../../components/map/PollutionMap';
import { useApp } from '../../store/AppContext';
import { useMonitoringRecommendations, PriorityFilterOption } from '../../hooks/useMonitoringRecommendations';
import { MonitoringRecommendation } from '../../types/monitoring';
import {
  Radio,
  RefreshCw,
  AlertTriangle,
  Compass,
  Shield,
  TrendingUp,
  MapPin,
  Layers,
  ChevronRight,
  Info,
} from 'lucide-react';

export const MonitoringDashboard: React.FC = () => {
  const { selectedCity } = useApp();
  const cityId = selectedCity?.id;
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');

  const {
    recommendations,
    filteredRecommendations,
    loading,
    error,
    priorityFilter,
    setPriorityFilter,
    selectedH3Index,
    selectedRecommendation,
    selectH3,
    refresh,
    summaryMetrics,
  } = useMonitoringRecommendations(cityId);

  const getPriorityColor = (level: string) => {
    switch (level) {
      case 'HIGH':
        return '#ec4899';
      case 'MEDIUM':
        return '#a855f7';
      case 'LOW':
      default:
        return '#6366f1';
    }
  };

  const getPriorityBadgeVariant = (level: string) => {
    switch (level) {
      case 'HIGH':
        return 'danger';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
      default:
        return 'info';
    }
  };

  return (
    <PageContainer
      title="MONITORING PRIORITY"
      subtitle="The dashboard identifies H3 cells where pollution risk, forecast uncertainty, and monitoring coverage indicate a need for additional observation."
      action={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div
            style={{
              display: 'flex',
              borderRadius: '8px',
              overflow: 'hidden',
              border: '1px solid var(--border-medium)',
              background: 'var(--bg-surface)',
            }}
            role="group"
            aria-label="View mode toggle"
          >
            <button
              onClick={() => setViewMode('list')}
              aria-pressed={viewMode === 'list'}
              style={{
                padding: '0.35rem 0.75rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                cursor: 'pointer',
                border: 'none',
                background: viewMode === 'list' ? 'var(--brand-primary)' : 'transparent',
                color: viewMode === 'list' ? '#ffffff' : 'var(--text-secondary)',
                transition: 'all 0.15s ease',
              }}
            >
              List View
            </button>
            <button
              onClick={() => setViewMode('map')}
              aria-pressed={viewMode === 'map'}
              style={{
                padding: '0.35rem 0.75rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                cursor: 'pointer',
                border: 'none',
                background: viewMode === 'map' ? 'var(--brand-primary)' : 'transparent',
                color: viewMode === 'map' ? '#ffffff' : 'var(--text-secondary)',
                transition: 'all 0.15s ease',
              }}
            >
              Map View
            </button>
          </div>

          {selectedCity && (
            <Badge variant="neutral">
              <MapPin size={12} style={{ marginRight: '0.3rem' }} />
              {selectedCity.name}
            </Badge>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => refresh()}
            disabled={loading}
            aria-label="Refresh monitoring recommendations"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} style={{ marginRight: '0.35rem' }} />
            <span>Refresh</span>
          </Button>
        </div>
      }
    >
      {/* Operational Protocol & Non-Alarmist Guidance Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          padding: '0.75rem 1.25rem',
          borderRadius: '10px',
          background: 'rgba(56, 189, 248, 0.08)',
          border: '1px solid rgba(56, 189, 248, 0.25)',
          fontSize: '0.825rem',
          color: 'var(--text-secondary)',
          lineHeight: 1.45,
        }}
      >
        <Info size={18} color="var(--brand-primary)" style={{ flexShrink: 0 }} />
        <span>
          <strong>Decision Support Framework:</strong> Monitoring priority combines F3 atmospheric risk,
          F4 forecast interval uncertainty, and proximity to active CAAQMS monitoring stations.
          Coverage gap indicates limited proximity to existing monitoring stations; it does not by itself indicate pollution.
        </span>
      </div>

      {/* Summary Metrics Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '1rem',
        }}
      >
        {/* Metric 1: Total Evaluated Cells */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(56, 189, 248, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--brand-primary)',
              }}
            >
              <Layers size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Total Cells
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {loading ? '—' : summaryMetrics.totalCells}
              </div>
            </div>
          </div>
        </Card>

        {/* Metric 2: HIGH Priority */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(236, 72, 153, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ec4899',
              }}
            >
              <Radio size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                High Priority
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#ec4899' }}>
                {loading ? '—' : summaryMetrics.highPriorityCount}
              </div>
            </div>
          </div>
        </Card>

        {/* Metric 3: MEDIUM Priority */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(168, 85, 247, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#a855f7',
              }}
            >
              <Radio size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Medium Priority
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#a855f7' }}>
                {loading ? '—' : summaryMetrics.mediumPriorityCount}
              </div>
            </div>
          </div>
        </Card>

        {/* Metric 4: LOW Priority */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(99, 102, 241, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#6366f1',
              }}
            >
              <Radio size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Low Priority
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#6366f1' }}>
                {loading ? '—' : summaryMetrics.lowPriorityCount}
              </div>
            </div>
          </div>
        </Card>

        {/* Metric 5: Coverage Gaps */}
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
                color: 'var(--accent-amber)',
              }}
            >
              <Compass size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Coverage Gaps
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {loading ? '—' : summaryMetrics.coverageGapsCount}
              </div>
            </div>
          </div>
        </Card>

        {/* Metric 6: Max Station Distance */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                backgroundColor: 'rgba(20, 184, 166, 0.12)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--accent-teal)',
              }}
            >
              <MapPin size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Max Distance
              </div>
              <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {loading ? '—' : `${summaryMetrics.maxDistanceKm} km`}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Priority Filter Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          flexWrap: 'wrap',
          padding: '0.4rem',
          background: 'var(--bg-surface)',
          borderRadius: '10px',
          border: '1px solid var(--border-subtle)',
          width: 'fit-content',
        }}
        role="group"
        aria-label="Priority level filters"
      >
        {(['ALL', 'HIGH', 'MEDIUM', 'LOW'] as PriorityFilterOption[]).map((opt) => {
          const isSelected = priorityFilter === opt;
          const count =
            opt === 'ALL'
              ? summaryMetrics.totalCells
              : opt === 'HIGH'
              ? summaryMetrics.highPriorityCount
              : opt === 'MEDIUM'
              ? summaryMetrics.mediumPriorityCount
              : summaryMetrics.lowPriorityCount;

          return (
            <button
              key={opt}
              onClick={() => setPriorityFilter(opt)}
              aria-pressed={isSelected}
              style={{
                padding: '0.45rem 0.95rem',
                borderRadius: '8px',
                border: isSelected ? '1px solid var(--brand-primary)' : '1px solid transparent',
                background: isSelected ? 'var(--brand-surface)' : 'transparent',
                color: isSelected ? 'var(--brand-primary)' : 'var(--text-secondary)',
                fontWeight: isSelected ? 700 : 500,
                fontSize: '0.825rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.45rem',
                transition: 'all 0.15s ease',
              }}
            >
              <span>{opt === 'ALL' ? 'All Priority' : `${opt} Priority`}</span>
              <span
                style={{
                  fontSize: '0.725rem',
                  padding: '0.1rem 0.45rem',
                  borderRadius: '12px',
                  background: isSelected ? 'var(--brand-primary)' : 'var(--bg-surface-elevated)',
                  color: isSelected ? '#ffffff' : 'var(--text-muted)',
                  fontWeight: 600,
                }}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Main Content Area */}
      {loading ? (
        <Loading
          message="Computing monitoring priorities..."
          subMessage="Evaluating F3 atmospheric risk, F4 forecast uncertainty, and F8-P2 CAAQMS station distance"
        />
      ) : error ? (
        <ErrorState
          title="Monitoring recommendations are currently unavailable."
          message={error}
          onRetry={() => refresh()}
          retryLabel="Retry Recommendations"
        />
      ) : recommendations.length === 0 ? (
        <EmptyState
          title="No monitoring priorities available for this city."
          message="The current dataset does not contain enough qualifying F3/F4/monitoring information to generate recommendations."
          actionLabel="Refresh Data"
          onAction={() => refresh()}
        />
      ) : viewMode === 'map' ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 1.4fr) minmax(0, 1fr)',
            gap: '1.5rem',
            alignItems: 'start',
          }}
          className="monitoring-dashboard-grid"
        >
          <div
            style={{
              borderRadius: '14px',
              overflow: 'hidden',
              border: '1px solid var(--border-subtle)',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            <PollutionMap
              height="620px"
              cityId={cityId}
              showMonitoringCoverage={true}
              monitoringRecommendations={filteredRecommendations}
              selectedH3Index={selectedH3Index}
              onSelectMonitoringRecommendation={(rec) => selectH3(rec.h3Index)}
            />
          </div>
          <div style={{ position: 'sticky', top: '1.5rem' }}>
            <MonitoringRecommendationDetailCard
              recommendation={selectedRecommendation}
              isLoading={loading}
            />
          </div>
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 1.35fr) minmax(0, 1fr)',
            gap: '1.5rem',
            alignItems: 'start',
          }}
          className="monitoring-dashboard-grid"
        >
          {/* Left Column: Recommendations List */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            <div
              style={{
                fontSize: '0.825rem',
                color: 'var(--text-muted)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0 0.25rem',
              }}
            >
              <span>
                Showing <strong>{filteredRecommendations.length}</strong> of {recommendations.length} evaluated cells
              </span>
              <span style={{ fontSize: '0.75rem' }}>Ordered by numerical priority score (desc)</span>
            </div>

            {filteredRecommendations.length === 0 ? (
              <div
                style={{
                  padding: '3rem 1.5rem',
                  textAlign: 'center',
                  background: 'var(--bg-surface)',
                  borderRadius: '12px',
                  border: '1px dashed var(--border-medium)',
                  color: 'var(--text-muted)',
                }}
              >
                No recommendations match the selected <strong>{priorityFilter}</strong> filter.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {filteredRecommendations.map((rec) => {
                  const isSelected = rec.h3Index === selectedH3Index;
                  const priorityColor = getPriorityColor(rec.priorityLevel);
                  const distanceKm = rec.nearestStationDistanceKm ?? rec.stationDistanceKm;

                  return (
                    <div
                      key={rec.h3Index}
                      onClick={() => selectH3(rec.h3Index)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          selectH3(rec.h3Index);
                        }
                      }}
                      tabIndex={0}
                      role="button"
                      aria-pressed={isSelected}
                      aria-label={`Inspect H3 cell ${rec.h3Index} with ${rec.priorityLevel} priority`}
                      style={{
                        padding: '1.1rem 1.25rem',
                        borderRadius: '12px',
                        background: isSelected ? 'var(--bg-surface-elevated)' : 'var(--bg-surface)',
                        border: isSelected ? `2px solid ${priorityColor}` : '1px solid var(--border-subtle)',
                        boxShadow: isSelected ? `0 0 16px ${priorityColor}25` : 'none',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.75rem',
                      }}
                    >
                      {/* Top Header Row */}
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          flexWrap: 'wrap',
                          gap: '0.5rem',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                          <span
                            style={{
                              fontFamily: 'var(--font-mono)',
                              fontSize: '0.9rem',
                              fontWeight: 700,
                              color: 'var(--text-primary)',
                            }}
                          >
                            {rec.h3Index}
                          </span>
                          <Badge variant={getPriorityBadgeVariant(rec.priorityLevel)}>
                            {rec.priorityLevel} PRIORITY ({rec.priorityScorePercent ?? Math.round(rec.priorityScore)}/100)
                          </Badge>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: priorityColor, fontSize: '0.775rem', fontWeight: 600 }}>
                          <span>{isSelected ? 'Selected' : 'Inspect'}</span>
                          <ChevronRight size={14} />
                        </div>
                      </div>

                      {/* Middle Metrics Row */}
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fit, minmax(110px, 1fr))',
                          gap: '0.5rem',
                          padding: '0.6rem 0.75rem',
                          borderRadius: '8px',
                          background: 'var(--bg-surface)',
                          fontSize: '0.775rem',
                        }}
                      >
                        <div>
                          <span style={{ color: 'var(--text-muted)' }}>Risk: </span>
                          <strong style={{ color: 'var(--text-primary)' }}>{rec.riskScore.toFixed(2)}</strong>
                          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}> ({rec.riskLevel})</span>
                        </div>
                        <div>
                          <span style={{ color: 'var(--text-muted)' }}>Uncertainty: </span>
                          <strong style={{ color: 'var(--brand-primary)' }}>
                            {rec.uncertaintyIntervalWidth != null ? `${rec.uncertaintyIntervalWidth.toFixed(1)} µg/m³` : `${(rec.uncertainty * 100).toFixed(0)}%`}
                          </strong>
                        </div>
                        <div>
                          <span style={{ color: 'var(--text-muted)' }}>Distance: </span>
                          <strong style={{ color: 'var(--text-primary)' }}>
                            {distanceKm != null ? `${distanceKm.toFixed(1)} km` : 'N/A'}
                          </strong>
                        </div>
                        <div>
                          <span style={{ color: 'var(--text-muted)' }}>Coverage: </span>
                          <strong style={{ color: rec.monitoringCoverageGapFlag === 1 ? 'var(--accent-rose)' : 'var(--accent-emerald)' }}>
                            {rec.monitoringCoverageGapFlag === 1 ? 'Gap Present' : 'Within Range'}
                          </strong>
                        </div>
                      </div>

                      {/* Bottom Recommendation Guidance */}
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}>
                        <span
                          style={{
                            padding: '0.15rem 0.45rem',
                            borderRadius: '4px',
                            background: `${priorityColor}18`,
                            color: priorityColor,
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            whiteSpace: 'nowrap',
                            textTransform: 'uppercase',
                          }}
                        >
                          {rec.recommendationType.replace(/_/g, ' ')}
                        </span>
                        <span style={{ color: 'var(--text-secondary)', lineHeight: 1.4, fontWeight: 500 }}>
                          {rec.recommendation}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Right Column: Selected Cell Detailed Lineage Card */}
          <div style={{ position: 'sticky', top: '1.5rem' }}>
            <MonitoringRecommendationDetailCard
              recommendation={selectedRecommendation}
              isLoading={loading}
            />
          </div>
        </div>
      )}
    </PageContainer>
  );
};
export default MonitoringDashboard;
