import React from 'react';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, Area } from 'recharts';
import { HourlyForecast } from '../../types';

interface ForecastChartProps {
  forecast?: HourlyForecast[];
}

export const ForecastChart: React.FC<ForecastChartProps> = ({
  forecast = [
    { targetHour: 1, predictedPm25: 72, lowerBound: 64, upperBound: 80, confidence: 0.94 },
    { targetHour: 2, predictedPm25: 85, lowerBound: 74, upperBound: 96, confidence: 0.89 },
    { targetHour: 3, predictedPm25: 98, lowerBound: 82, upperBound: 114, confidence: 0.84 },
    { targetHour: 4, predictedPm25: 115, lowerBound: 95, upperBound: 135, confidence: 0.79 },
    { targetHour: 5, predictedPm25: 105, lowerBound: 85, upperBound: 125, confidence: 0.74 },
    { targetHour: 6, predictedPm25: 92, lowerBound: 70, upperBound: 114, confidence: 0.70 },
  ],
}) => {
  const chartData = forecast.map((f) => ({
    hour: `+${f.targetHour}h`,
    pm25: f.predictedPm25,
    lower: f.lowerBound,
    upper: f.upperBound,
  }));

  return (
    <div style={{ width: '100%', height: '260px' }}>
      <ResponsiveContainer>
        <LineChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
          <XAxis dataKey="hour" stroke="#6b7280" fontSize={12} tickLine={false} />
          <YAxis stroke="#6b7280" fontSize={12} tickLine={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: '#111827',
              borderColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
          />
          <Line type="monotone" dataKey="pm25" stroke="#38bdf8" strokeWidth={3} dot={{ r: 4, fill: '#38bdf8' }} name="Predicted PM2.5" />
          <Line type="monotone" dataKey="upper" stroke="#94a3b8" strokeDasharray="3 3" dot={false} name="Upper Bound (95%)" />
          <Line type="monotone" dataKey="lower" stroke="#94a3b8" strokeDasharray="3 3" dot={false} name="Lower Bound (95%)" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};
