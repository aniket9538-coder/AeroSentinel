import React, { useState } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ReferenceLine,
} from 'recharts';
import { useApp } from '../../store/AppContext';
import { Inbox, AlertCircle, RefreshCw } from 'lucide-react';

export interface TrendPoint {
  time: string;
  pm25: number;
  observedAt?: string;
  source?: string;
  quality?: string;
  stationAvg?: number;
}

interface PM25ChartProps {
  data?: TrendPoint[];
  currentPm25?: number;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

export const PM25Chart: React.FC<PM25ChartProps> = ({
  data = [],
  currentPm25,
  isLoading = false,
  error = null,
  onRetry,
}) => {
  const { theme } = useApp();
  const [timeRange, setTimeRange] = useState<'24H' | '12H' | '6H' | '1H'>('24H');

  // Filter data according to selected timeframe
  const getFilteredData = () => {
    switch (timeRange) {
      case '1H':
        return data.slice(-2);
      case '6H':
        return data.slice(-4);
      case '12H':
        return data.slice(-7);
      case '24H':
      default:
        return data;
    }
  };

  const filteredData = getFilteredData();

  const isDark = theme === 'dark';
  const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(15, 23, 42, 0.06)';
  const textColor = isDark ? '#94a3b8' : '#64748b';
  const tooltipBg = isDark ? '#111d35' : '#ffffff';
  const tooltipBorder = isDark ? 'rgba(255, 255, 255, 0.15)' : '#cbd5e1';

  return (
    <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
      {/* Time Range Selector */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>
            RANGE:
          </span>
          {(['24H', '12H', '6H', '1H'] as const).map((range) => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              style={{
                padding: '0.2rem 0.55rem',
                borderRadius: '6px',
                fontSize: '0.725rem',
                fontWeight: 600,
                cursor: 'pointer',
                border:
                  timeRange === range
                    ? '1px solid var(--brand-border)'
                    : '1px solid var(--border-subtle)',
                background:
                  timeRange === range ? 'var(--brand-surface)' : 'transparent',
                color:
                  timeRange === range ? 'var(--brand-primary)' : 'var(--text-muted)',
                transition: 'all 0.15s ease',
              }}
            >
              {range}
            </button>
          ))}
        </div>

        {currentPm25 !== undefined && (
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Current Point:{' '}
            <strong style={{ color: 'var(--text-primary)' }}>{currentPm25} µg/m³</strong>
          </div>
        )}
      </div>

      {/* Chart Canvas or Loading / Error / Empty States */}
      <div style={{ width: '100%', height: '260px' }}>
        {isLoading ? (
          <div
            style={{
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.6rem',
              color: 'var(--text-muted)',
              fontSize: '0.85rem',
            }}
          >
            <RefreshCw size={22} style={{ animation: 'spin 1s linear infinite' }} />
            <span>Loading historical telemetry observations...</span>
          </div>
        ) : error ? (
          <div
            style={{
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.6rem',
              color: 'var(--accent-rose)',
              fontSize: '0.85rem',
              padding: '1rem',
              textAlign: 'center',
            }}
          >
            <AlertCircle size={22} />
            <span>Failed to load telemetry history: {error}</span>
            {onRetry && (
              <button
                type="button"
                onClick={onRetry}
                style={{
                  marginTop: '0.35rem',
                  padding: '0.3rem 0.75rem',
                  borderRadius: '6px',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  border: '1px solid var(--accent-rose)',
                  background: 'rgba(244, 63, 94, 0.1)',
                  color: 'var(--accent-rose)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                }}
              >
                <RefreshCw size={12} /> Retry History
              </button>
            )}
          </div>
        ) : filteredData.length === 0 ? (
          <div
            style={{
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.6rem',
              color: 'var(--text-muted)',
              fontSize: '0.85rem',
              padding: '1rem',
              textAlign: 'center',
            }}
          >
            <Inbox size={24} />
            <span>No historical observations recorded for this station and timeframe.</span>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={filteredData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="pm25Gradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="var(--brand-primary)" stopOpacity={0.5} />
                  <stop offset="95%" stopColor="var(--brand-primary)" stopOpacity={0.0} />
                </linearGradient>
              </defs>

              <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />

              <XAxis
                dataKey="time"
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={{ stroke: gridColor }}
              />

              <YAxis
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={false}
                domain={[0, 'dataMax + 20']}
              />

              <Tooltip
                contentStyle={{
                  backgroundColor: tooltipBg,
                  borderColor: tooltipBorder,
                  borderRadius: '10px',
                  fontSize: '12px',
                  color: 'var(--text-primary)',
                  boxShadow: 'var(--shadow-md)',
                  padding: '8px 12px',
                }}
                formatter={(value: any) => [`${value} µg/m³`, 'PM2.5']}
                labelFormatter={(label) => `Time: ${label}`}
              />

              <ReferenceLine
                y={60}
                stroke="#f59e0b"
                strokeDasharray="4 4"
                label={{
                  value: 'Standard (60 µg/m³)',
                  fill: '#f59e0b',
                  fontSize: 10,
                  position: 'right',
                }}
              />

              <Area
                type="monotone"
                dataKey="pm25"
                stroke="var(--brand-primary)"
                strokeWidth={2.5}
                fillOpacity={1}
                fill="url(#pm25Gradient)"
                activeDot={{ r: 6, fill: 'var(--brand-primary)', stroke: '#ffffff', strokeWidth: 2 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
};
