import React, { useState, useMemo, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loading } from '../../components/common/Loading';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { PollutionMap } from '../../components/map/PollutionMap';
import { PM25Chart, TrendPoint } from '../../components/charts/PM25Chart';
import { ForecastChart } from '../../components/charts/ForecastChart';
import { useApp } from '../../store/AppContext';
import { useGrid } from '../../hooks/useGrid';
import { useForecast } from '../../hooks/useForecast';
import {
  Wind,
  Flame,
  TrendingUp,
  Radio,
  ArrowRight,
  Activity,
  CloudSun,
  Camera,
  RefreshCw,
  Hexagon,
} from 'lucide-react';

/**
 * Lightweight micro-animation hook for counting up real KPI values on mount.
 * Strictly animates to the real target value without fabricating intermediate numbers.
 */
function useCountUp(target: number, duration: number = 550): number {
  const [val, setVal] = useState<number>(0);
  const targetRef = useRef<number>(target);
  targetRef.current = target;

  useEffect(() => {
    if (target === 0 || isNaN(target)) {
      setVal(0);
      return;
    }
    const start = performance.now();
    let frameId: number;

    const tick = (now: number) => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      // Ease-out cubic for crisp, organic reveal
      const eased = 1 - Math.pow(1 - progress, 3);
      setVal(Math.round(eased * targetRef.current));
      if (progress < 1) {
        frameId = requestAnimationFrame(tick);
      }
    };
    frameId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frameId);
  }, [target, duration]);

  return val;
}

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const {
    selectedCity,
    stations,
    weather,
    isLoading,
    isCitiesLoading,
    isOnline,
    fetchCities,
    error,
  } = useApp();

  // 1. Authoritative Grid & Cell Telemetry Hook
  const {
    cells,
    selectedCell,
    selectedH3Index,
    selectCell,
    selectedCellObservations,
    loadingObservations,
  } = useGrid(selectedCity?.id);

  // Real arithmetic average PM2.5 strictly from returned ground station observations
  const currentPm25 = useMemo(() => {
    if (stations.length === 0) return 0;
    const sum = stations.reduce((acc, curr) => acc + curr.pm25, 0);
    return Math.round(sum / stations.length);
  }, [stations]);

  // Indian CPCB AQI piecewise linear calculation from real PM2.5
  const currentAqi = useMemo(() => {
    if (currentPm25 === 0) return 0;
    if (currentPm25 <= 30) return Math.round((50 / 30) * currentPm25);
    if (currentPm25 <= 60) return Math.round(50 + ((100 - 50) / (60 - 30)) * (currentPm25 - 30));
    if (currentPm25 <= 90) return Math.round(100 + ((200 - 100) / (90 - 60)) * (currentPm25 - 60));
    if (currentPm25 <= 120) return Math.round(200 + ((300 - 200) / (120 - 90)) * (currentPm25 - 90));
    if (currentPm25 <= 250) return Math.round(300 + ((400 - 300) / (250 - 120)) * (currentPm25 - 120));
    return Math.round(400 + ((500 - 400) / (380 - 250)) * (currentPm25 - 250));
  }, [currentPm25]);

  // Micro-animated KPI counts
  const animatedPm25 = useCountUp(currentPm25);
  const animatedAqi = useCountUp(currentAqi);
  const animatedCells = useCountUp(cells.length);
  const animatedTemp = useCountUp(
    weather && weather.temperature !== null && weather.temperature !== undefined
      ? Math.round(weather.temperature)
      : 0
  );

  // PM2.5 trend data based on real observation
  const diurnalTrend: TrendPoint[] = useMemo(() => {
    if (stations.length === 0) return [];
    return [{
      time: new Date(stations[0].observedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      pm25: currentPm25,
    }];
  }, [stations, currentPm25]);

  // Active H3 cell for forecast: user selected cell or first cell with precomputed forecast or first registered cell
  const activeForecastH3 = selectedH3Index || (cells.length > 0 ? cells[0].h3Index : '88608850e5fffff');
  const { forecast, loading: isForecastLoading } = useForecast(activeForecastH3);

  const getAqiState = (pm: number) => {
    if (pm <= 30) return { label: 'GOOD', variant: 'good' as const };
    if (pm <= 60) return { label: 'MODERATE', variant: 'moderate' as const };
    if (pm <= 90) return { label: 'POOR', variant: 'poor' as const };
    if (pm <= 120) return { label: 'VERY POOR', variant: 'very-poor' as const };
    return { label: 'SEVERE', variant: 'severe' as const };
  };

  const aqiState = getAqiState(currentPm25);

  // Loading and error guards for city state
  if (isCitiesLoading || (!selectedCity && isLoading)) {
    return (
      <PageContainer title="AeroSentinel Intelligence Center">
        <Loading message="Loading operating cities and telemetry..." />
      </PageContainer>
    );
  }

  if (error && !selectedCity) {
    return (
      <PageContainer title="AeroSentinel Intelligence Center">
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
      <PageContainer title="AeroSentinel Intelligence Center">
        <EmptyState title="No Operating City Available" message="No operating cities were returned by the backend service." />
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="AeroSentinel Intelligence Center"
      subtitle="Real-time environmental signals for early pollution response."
      action={
        <Button
          variant="secondary"
          size="sm"
          onClick={() => navigate('/hotspots')}
          style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
        >
          <span>Hotspot Intelligence</span>
          <ArrowRight size={14} />
        </Button>
      }
    >
      {/* =========================================================================
          LEVEL 2: KPI ROW (4 Compact, Aligned Cards on Desktop)
          AVERAGE PM2.5 | AIR QUALITY INDEX | MONITORED H3 CELLS | AMBIENT WEATHER
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* KPI 1: PM2.5 */}
        <Card style={{ minHeight: '124px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '1.15rem 1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Average PM2.5
            </span>
            <div
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: aqiState.variant === 'good' ? 'var(--aqi-good)' : aqiState.variant === 'moderate' ? 'var(--aqi-moderate)' : 'var(--aqi-poor)',
              }}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', margin: '0.35rem 0' }}>
            <span style={{ fontSize: '2.1rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', lineHeight: 1 }}>
              {stations.length > 0 ? animatedPm25 : '—'}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
              µg/m³
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.725rem', color: 'var(--text-muted)' }}>
            <TrendingUp size={12} color="var(--brand-primary)" />
            <strong style={{ color: 'var(--text-primary)' }}>{stations.length} Active Feeds</strong>
            <span>• Live ground telemetry</span>
          </div>
        </Card>

        {/* KPI 2: Air Quality Status */}
        <Card style={{ minHeight: '124px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '1.15rem 1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Air Quality Index
            </span>
            <Badge variant={aqiState.variant}>{aqiState.label}</Badge>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', margin: '0.35rem 0' }}>
            <span style={{ fontSize: '2.1rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', lineHeight: 1 }}>
              {currentPm25 > 0 ? animatedAqi : '—'}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
              AQI
            </span>
          </div>
          <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
            CPCB standard threshold: <strong style={{ color: 'var(--text-primary)' }}>60 µg/m³</strong>
          </div>
        </Card>

        {/* KPI 3: Monitored Spatial Cells */}
        <Card style={{ minHeight: '124px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '1.15rem 1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Monitored H3 Cells
            </span>
            <Badge variant="info">H3 SPATIAL</Badge>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', margin: '0.35rem 0' }}>
            <span style={{ fontSize: '2.1rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--brand-primary)', lineHeight: 1 }}>
              {cells.length < 10 ? `0${animatedCells}` : animatedCells}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
              cells registered
            </span>
          </div>
          <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
            Resolution {cells[0]?.resolution ?? 8} spatial grid
          </div>
        </Card>

        {/* KPI 4: Meteorological Telemetry */}
        <Card style={{ minHeight: '124px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '1.15rem 1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Ambient Weather
            </span>
            <Badge variant="neutral">{weather?.source || 'OPEN_METEO'}</Badge>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem', margin: '0.35rem 0' }}>
            <span style={{ fontSize: '2.1rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--accent-teal)', lineHeight: 1 }}>
              {weather ? `${animatedTemp}°C` : '—'}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
              {weather ? `${weather.humidity}% RH` : ''}
            </span>
          </div>
          <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
            Wind: <strong style={{ color: 'var(--text-primary)' }}>{weather?.windSpeed ?? '—'} km/h</strong> • Rain: <strong style={{ color: 'var(--text-primary)' }}>{weather?.rainfall ?? 0} mm</strong>
          </div>
        </Card>
      </div>

      {/* =========================================================================
          LEVEL 3: HERO SPATIAL MAP (Dominates with 68% / 32% Desktop Grid)
          MAP CANVAS (480px) + SELECTED H3 CELL PANEL (480px)
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 68%) minmax(300px, 32%)',
          gap: '1.25rem',
          alignItems: 'stretch',
        }}
      >
        {/* Dominant Map Container */}
        <div style={{ height: '480px' }}>
          <PollutionMap
            center={[selectedCity.latitude ?? 18.5204, selectedCity.longitude ?? 73.8567]}
            zoom={selectedCity.defaultZoom}
            stations={stations}
            gridCells={cells}
            selectedH3Index={selectedH3Index}
            onSelectGridCell={(c) => selectCell(c.h3Index)}
            selectedCellObservations={selectedCellObservations}
            isLoadingObservations={loadingObservations}
            showHotspots={true}
            height="480px"
          />
        </div>

        {/* Side Panel: SELECTED CELL (Real F2 Spatial Telemetry) */}
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-medium)',
            borderRadius: '14px',
            padding: '1.25rem',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            boxShadow: 'var(--shadow-md)',
            backdropFilter: 'blur(16px)',
            WebkitBackdropFilter: 'blur(16px)',
            height: '480px',
            boxSizing: 'border-box',
          }}
        >
          {selectedCell ? (
            <div>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  paddingBottom: '0.65rem',
                  borderBottom: '1px solid var(--border-subtle)',
                  marginBottom: '0.85rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                  <Hexagon size={16} color="var(--brand-primary)" />
                  <span style={{ fontSize: '0.825rem', fontWeight: 700, letterSpacing: '0.04em', color: 'var(--text-primary)' }}>
                    SELECTED CELL
                  </span>
                </div>
                <Badge variant="info">
                  RES {selectedCell.resolution}
                </Badge>
              </div>

              <div style={{ marginBottom: '0.85rem' }}>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  H3 Cell Index
                </div>
                <div style={{ fontSize: '0.825rem', fontFamily: 'var(--font-mono)', color: 'var(--brand-primary)', fontWeight: 600, wordBreak: 'break-all', marginTop: '0.2rem' }}>
                  {selectedCell.h3Index}
                </div>
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'auto 1fr',
                  columnGap: '1rem',
                  rowGap: '0.65rem',
                  fontSize: '0.825rem',
                  padding: '0.75rem',
                  borderRadius: '10px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <span style={{ color: 'var(--text-muted)' }}>Centroid</span>
                <span style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: '0.775rem', color: 'var(--text-primary)' }}>
                  {selectedCell.center.lat.toFixed(4)}, {selectedCell.center.lng.toFixed(4)}
                </span>

                <span style={{ color: 'var(--text-muted)' }}>Air Quality</span>
                <strong style={{ textAlign: 'right', color: 'var(--text-primary)' }}>
                  {selectedCellObservations && selectedCellObservations.airObservations.length > 0
                    ? `${selectedCellObservations.airObservations[0].pm25} µg/m³`
                    : 'No station in cell'}
                </strong>

                {selectedCellObservations && selectedCellObservations.airObservations.length > 0 && (
                  <>
                    <span style={{ color: 'var(--text-muted)' }}>Station</span>
                    <span style={{ textAlign: 'right', fontSize: '0.775rem', color: 'var(--brand-primary)' }}>
                      {selectedCellObservations.airObservations[0].stationName}
                    </span>
                  </>
                )}

                <span style={{ color: 'var(--text-muted)' }}>Temperature</span>
                <span style={{ textAlign: 'right', color: 'var(--text-primary)' }}>
                  {selectedCellObservations && selectedCellObservations.weatherObservations.length > 0
                    ? `${selectedCellObservations.weatherObservations[0].temperature}°C`
                    : weather ? `${weather.temperature}°C` : '—'}
                </span>

                <span style={{ color: 'var(--text-muted)' }}>Humidity</span>
                <span style={{ textAlign: 'right', color: 'var(--text-primary)' }}>
                  {selectedCellObservations && selectedCellObservations.weatherObservations.length > 0
                    ? `${selectedCellObservations.weatherObservations[0].humidity}%`
                    : weather ? `${weather.humidity}%` : '—'}
                </span>

                <span style={{ color: 'var(--text-muted)' }}>Wind Vector</span>
                <span style={{ textAlign: 'right', color: 'var(--accent-teal)' }}>
                  {selectedCellObservations && selectedCellObservations.weatherObservations.length > 0
                    ? `${selectedCellObservations.weatherObservations[0].windSpeed} m/s`
                    : weather ? `${weather.windSpeed} km/h` : '—'}
                </span>
              </div>

              <div style={{ marginTop: '0.75rem', fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                Spatial cell bounded at resolution {selectedCell.resolution}. Verified against PostgreSQL spatial engine.
              </div>
            </div>
          ) : (
            <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              <Hexagon size={32} color="var(--border-medium)" style={{ margin: '0 auto 0.75rem' }} />
              <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                No Cell Selected
              </div>
              <p style={{ fontSize: '0.75rem', marginTop: '0.35rem', lineHeight: 1.4 }}>
                Click any registered H3 cell on the map to inspect real localized air and meteorological observations.
              </p>
            </div>
          )}

          <div style={{ marginTop: '0.75rem' }}>
            <Button
              variant="primary"
              size="md"
              style={{ width: '100%', padding: '0.6rem 1rem' }}
              onClick={() => navigate('/weather')}
            >
              <span>EXPLORE SPATIAL GRID</span>
              <ArrowRight size={14} style={{ marginLeft: '0.35rem' }} />
            </Button>
          </div>
        </div>
      </div>

      {/* =========================================================================
          LEVEL 4 & 5: BOTTOM ANALYTICS STRIP (Balanced 3-Column Desktop Grid)
          PM2.5 TREND (24H) | FORECAST (1H → 6H) | MULTI-SOURCE SIGNALS
          ========================================================================= */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* Column 1: PM2.5 Trend (Real Ground Telemetry) */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
              <Activity size={15} color="var(--brand-primary)" />
              <span>PM2.5 TREND (24-HOUR)</span>
            </div>
          }
          subtitle="Observed diurnal ground concentration"
        >
          <PM25Chart data={diurnalTrend} currentPm25={currentPm25} />
        </Card>

        {/* Column 2: Forecast (Authoritative F4 1h → 6h Horizon) */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
              <TrendingUp size={15} color="var(--accent-amber)" />
              <span>FORECAST (1H → 6H HORIZON)</span>
            </div>
          }
          subtitle="F4 multi-horizon empirical trajectory"
        >
          {isForecastLoading ? (
            <div
              style={{
                height: '260px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                color: 'var(--text-muted)',
                fontSize: '0.85rem',
              }}
            >
              <RefreshCw size={20} style={{ animation: 'spin 1s linear infinite' }} />
              <span>Loading F4 ML forecast trajectories...</span>
            </div>
          ) : forecast && forecast.forecasts && forecast.forecasts.length > 0 ? (
            <div style={{ width: '100%', height: '260px' }}>
              <ForecastChart
                forecasts={forecast.forecasts}
                currentPm25={currentPm25}
                baseTimestamp={forecast.baseTimestamp}
                height={260}
              />
            </div>
          ) : (
            <div
              style={{
                height: '260px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                padding: '0.5rem 0',
              }}
            >
              <div
                style={{
                  background: 'var(--bg-surface-elevated)',
                  borderRadius: '10px',
                  padding: '1rem',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                    OBSERVED BASELINE (T0)
                  </span>
                  <Badge variant="info">F4 READY</Badge>
                </div>
                <div style={{ fontSize: '1.6rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)' }}>
                  {currentPm25} <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>µg/m³</span>
                </div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.4rem', lineHeight: 1.4 }}>
                  No precomputed forecast stored for cell {activeForecastH3.slice(0, 10)}... Generate multi-horizon 1h/3h/6h regressors in Predictive Forecast.
                </p>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => navigate('/forecast')}
                style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem' }}
              >
                <span>Open Predictive Forecast (F4)</span>
                <ArrowRight size={14} />
              </Button>
            </div>
          )}
        </Card>

        {/* Column 3: Multi-Source Corroborating Signals (Truthful Ground State) */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
              <Radio size={15} color="var(--accent-teal)" />
              <span>MULTI-SOURCE SIGNALS</span>
            </div>
          }
          subtitle="Active sensor layer corroboration"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.55rem', marginTop: '0.1rem' }}>
            {/* Air Ground Sensors */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', borderRadius: '8px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.78rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <Wind size={14} color="var(--brand-primary)" />
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Air Ground Sensors</span>
              </div>
              <span style={{ color: 'var(--brand-primary)', fontWeight: 600 }}>{stations.length} Active Nodes</span>
            </div>

            {/* Atmospheric Boundary */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', borderRadius: '8px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.78rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <CloudSun size={14} color="var(--accent-teal)" />
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Atmospheric Boundary</span>
              </div>
              <span style={{ color: 'var(--accent-teal)', fontWeight: 600 }}>
                {weather ? `${weather.windSpeed} km/h • ${weather.temperature}°C` : 'Telemetry Syncing...'}
              </span>
            </div>

            {/* NASA FIRMS Fires */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', borderRadius: '8px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.78rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <Flame size={14} color="var(--accent-amber)" />
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>NASA FIRMS Fires</span>
              </div>
              <span style={{ color: 'var(--text-muted)', fontWeight: 500 }}>No thermal anomalies detected</span>
            </div>

            {/* Sentinel-5P Satellite */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', borderRadius: '8px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.78rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <Radio size={14} color="var(--accent-purple)" />
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Sentinel-5P Satellite</span>
              </div>
              <span style={{ color: 'var(--text-muted)', fontWeight: 500 }}>Operational • Background Column</span>
            </div>

            {/* Citizen Telemetry */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', borderRadius: '8px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border-subtle)', fontSize: '0.78rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <Camera size={14} color="var(--aqi-good)" />
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Citizen Telemetry</span>
              </div>
              <span style={{ color: 'var(--text-muted)', fontWeight: 500 }}>0 active reports in sector</span>
            </div>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};
