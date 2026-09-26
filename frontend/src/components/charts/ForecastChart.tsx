import React from 'react';
import {
  ResponsiveContainer,
  ComposedChart,
  Line,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ReferenceLine,
} from 'recharts';
import { HourlyForecast } from '../../types';
import { useApp } from '../../store/AppContext';

export interface ForecastPoint {
  hour: string;
  type: 'observed' | 'forecast';
  pm25: number;
  lower?: number;
  upper?: number;
  confidence?: number;
}

interface ForecastChartProps {
  forecast?: HourlyForecast[];
  currentPm25?: number;
  customData?: ForecastPoint[];
  data?: { hour: string; predicted: number; lower?: number; upper?: number }[];
  height?: number;
}

export const ForecastChart: React.FC<ForecastChartProps> = ({
  forecast,
  currentPm25 = 118,
  customData,
  data,
  height = 320,
}) => {
  const { theme } = useApp();
  const isDark = theme === 'dark';

  // Construct combined observed (past 3h + current) and forecast (+1h to +6h) data
  const chartData: ForecastPoint[] = customData
    ? customData
    : data
    ? data.map((d) => ({
        hour: d.hour,
        type: 'forecast' as const,
        pm25: d.predicted,
        lower: d.lower,
        upper: d.upper,
      }))
    : [
        { hour: '-3h', type: 'observed', pm25: 96 },
        { hour: '-2h', type: 'observed', pm25: 104 },
        { hour: '-1h', type: 'observed', pm25: 112 },
        { hour: 'CURRENT', type: 'observed', pm25: currentPm25, lower: currentPm25, upper: currentPm25 },
        ...(forecast || [
      { targetHour: 1, predictedPm25: 124, lowerBound: 114, upperBound: 134, confidence: 0.92 },
      { targetHour: 2, predictedPm25: 137, lowerBound: 122, upperBound: 152, confidence: 0.86 },
      { targetHour: 3, predictedPm25: 132, lowerBound: 112, upperBound: 152, confidence: 0.81 },
      { targetHour: 4, predictedPm25: 125, lowerBound: 101, upperBound: 149, confidence: 0.76 },
      { targetHour: 5, predictedPm25: 116, lowerBound: 88, upperBound: 144, confidence: 0.72 },
      { targetHour: 6, predictedPm25: 108, lowerBound: 78, upperBound: 138, confidence: 0.68 },
    ]).map((f) => ({
      hour: `+${f.targetHour}h`,
      type: 'forecast' as const,
      pm25: f.predictedPm25,
      lower: f.lowerBound,
      upper: f.upperBound,
      confidence: f.confidence,
    })),
  ];

  // Prepare chart series: observed line only has points up to CURRENT
  const processedData = chartData.map((d) => ({
    ...d,
    observedPm25: d.type === 'observed' ? d.pm25 : (d.hour === '+1h' ? undefined : undefined),
    forecastPm25: d.type === 'forecast' || d.hour === 'CURRENT' ? d.pm25 : undefined,
    // Connect CURRENT to +1h seamlessly
    forecastUpper: d.type === 'forecast' || d.hour === 'CURRENT' ? (d.upper ?? d.pm25) : undefined,
    forecastLower: d.type === 'forecast' || d.hour === 'CURRENT' ? (d.lower ?? d.pm25) : undefined,
  }));

  const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.08)';
  const textColor = isDark ? '#94a3b8' : '#64748b';
  const tooltipBg = isDark ? '#0f172a' : '#ffffff';
  const tooltipBorder = isDark ? 'rgba(255, 255, 255, 0.15)' : '#cbd5e1';

  return (
    <div style={{ width: '100%', height }}>
      {/* Legend Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-end',
          gap: '1.25rem',
          fontSize: '0.78rem',
          marginBottom: '0.75rem',
          color: 'var(--text-secondary)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <span
            style={{
              width: '14px',
              height: '3px',
              background: 'var(--brand-primary)',
              borderRadius: '2px',
            }}
          />
          <span style={{ fontWeight: 600, color: 'var(--brand-primary)' }}>Observed Telemetry</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <span
            style={{
              width: '14px',
              height: '3px',
              background: 'var(--accent-amber)',
              borderRadius: '2px',
              borderStyle: 'dashed',
            }}
          />
          <span style={{ fontWeight: 600, color: 'var(--accent-amber)' }}>Model Forecast</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <span
            style={{
              width: '12px',
              height: '10px',
              background: 'rgba(245, 158, 11, 0.18)',
              borderRadius: '2px',
              border: '1px solid rgba(245, 158, 11, 0.4)',
            }}
          />
          <span>90% Uncertainty Band</span>
        </div>
      </div>

      <ResponsiveContainer width="100%" height="90%">
        <ComposedChart data={processedData} margin={{ top: 10, right: 15, left: -15, bottom: 5 }}>
          <defs>
            <linearGradient id="forecastUncertainty" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="var(--accent-amber)" stopOpacity={0.28} />
              <stop offset="95%" stopColor="var(--accent-amber)" stopOpacity={0.03} />
            </linearGradient>
          </defs>

          <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />

          <XAxis
            dataKey="hour"
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
            domain={['auto', 'auto']}
            unit=" µg"
          />

          {/* Vertical line separating observed telemetry from future forecast */}
          <ReferenceLine
            x="CURRENT"
            stroke="var(--brand-primary)"
            strokeDasharray="4 4"
            strokeWidth={1.5}
            label={{
              value: 'NOW (LIVE)',
              position: 'top',
              fill: 'var(--brand-primary)',
              fontSize: 10,
              fontWeight: 700,
            }}
          />

          {/* National NAAQS Standard Line (60 µg/m³ 24h limit) */}
          <ReferenceLine
            y={60}
            stroke="rgba(239, 68, 68, 0.6)"
            strokeDasharray="3 3"
            label={{
              value: 'NAAQS 60 µg/m³',
              position: 'insideBottomRight',
              fill: '#ef4444',
              fontSize: 9,
            }}
          />

          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload || !payload.length) return null;
              const data = payload[0].payload as ForecastPoint & {
                observedPm25?: number;
                forecastPm25?: number;
                forecastUpper?: number;
                forecastLower?: number;
              };

              const isForecast = data.type === 'forecast';

              return (
                <div
                  style={{
                    backgroundColor: tooltipBg,
                    border: `1px solid ${tooltipBorder}`,
                    borderRadius: '8px',
                    padding: '10px 12px',
                    boxShadow: 'var(--shadow-md)',
                    fontSize: '12px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                      Horizon: {data.hour}
                    </span>
                    <span
                      style={{
                        fontSize: '10px',
                        padding: '1px 6px',
                        borderRadius: '4px',
                        fontWeight: 700,
                        textTransform: 'uppercase',
                        background: isForecast ? 'rgba(245, 158, 11, 0.15)' : 'rgba(56, 189, 248, 0.15)',
                        color: isForecast ? 'var(--accent-amber)' : 'var(--brand-primary)',
                      }}
                    >
                      {data.type}
                    </span>
                  </div>

                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: isForecast ? 'var(--accent-amber)' : 'var(--brand-primary)' }}>
                    {data.pm25} <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-muted)' }}>µg/m³</span>
                  </div>

                  {isForecast && data.lower && data.upper && (
                    <div style={{ marginTop: '6px', fontSize: '11px', color: 'var(--text-secondary)' }}>
                      <div>90% Range: <strong>{data.lower} – {data.upper} µg/m³</strong></div>
                      {data.confidence && (
                        <div>Confidence: <strong>{Math.round(data.confidence * 100)}%</strong></div>
                      )}
                    </div>
                  )}
                </div>
              );
            }}
          />

          {/* Uncertainty Envelope */}
          <Area
            type="monotone"
            dataKey="forecastUpper"
            stroke="none"
            fill="url(#forecastUncertainty)"
            name="Upper Range"
          />

          {/* Observed Line (Ground Sensors) */}
          <Line
            type="monotone"
            dataKey="observedPm25"
            stroke="var(--brand-primary)"
            strokeWidth={3}
            dot={{ r: 4, fill: 'var(--brand-primary)', stroke: isDark ? '#0b132b' : '#ffffff', strokeWidth: 2 }}
            activeDot={{ r: 6 }}
            name="Observed PM2.5"
            connectNulls={false}
          />

          {/* Forecast Line (XGBoost Ensemble) */}
          <Line
            type="monotone"
            dataKey="forecastPm25"
            stroke="var(--accent-amber)"
            strokeWidth={2.5}
            strokeDasharray="4 4"
            dot={{ r: 4, fill: 'var(--accent-amber)', stroke: isDark ? '#0b132b' : '#ffffff', strokeWidth: 2 }}
            activeDot={{ r: 6 }}
            name="Forecast PM2.5"
            connectNulls={true}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
};
