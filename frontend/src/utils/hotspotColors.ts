import { HotspotRiskLevel } from '../types/hotspot';

export interface RiskLevelStyle {
  color: string;
  fillColor: string;
  bg: string;
  border: string;
  label: string;
  description: string;
}

export const HOTSPOT_RISK_STYLES: Record<HotspotRiskLevel, RiskLevelStyle> = {
  LOW: {
    color: '#10b981',
    fillColor: '#10b981',
    bg: 'rgba(16, 185, 129, 0.12)',
    border: 'rgba(16, 185, 129, 0.35)',
    label: 'Low Risk',
    description: 'Minimal atmospheric stagnation and low pollutant accumulation.',
  },
  MODERATE: {
    color: '#f59e0b',
    fillColor: '#f59e0b',
    bg: 'rgba(245, 158, 11, 0.12)',
    border: 'rgba(245, 158, 11, 0.35)',
    label: 'Moderate Risk',
    description: 'Elevated particulate concentration or moderate local stagnation.',
  },
  HIGH: {
    color: '#f97316',
    fillColor: '#f97316',
    bg: 'rgba(249, 115, 22, 0.15)',
    border: 'rgba(249, 115, 22, 0.40)',
    label: 'High Risk',
    description: 'Strong potential hotspot conditions; stagnant winds and heavy load.',
  },
  CRITICAL: {
    color: '#ef4444',
    fillColor: '#ef4444',
    bg: 'rgba(239, 68, 68, 0.18)',
    border: 'rgba(239, 68, 68, 0.50)',
    label: 'Critical Risk',
    description: 'Severe potential hotspot signature with extreme concentration persistence.',
  },
};

export const getRiskStyle = (level?: string): RiskLevelStyle => {
  const normalized = (level || 'LOW').toUpperCase() as HotspotRiskLevel;
  return HOTSPOT_RISK_STYLES[normalized] || HOTSPOT_RISK_STYLES.LOW;
};
