import React from 'react';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, Legend } from 'recharts';

interface PollutantTrendChartProps {
  data?: { time: string; pm25: number; pm10: number; no2: number }[];
}

export const PollutantTrendChart: React.FC<PollutantTrendChartProps> = ({
  data = [
    { time: '00:00', pm25: 42, pm10: 80, no2: 24 },
    { time: '04:00', pm25: 38, pm10: 72, no2: 18 },
    { time: '08:00', pm25: 85, pm10: 160, no2: 45 },
    { time: '12:00', pm25: 64, pm10: 120, no2: 32 },
    { time: '16:00', pm25: 75, pm10: 145, no2: 38 },
    { time: '20:00', pm25: 98, pm10: 190, no2: 52 },
  ],
}) => {
  return (
    <div style={{ width: '100%', height: '260px' }}>
      <ResponsiveContainer>
        <LineChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
          <XAxis dataKey="time" stroke="#6b7280" fontSize={11} tickLine={false} />
          <YAxis stroke="#6b7280" fontSize={12} tickLine={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: '#111827',
              borderColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
          />
          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
          <Line type="monotone" dataKey="pm25" stroke="#06b6d4" strokeWidth={2} dot={false} name="PM2.5 (µg/m³)" />
          <Line type="monotone" dataKey="pm10" stroke="#f59e0b" strokeWidth={2} dot={false} name="PM10 (µg/m³)" />
          <Line type="monotone" dataKey="no2" stroke="#8b5cf6" strokeWidth={2} dot={false} name="NO2 (ppb)" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};
