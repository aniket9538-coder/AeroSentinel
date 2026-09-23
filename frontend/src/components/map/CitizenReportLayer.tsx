import React from 'react';
import { CircleMarker, Popup } from 'react-leaflet';
import { CitizenReport } from '../../types';

interface CitizenReportLayerProps {
  reports?: CitizenReport[];
}

export const CitizenReportLayer: React.FC<CitizenReportLayerProps> = ({ reports = [] }) => {
  return (
    <>
      {reports.map((report) => (
        <CircleMarker
          key={report.id}
          center={[report.latitude, report.longitude]}
          radius={5}
          pathOptions={{
            color: '#06b6d4',
            fillColor: '#22d3ee',
            fillOpacity: 0.9,
            weight: 1,
          }}
        >
          <Popup>
            <div style={{ color: '#0f172a', fontSize: '0.85rem' }}>
              <strong style={{ color: '#0891b2' }}>Citizen Report ({report.category})</strong>
              <div style={{ marginTop: '0.2rem' }}>{report.description}</div>
              {report.imageUrl && (
                <div style={{ marginTop: '0.4rem' }}>
                  <img
                    src={report.imageUrl}
                    alt="Citizen submission"
                    style={{ width: '100%', maxHeight: '100px', objectFit: 'cover', borderRadius: '4px' }}
                  />
                </div>
              )}
              <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: '0.3rem' }}>
                Status: {report.status} | {new Date(report.submittedAt).toLocaleTimeString()}
              </div>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </>
  );
};
