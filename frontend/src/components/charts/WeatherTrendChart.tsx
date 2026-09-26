import React, { useState, useMemo } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { GridWeatherObservationResponse } from '../../types/grid';
import { useApp } from '../../store/AppContext';
import { CloudSun, Droplets, Wind, RefreshCw, AlertCircle, Inbox, Clock } from 'lucide-react';

interface WeatherTrendChartProps {
  weatherObservations?: GridWeatherObservationResponse[];
  h3Index?: string | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

type WeatherMetric = 'temperature' | 'humidity' | 'windSpeed';

export const WeatherTrendChart: React.FC<WeatherTrendChartProps> = ({
  weatherObservations = [],
  h3Index,
  isLoading = false,
  error = null,
  onRetry,
}) => {
  const { theme } = useApp();
  const [selectedMetric, setSelectedMetric] = useState<WeatherMetric>('temperature');

  const isDark = theme === 'dark';
  const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.06)';
  const textColor = isDark ? '#94a3b8' : '#64748b';

  // Format real observation timestamps for charting
  const chartData = useMemo(() => {
    return weatherObservations.map((obs) => {
      let timeLabel = '';
      try {
        const d = new Date(obs.observedAt);
        timeLabel = d.toLocaleTimeString('en-US', {
          hour: 'numeric',
          minute: '2-digit',
          hour12: true,
        });
      } catch {
        timeLabel = obs.observedAt;
      }

      return {
        time: timeLabel,
        rawTimestamp: obs.observedAt,
        temperature: obs.temperature,
        humidity: obs.humidity,
        windSpeed: obs.windSpeed,
        windDirection: obs.windDirection,
        rainfall: obs.rainfall,
        source: obs.source,
      };
    });
  }, [weatherObservations]);

  // Metric configuration
  const metricConfig = {
    temperature: {
      label: 'Temperature',
      unit: '°C',
      color: '#f59e0b',
      fillStart: 'rgba(245, 158, 11, 0.35)',
      fillEnd: 'rgba(245, 158, 11, 0.02)',
      icon: CloudSun,
    },
    humidity: {
      label: 'Humidity',
      unit: '%',
      color: '#0ea5e9',
      fillStart: 'rgba(14, 165, 233, 0.35)',
      fillEnd: 'rgba(14, 165, 233, 0.02)',
      icon: Droplets,
    },
    windSpeed: {
      label: 'Wind Speed',
      unit: 'km/h',
      color: '#10b981',
      fillStart: 'rgba(16, 185, 129, 0.35)',
      fillEnd: 'rgba(16, 185, 129, 0.02)',
      icon: Wind,
    },
  };

  const currentCfg = metricConfig[selectedMetric];

  // Custom polished Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div
          style={{
            background: 'var(--bg-card)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '0.65rem 0.85rem',
            boxShadow: 'var(--shadow-lg)',
            fontSize: '0.75rem',
            minWidth: '160px',
          }}
        >
          <div
            style={{
              fontWeight: 600,
              color: 'var(--text-primary)',
              marginBottom: '0.35rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.3rem',
            }}
          >
            <Clock size={12} color="var(--text-muted)" />
            <span>{data.time}</span>
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'baseline',
              marginBottom: '0.2rem',
            }}
          >
            <span style={{ color: 'var(--text-secondary)' }}>{currentCfg.label}:</span>
            <span style={{ fontWeight: 700, color: currentCfg.color, fontSize: '0.9rem' }}>
              {data[selectedMetric] !== null ? `${data[selectedMetric]} ${currentCfg.unit}` : '—'}
            </span>
          </div>
          {data.rainfall !== null && data.rainfall > 0 && (
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                color: 'var(--text-muted)',
                fontSize: '0.7rem',
              }}
            >
              <span>Rain:</span>
              <span>{data.rainfall} mm</span>
            </div>
          )}
          <div
            style={{
              marginTop: '0.35rem',
              paddingTop: '0.25rem',
              borderTop: '1px solid var(--border-subtle)',
              fontSize: '0.65rem',
              color: 'var(--text-muted)',
              display: 'flex',
              justifyContent: 'space-between',
            }}
          >
            <span>Source:</span>
            <span style={{ fontWeight: 600 }}>{data.source || 'OPEN_METEO'}</span>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div
      style={{
        background: 'var(--bg-surface)',
        borderRadius: '12px',
        padding: '1.25rem',
        border: '1px solid var(--border-subtle)',
        boxShadow: 'var(--shadow-sm)',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
      }}
    >
      {/* Header with Title and Metric Buttons */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.75rem',
          marginBottom: '1rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h3
              style={{
                margin: 0,
                fontSize: '0.95rem',
                fontWeight: 700,
                color: 'var(--text-primary)',
                letterSpacing: '0.01em',
              }}
            >
              WEATHER TREND (H3 CELL)
            </h3>
            {h3Index && (
              <span
                style={{
                  fontSize: '0.675rem',
                  fontFamily: 'monospace',
                  background: 'var(--brand-surface)',
                  color: 'var(--brand-primary)',
                  border: '1px solid var(--brand-border)',
                  padding: '0.1rem 0.4rem',
                  borderRadius: '4px',
                }}
              >
                {h3Index}
              </span>
            )}
          </div>
          <p
            style={{
              margin: '0.2rem 0 0 0',
              fontSize: '0.725rem',
              color: 'var(--text-muted)',
            }}
          >
            Authoritative Open-Meteo physical hourly observations
          </p>
        </div>

        {/* Metric Selector Tabs */}
        <div
          style={{
            display: 'flex',
            background: 'var(--bg-card)',
            padding: '0.2rem',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
            gap: '0.2rem',
          }}
        >
          {(['temperature', 'humidity', 'windSpeed'] as WeatherMetric[]).map((metric) => {
            const isSelected = selectedMetric === metric;
            const cfg = metricConfig[metric];
            const Icon = cfg.icon;
            return (
              <button
                key={metric}
                onClick={() => setSelectedMetric(metric)}
                style={{
                  padding: '0.3rem 0.65rem',
                  fontSize: '0.725rem',
                  fontWeight: isSelected ? 700 : 500,
                  borderRadius: '6px',
                  border: 'none',
                  cursor: 'pointer',
                  background: isSelected ? cfg.color : 'transparent',
                  color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  transition: 'all 0.15s ease',
                }}
              >
                <Icon size={13} />
                <span>{cfg.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Chart Canvas Area */}
      <div style={{ flex: 1, minHeight: '220px', width: '100%', position: 'relative' }}>
        {/* Loading State */}
        {isLoading && (
          <div
            style={{
              height: '100%',
              minHeight: '220px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              color: 'var(--text-muted)',
            }}
          >
            <RefreshCw size={24} className="animate-spin" />
            <span style={{ fontSize: '0.8rem' }}>Loading physical weather series...</span>
          </div>
        )}

        {/* Error State */}
        {!isLoading && error && (
          <div
            style={{
              height: '100%',
              minHeight: '220px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.6rem',
              color: 'var(--status-danger)',
              textAlign: 'center',
              padding: '1rem',
            }}
          >
            <AlertCircle size={28} />
            <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{error}</div>
            {onRetry && (
              <button
                onClick={onRetry}
                style={{
                  padding: '0.35rem 0.85rem',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  borderRadius: '6px',
                  background: 'var(--brand-surface)',
                  color: 'var(--brand-primary)',
                  border: '1px solid var(--brand-border)',
                  cursor: 'pointer',
                }}
              >
                Retry
              </button>
            )}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && chartData.length === 0 && (
          <div
            style={{
              height: '100%',
              minHeight: '220px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              color: 'var(--text-muted)',
              textAlign: 'center',
            }}
          >
            <Inbox size={32} strokeWidth={1.5} />
            <div style={{ fontSize: '0.85rem', fontWeight: 500 }}>
              {h3Index ? 'No weather telemetry recorded for this cell' : 'Select an H3 cell on the map to inspect trend'}
            </div>
          </div>
        )}

        {/* Real Chart Canvas */}
        {!isLoading && !error && chartData.length > 0 && (
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={chartData} margin={{ top: 10, right: 12, left: -15, bottom: 0 }}>
              <defs>
                <linearGradient id={`gradient-${selectedMetric}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={currentCfg.color} stopOpacity={0.4} />
                  <stop offset="95%" stopColor={currentCfg.color} stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
              <XAxis
                dataKey="time"
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={{ stroke: gridColor }}
                dy={6}
                interval="preserveStartEnd"
              />
              <YAxis
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={false}
                unit={selectedMetric === 'humidity' ? '%' : ''}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                type="monotone"
                dataKey={selectedMetric}
                stroke={currentCfg.color}
                strokeWidth={2.2}
                fillOpacity={1}
                fill={`url(#gradient-${selectedMetric})`}
                dot={false}
                activeDot={{ r: 5, fill: currentCfg.color, stroke: 'var(--bg-surface)', strokeWidth: 2 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
};

export default WeatherTrendChart;
