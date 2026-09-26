import React from 'react';
import { EvidenceItem, RichEvidenceItem } from './EvidenceItem';

interface EvidenceTimelineProps {
  items?: RichEvidenceItem[];
}

export const defaultEvidenceItems: RichEvidenceItem[] = [
  {
    id: 'ev-1',
    time: '10:10',
    source: 'CPCB CAAQMS (Shivajinagar)',
    sourceCategory: 'sensor',
    signal: 'PM2.5 rising rapidly',
    detail: 'Telemetry surged from 94 µg/m³ to 118 µg/m³ within a 20-minute interval (+25.5% delta).',
    quality: 'High (Calibrated)',
    confidenceScore: 0.95,
  },
  {
    id: 'ev-2',
    time: '10:15',
    source: 'IMD Microclimate Telemetry',
    sourceCategory: 'weather',
    signal: 'Low surface wind / boundary layer stagnation',
    detail: 'Surface wind collapsed to 1.1 m/s (WNW 285°). Low ventilation coefficient impedes particulate dispersion.',
    quality: 'Verified IMD Feed',
    confidenceScore: 0.91,
  },
  {
    id: 'ev-3',
    time: '10:20',
    source: 'NASA FIRMS VIIRS / MODIS',
    sourceCategory: 'satellite',
    signal: 'Thermal anomaly / fire signal nearby',
    detail: 'Active thermal anomaly detected 1.4 km upwind. Fire Radiative Power (FRP): 14.8 MW.',
    quality: 'Nominal Satellite Pass',
    confidenceScore: 0.86,
  },
  {
    id: 'ev-4',
    time: '10:25',
    source: 'Sentinel-5P TROPOMI',
    sourceCategory: 'satellite',
    signal: 'Satellite atmospheric indicator elevated',
    detail: 'Tropospheric NO2 and Aerosol Index (AI) exceed 88th percentile across adjacent H3 cell cluster.',
    quality: 'Valid Tropospheric Column',
    confidenceScore: 0.84,
  },
  {
    id: 'ev-5',
    time: '10:28',
    source: 'Citizen Ground Report #CR-4821',
    sourceCategory: 'citizen',
    signal: 'Corroborating citizen visual report',
    detail: 'Resident photo upload: dense dark plume observed behind industrial estate. Gemini Vision flagged open biomass burning.',
    quality: 'Gemini Vision Verified',
    confidenceScore: 0.88,
  },
  {
    id: 'ev-6',
    time: '10:30',
    source: 'AeroSentinel Hotspot Engine v1',
    sourceCategory: 'model',
    signal: 'Hotspot risk score upgraded to HIGH',
    detail: 'Composite spatial probability matrix reached 0.84 threshold. Dispatched priority evidence dossier.',
    quality: 'Model Convergence OK',
    confidenceScore: 0.84,
  },
];

export const EvidenceTimeline: React.FC<EvidenceTimelineProps> = ({
  items = defaultEvidenceItems,
}) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {items.map((item, index) => (
        <EvidenceItem
          key={item.id}
          evidence={item}
          isLast={index === items.length - 1}
        />
      ))}
    </div>
  );
};
