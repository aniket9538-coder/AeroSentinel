import React from 'react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

interface AQIChartProps {
  data?: { station: string; aqi: number }[];
}

export const AQIChart: React.FC<AQIChartProps> = ({
  data = [
    { station: 'Shivajinagar', aqi: 142 },
    { station: 'Katraj', aqi: 98 },
    { station: 'Hadapsar', aqi: 185 },
    { station: 'Pimpri', aqi: 165 },
    { station: 'Kothrud', aqi: 82 },
  ],
}) => {
  return (
    <div style={{ width: '100%', height: '260px' }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
          <XAxis dataKey="station" stroke="#6b7280" fontSize={11} tickLine={false} />
          <YAxis stroke="#6b7280" fontSize={12} tickLine={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: '#111827',
              borderColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
          />
          <Bar dataKey="aqi" fill="#f59e0b" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};
