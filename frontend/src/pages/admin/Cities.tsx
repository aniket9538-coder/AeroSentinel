import React from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';

export const CitiesPage: React.FC = () => {
  const cities = [
    { name: 'Pune', state: 'Maharashtra', stations: 8, cells: 412 },
    { name: 'Mumbai', state: 'Maharashtra', stations: 21, cells: 620 },
    { name: 'Delhi', state: 'Delhi NCR', stations: 38, cells: 1250 },
  ];

  return (
    <PageContainer title="Municipal Configurations" subtitle="Configured pilot areas and monitoring bounds">
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
        {cities.map((c) => (
          <Card key={c.name} title={c.name} subtitle={c.state}>
            <div style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#9ca3af' }}>
              <div>CAAQMS Stations: <strong>{c.stations}</strong></div>
              <div>H3 Hexagonal Cells: <strong>{c.cells}</strong></div>
            </div>
            <div style={{ marginTop: '0.75rem' }}>
              <Badge variant="success">ACTIVE PILOT</Badge>
            </div>
          </Card>
        ))}
      </div>
    </PageContainer>
  );
};
