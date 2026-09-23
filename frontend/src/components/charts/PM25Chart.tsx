import React from 'react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

interface PM25ChartProps {
  data?: { time: string; pm25: number }[];
}

export const PM25Chart: React.FC<PM25ChartProps> = ({
  data = [
    { time: '08:00', pm25: 45 },
    { time: '10:00', pm25: 62 },
    { time: '12:00', pm25: 84 },
    { time: '14:00', pm25: 96 },
    { time: '16:00', pm25: 110 },
    { time: '18:00', pm25: 88 },
  ],
}) => {
  return (
    <div style={{ width: '100%', height: '260px' }}>
      <ResponsiveContainer>
        <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="pm25Gradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.8} />
              <stop offset="95%" stopColor="#06b6d4" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
          <XAxis dataKey="time" stroke="#6b7280" fontSize={12} tickLine={false} />
          <YAxis stroke="#6b7280" fontSize={12} tickLine={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: '#111827',
              borderColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
          />
          <Area type="monotone" dataKey="pm25" stroke="#06b6d4" strokeWidth={2} fillOpacity={1} fill="url(#pm25Gradient)" />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
