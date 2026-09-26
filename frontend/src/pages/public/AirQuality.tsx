import React, { useState, useEffect, useMemo } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loading } from '../../components/common/Loading';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { PollutionMap } from '../../components/map/PollutionMap';
import { PM25Chart, TrendPoint } from '../../components/charts/PM25Chart';
import { useApp } from '../../store/AppContext';
import { airQualityApi } from '../../services/airQualityApi';
import { AirQualityObservationResponse, HistoricalObservationResponse } from '../../types';
import {
  calculateFreshnessStatus,
  getFreshnessBadgeVariant,
  formatObservationTimestamp,
  isSourceUnavailableError,
  FreshnessStatus,
} from '../../utils/freshness';
import {
  Search,
  Clock,
  MapPin,
  RefreshCw,
} from 'lucide-react';

export const AirQuality: React.FC = () => {
  const {
    selectedCity,
    stations,
    isLoading,
    isCitiesLoading,
    isLive,
    isOnline,
    lastUpdated,
    refreshData,
    fetchCities,
    error,
    backendStatus,
  } = useApp();

  const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Station historical telemetry state
  const [historyReadings, setHistoryReadings] = useState<HistoricalObservationResponse[]>([]);
  const [isHistoryLoading, setIsHistoryLoading] = useState<boolean>(false);
  const [historyError, setHistoryError] = useState<string | null>(null);

  // Request race condition tracking ref for station history
  const activeHistoryReqIdRef = React.useRef<number>(0);

  // Handle active station selection cleanly from real returned observations
  const activeStation: AirQualityObservationResponse | null = useMemo(() => {
    if (stations.length === 0) return null;
    if (selectedStationId) {
      const found = stations.find((s) => s.stationId === selectedStationId);
      if (found) return found;
    }
    return stations[0];
  }, [stations, selectedStationId]);

  // Distinguish genuine upstream provider / backend unavailability from generic errors
  const isSourceUnavail = useMemo(() => {
    return isOnline && isSourceUnavailableError(error, backendStatus);
  }, [error, backendStatus, isOnline]);

  // F1 Phase 7 Freshness Calculation for currently active station
  const activeStationFreshness: FreshnessStatus = useMemo(() => {
    if (isSourceUnavail) {
      return 'SOURCE_UNAVAILABLE';
    }
    if (!activeStation || !activeStation.observedAt || stations.length === 0) {
      return 'NO_DATA';
    }
    return calculateFreshnessStatus({
      observedAt: activeStation.observedAt,
      hasObservations: stations.length > 0,
      isSourceUnavailable: isSourceUnavail,
    });
  }, [activeStation, stations.length, isSourceUnavail]);

  // Fetch real historical telemetry for currently active station anchored to activeStation.observedAt
  useEffect(() => {
    if (!activeStation?.stationId || !activeStation.observedAt) {
      setHistoryReadings([]);
      setIsHistoryLoading(false);
      setHistoryError(null);
      return;
    }

    const historyReqId = ++activeHistoryReqIdRef.current;

    // Immediately clear stale readings and mark loading to avoid displaying previous station's data
    setHistoryReadings([]);
    setIsHistoryLoading(true);
    setHistoryError(null);

    const fetchHistory = async () => {
      if (typeof navigator !== 'undefined' && !navigator.onLine) {
        if (historyReqId === activeHistoryReqIdRef.current) {
          setHistoryError("You're offline. Check your network connection and retry.");
          setHistoryReadings([]);
          setIsHistoryLoading(false);
        }
        return;
      }

      try {
        const toDate = new Date(activeStation.observedAt);
        const fromDate = new Date(toDate.getTime() - 24 * 60 * 60 * 1000);
        const to = toDate.toISOString();
        const from = fromDate.toISOString();

        const historyRes = await airQualityApi.getStationHistory(activeStation.stationId, from, to);
        if (historyReqId === activeHistoryReqIdRef.current) {
          const observations = historyRes.observations || historyRes.readings || [];
          const sorted = [...observations].sort(
            (a, b) => new Date(a.observedAt).getTime() - new Date(b.observedAt).getTime()
          );
          setHistoryReadings(sorted);
          setHistoryError(null);
        }
      } catch (err: any) {
        if (historyReqId === activeHistoryReqIdRef.current) {
          const isNetErr = !navigator.onLine || err?.code === 'ERR_NETWORK';
          setHistoryError(
            isNetErr
              ? "You're offline. Check your network connection and retry."
              : err?.message || 'Failed to load station telemetry history'
          );
          setHistoryReadings([]);
        }
      } finally {
        if (historyReqId === activeHistoryReqIdRef.current) {
          setIsHistoryLoading(false);
        }
      }
    };

    fetchHistory();
  }, [activeStation?.stationId, activeStation?.observedAt]);

  // Manual retry handler for station history
  const retryHistory = React.useCallback(() => {
    if (!activeStation?.stationId || !activeStation.observedAt) return;
    const historyReqId = ++activeHistoryReqIdRef.current;

    setIsHistoryLoading(true);
    setHistoryError(null);
    setHistoryReadings([]);

    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setHistoryError("You're offline. Check your network connection and retry.");
      setIsHistoryLoading(false);
      return;
    }

    const toDate = new Date(activeStation.observedAt);
    const fromDate = new Date(toDate.getTime() - 24 * 60 * 60 * 1000);
    const to = toDate.toISOString();
    const from = fromDate.toISOString();

    airQualityApi
      .getStationHistory(activeStation.stationId, from, to)
      .then((historyRes) => {
        if (historyReqId === activeHistoryReqIdRef.current) {
          const observations = historyRes.observations || historyRes.readings || [];
          const sorted = [...observations].sort(
            (a, b) => new Date(a.observedAt).getTime() - new Date(b.observedAt).getTime()
          );
          setHistoryReadings(sorted);
          setHistoryError(null);
        }
      })
      .catch((err: any) => {
        if (historyReqId === activeHistoryReqIdRef.current) {
          const isNetErr = !navigator.onLine || err?.code === 'ERR_NETWORK';
          setHistoryError(
            isNetErr
              ? "You're offline. Check your network connection and retry."
              : err?.message || 'Failed to load station telemetry history'
          );
          setHistoryReadings([]);
        }
      })
      .finally(() => {
        if (historyReqId === activeHistoryReqIdRef.current) {
          setIsHistoryLoading(false);
        }
      });
  }, [activeStation?.stationId, activeStation?.observedAt]);

  // Filter stations based on search query
  const filteredStations = useMemo(() => {
    return stations.filter(
      (s) =>
        s.stationName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.stationId.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [stations, searchTerm]);

  // Real KPI calculations based strictly on returned observations
  const totalStations = stations.length;

  const averagePM25 = useMemo(() => {
    if (stations.length === 0) return null;
    const sum = stations.reduce((acc, curr) => acc + curr.pm25, 0);
    return Math.round(sum / stations.length);
  }, [stations]);

  const getPM25Category = (val: number | null) => {
    if (val === null) return 'No Telemetry';
    if (val <= 30) return 'Good (0-30 µg/m³)';
    if (val <= 60) return 'Moderate (31-60 µg/m³)';
    if (val <= 90) return 'Poor (61-90 µg/m³)';
    if (val <= 120) return 'Very Poor (91-120 µg/m³)';
    return 'Severe (>120 µg/m³)';
  };

  const formatTime = (iso?: string) => {
    if (!iso) return 'N/A';
    try {
      return new Date(iso).toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });
    } catch {
      return 'N/A';
    }
  };

  const formatReadingTime = (iso: string) => {
    try {
      return new Date(iso).toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return 'N/A';
    }
  };

  const formatRelativeTime = (isoString?: string) => {
    if (!isoString) return 'N/A';
    try {
      const diffMs = Date.now() - new Date(isoString).getTime();
      if (diffMs < 0) return 'Just now';
      const diffMins = Math.floor(diffMs / (1000 * 60));
      if (diffMins < 1) return 'Just now';
      if (diffMins === 1) return '1 min ago';
      if (diffMins < 60) return `${diffMins} min ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      return `${Math.floor(diffHours / 24)}d ago`;
    } catch {
      return 'Recently';
    }
  };

  // Real historical observation points for PM2.5 chart
  const stationTrend: TrendPoint[] = useMemo(() => {
    return historyReadings.map((r) => ({
      time: formatReadingTime(r.observedAt),
      pm25: r.pm25,
      observedAt: r.observedAt,
      source: r.source,
      quality: r.quality,
    }));
  }, [historyReadings]);

  // Loading and error states for city initialization
  if (isCitiesLoading || (!selectedCity && isLoading)) {
    return (
      <PageContainer title="CITY AIR QUALITY WORKBENCH">
        <Loading message="Loading operating cities and telemetry..." />
      </PageContainer>
    );
  }

  if (error && !selectedCity) {
    return (
      <PageContainer title="CITY AIR QUALITY WORKBENCH">
        <ErrorState
          title={!isOnline ? 'Network Offline' : 'Failed to Load Cities'}
          message={error}
          onRetry={fetchCities}
        />
      </PageContainer>
    );
  }

  if (!selectedCity) {
    return (
      <PageContainer title="CITY AIR QUALITY WORKBENCH">
        <EmptyState
          title="No Operating City Available"
          message="No operating cities were returned by the backend service."
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <span>CITY AIR QUALITY WORKBENCH</span>
          <Badge
            variant={getFreshnessBadgeVariant(activeStationFreshness)}
            pulse={activeStationFreshness === 'LIVE'}
          >
            {activeStationFreshness === 'LIVE'
              ? 'STREAM: LIVE'
              : activeStationFreshness === 'STALE'
              ? 'STREAM: STALE'
              : activeStationFreshness === 'SOURCE_UNAVAILABLE'
              ? 'STREAM: SOURCE UNAVAILABLE'
              : 'STREAM: NO DATA'}
          </Badge>
        </div>
      }
      subtitle={
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <span>Ground CAAQMS Station Density & Telemetry Quality ({selectedCity.name}, {selectedCity.state})</span>
          <span style={{ color: 'var(--text-muted)' }}>•</span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <Clock size={13} /> UI Refreshed: {formatTime(lastUpdated.toISOString())}
          </span>
        </div>
      }
      action={
        <Button variant="secondary" size="sm" onClick={refreshData} isLoading={isLoading}>
          <RefreshCw size={13} style={{ marginRight: '0.35rem' }} /> Refresh Telemetry
        </Button>
      }
    >
      {/* =========================================================================
          TOP KPIS: Average PM2.5 | Station Count | Active Nodes | Data Quality
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* KPI 1: City Average PM2.5 (strictly computed from real observations) */}
        <Card>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            City Average PM2.5
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2.2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', lineHeight: 1 }}>
              {averagePM25 !== null ? averagePM25 : '—'}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>µg/m³</span>
          </div>
          <div style={{ marginTop: '0.4rem', fontSize: '0.75rem', color: 'var(--aqi-poor)', fontWeight: 600 }}>
            {getPM25Category(averagePM25)}
          </div>
        </Card>

        {/* KPI 2: Total Ground Stations (strictly observations.length) */}
        <Card>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Total Ground Stations
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2.2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', lineHeight: 1 }}>
              {totalStations}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>CAAQMS</span>
          </div>
          <div style={{ marginTop: '0.4rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            CPCB & State PCB certified
          </div>
        </Card>

        {/* KPI 3: Active Online Nodes (N/A when status field not provided) */}
        <Card>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Active Online Nodes
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span style={{ fontSize: '2.2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-muted)', lineHeight: 1 }}>
              N/A
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>nodes</span>
          </div>
          <div style={{ marginTop: '0.4rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Status flag unavailable in API v1
          </div>
        </Card>

        {/* KPI 4: Data Freshness & Quality */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Data Freshness & Stream
            </div>
            <Badge variant={getFreshnessBadgeVariant(activeStationFreshness)} size="sm">
              {activeStationFreshness}
            </Badge>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', marginTop: '0.4rem' }}>
            <span
              style={{
                fontSize: '1.9rem',
                fontWeight: 800,
                fontFamily: 'var(--font-heading)',
                color:
                  activeStationFreshness === 'LIVE'
                    ? 'var(--aqi-good)'
                    : activeStationFreshness === 'STALE'
                    ? 'var(--accent-amber)'
                    : activeStationFreshness === 'SOURCE_UNAVAILABLE'
                    ? 'var(--accent-rose)'
                    : 'var(--text-muted)',
                lineHeight: 1,
              }}
            >
              {activeStationFreshness}
            </span>
          </div>
          <div style={{ marginTop: '0.45rem', fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
            {activeStation ? (
              <>
                <div>
                  <span>{activeStationFreshness === 'STALE' ? 'Last observed: ' : 'Observed: '}</span>
                  <strong style={{ color: 'var(--text-primary)' }}>
                    {formatReadingTime(activeStation.observedAt)}
                  </strong>{' '}
                  ({formatRelativeTime(activeStation.observedAt)})
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.1rem' }}>
                  <span>
                    Source: <strong style={{ color: 'var(--text-primary)' }}>{activeStation.source}</strong>
                  </span>
                  <span>
                    Quality:{' '}
                    <strong
                      style={{
                        color:
                          activeStation.quality === 'VALID'
                            ? 'var(--aqi-good)'
                            : activeStation.quality === 'SUSPECT'
                            ? 'var(--accent-amber)'
                            : 'var(--accent-rose)',
                      }}
                    >
                      {activeStation.quality}
                    </strong>
                  </span>
                </div>
              </>
            ) : (
              <div>No telemetry observations available</div>
            )}
          </div>
        </Card>
      </div>

      {/* =========================================================================
          MAIN SECTION: LOADING / ERROR / EMPTY or MAP + STATION LIST
          ========================================================================= */}
      {isLoading && stations.length === 0 ? (
        <Loading message={`Querying latest air quality telemetry for ${selectedCity.name}...`} />
      ) : error && stations.length === 0 ? (
        <ErrorState
          title={
            !isOnline
              ? 'Network Offline'
              : isSourceUnavail
              ? 'SOURCE_UNAVAILABLE: Telemetry Source Unreachable'
              : 'Air Quality Telemetry Error'
          }
          message={error}
          onRetry={refreshData}
        />
      ) : stations.length === 0 ? (
        <EmptyState
          title="NO_DATA: No Air Quality Observations"
          message={`No ground station air observations were returned for ${selectedCity.name} from /api/v1/cities/${selectedCity.id}/air-quality/latest.`}
        />
      ) : (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'minmax(0, 2.6fr) minmax(320px, 1fr)',
              gap: '1.25rem',
              alignItems: 'stretch',
            }}
          >
            {/* Large Center Map */}
            <div style={{ minHeight: '520px' }}>
              <PollutionMap
                center={
                  typeof activeStation?.latitude === 'number' && typeof activeStation?.longitude === 'number'
                    ? [activeStation.latitude, activeStation.longitude]
                    : typeof selectedCity.latitude === 'number' && typeof selectedCity.longitude === 'number'
                    ? [selectedCity.latitude, selectedCity.longitude]
                    : [18.5204, 73.8567]
                }
                zoom={selectedCity.defaultZoom}
                stations={stations}
                height="520px"
                onSelectStation={(st) => setSelectedStationId(st.stationId)}
              />
            </div>

            {/* Side: Real Station List */}
            <div
              style={{
                background: 'var(--bg-card)',
                border: '1px solid var(--border-medium)',
                borderRadius: '14px',
                padding: '1.25rem',
                display: 'flex',
                flexDirection: 'column',
                boxShadow: 'var(--shadow-md)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.85rem' }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Monitoring Stations ({filteredStations.length})
                </span>
                <Badge variant="neutral">Ground Network</Badge>
              </div>

              {/* Search Box */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.45rem 0.75rem',
                  borderRadius: '8px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-subtle)',
                  marginBottom: '0.85rem',
                }}
              >
                <Search size={14} color="var(--text-muted)" />
                <input
                  type="text"
                  placeholder="Search station name or ID..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--text-primary)',
                    fontSize: '0.8rem',
                    outline: 'none',
                    width: '100%',
                  }}
                />
              </div>

              {/* Scrollable Station List */}
              <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '420px', paddingRight: '0.2rem' }}>
                {filteredStations.map((st) => {
                  const isSelected = activeStation?.stationId === st.stationId;
                  const stFreshness = calculateFreshnessStatus({
                    observedAt: st.observedAt,
                    hasObservations: true,
                  });
                  return (
                    <div
                      key={st.stationId}
                      onClick={() => setSelectedStationId(st.stationId)}
                      style={{
                        padding: '0.75rem 0.85rem',
                        borderRadius: '10px',
                        background: isSelected ? 'var(--brand-surface)' : 'var(--bg-surface-elevated)',
                        border: isSelected ? '1px solid var(--brand-border)' : '1px solid var(--border-subtle)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', flexWrap: 'wrap' }}>
                            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: isSelected ? 'var(--brand-primary)' : 'var(--text-primary)' }}>
                              {st.stationName}
                            </div>
                            <Badge variant={getFreshnessBadgeVariant(stFreshness)} size="sm">
                              {stFreshness}
                            </Badge>
                          </div>
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                            ID: {st.stationId} • {st.source} • {formatRelativeTime(st.observedAt)}
                          </div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontSize: '0.95rem', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>
                            {st.pm25} <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>µg/m³</span>
                          </div>
                          <div style={{ fontSize: '0.68rem', color: st.quality === 'VALID' ? 'var(--aqi-good)' : 'var(--accent-amber)', fontWeight: 600 }}>
                            {st.quality}
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* =========================================================================
              BOTTOM: SELECTED STATION DETAILS + PM2.5 CHART
              ========================================================================= */}
          {activeStation && (
            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) minmax(0, 2fr)', gap: '1.25rem' }}>
              {/* Selected Station Detail Card */}
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <MapPin size={16} color="var(--brand-primary)" />
                    <span>Station Detail: {activeStation.stationName}</span>
                  </div>
                }
                subtitle="Telemetry stream & sensor health diagnostics"
                badge={
                  <div style={{ display: 'flex', gap: '0.45rem', alignItems: 'center' }}>
                    <Badge variant={getFreshnessBadgeVariant(activeStationFreshness)}>
                      STATUS: {activeStationFreshness}
                    </Badge>
                    <Badge
                      variant={
                        activeStation.quality === 'VALID'
                          ? 'success'
                          : activeStation.quality === 'SUSPECT'
                          ? 'warning'
                          : 'danger'
                      }
                    >
                      QUALITY: {activeStation.quality}
                    </Badge>
                  </div>
                }
              >
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr',
                    columnGap: '1rem',
                    rowGap: '0.75rem',
                    fontSize: '0.85rem',
                    padding: '0.85rem 0',
                  }}
                >
                  <span style={{ color: 'var(--text-muted)' }}>Station Code</span>
                  <strong style={{ textAlign: 'right', color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                    {activeStation.stationId}
                  </strong>

                  <span style={{ color: 'var(--text-muted)' }}>PM2.5 Concentration</span>
                  <strong style={{ textAlign: 'right', color: 'var(--aqi-poor)', fontSize: '1.05rem' }}>
                    {activeStation.pm25} µg/m³
                  </strong>

                  <span style={{ color: 'var(--text-muted)' }}>Freshness Status</span>
                  <span style={{ textAlign: 'right' }}>
                    <Badge variant={getFreshnessBadgeVariant(activeStationFreshness)} size="sm">
                      {activeStationFreshness}
                    </Badge>
                  </span>

                  <span style={{ color: 'var(--text-muted)' }}>Source Authority</span>
                  <span style={{ textAlign: 'right', color: 'var(--text-primary)', fontWeight: 600 }}>
                    {activeStation.source}
                  </span>

                  <span style={{ color: 'var(--text-muted)' }}>
                    {activeStationFreshness === 'STALE' ? 'Last Observed' : 'Observed Timestamp'}
                  </span>
                  <span style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                    {formatObservationTimestamp(activeStation.observedAt)}
                  </span>

                  <span style={{ color: 'var(--text-muted)' }}>Data Quality Flag</span>
                  <span
                    style={{
                      textAlign: 'right',
                      color:
                        activeStation.quality === 'VALID'
                          ? 'var(--aqi-good)'
                          : activeStation.quality === 'SUSPECT'
                          ? 'var(--accent-amber)'
                          : 'var(--accent-rose)',
                      fontWeight: 700,
                    }}
                  >
                    {activeStation.quality}
                  </span>

                  <span style={{ color: 'var(--text-muted)' }}>Coordinates</span>
                  <span style={{ textAlign: 'right', color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                    {typeof activeStation.latitude === 'number' && typeof activeStation.longitude === 'number'
                      ? `${activeStation.latitude.toFixed(4)}° N, ${activeStation.longitude.toFixed(4)}° E`
                      : 'Coordinates unavailable'}
                  </span>
                </div>
              </Card>

              {/* Trend Chart Card */}
              <Card
                title={`24-Hour PM2.5 Profile — ${activeStation.stationName}`}
                subtitle={`Observed sensor reading history (${historyReadings.length} readings)`}
              >
                <PM25Chart
                  data={stationTrend}
                  currentPm25={activeStation.pm25}
                  isLoading={isHistoryLoading}
                  error={historyError}
                  onRetry={retryHistory}
                />
              </Card>
            </div>
          )}
        </>
      )}
    </PageContainer>
  );
};
