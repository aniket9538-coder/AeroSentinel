import { CitizenEvidenceItem } from './alert';

/**
 * Authoritative Unified Context Contract for a Potential Pollution Event (Feature 7).
 *
 * Prediction != Event != Alert != Action
 */
export interface PollutionEventContext {
  id: string;
  eventCode: string;
  gridCellId?: string;
  h3Index: string;
  cityId?: string;
  cityName?: string;
  predictionId?: string;
  severity: string;
  status: string;
  startedAt: string;
  resolvedAt?: string;
  createdAt: string;

  event?: {
    id: string;
    eventCode: string;
    gridCellId?: string;
    h3Index: string;
    predictionId?: string;
    severity: string;
    status: string;
    startedAt: string;
    resolvedAt?: string;
    createdAt: string;
  };

  prediction?: {
    predictionId: string;
    h3Index: string;
    riskScore: number;
    riskLevel: string;
    confidence: number;
    operationalThreshold: number;
    modelVersion: string;
    predictedAt: string;
  };

  forecast?: {
    available: boolean;
    status: string;
    parentPredictionId?: string;
    forecastModelVersion?: string;
    baseTimestamp?: string;
    generatedAt?: string;
    forecastConfidence?: number;
    horizons: Array<{
      horizonHours: number;
      targetTime: string;
      predictedPm25: number;
      lowerBound: number;
      upperBound: number;
      unit: string;
    }>;
  };

  evidence?: {
    evidenceScore?: number;
    consistency?: string;
    triageState: string;
    completeness?: number;
    signalsCount: number;
    signals: Array<{
      signalId: string;
      sourceType: string;
      dataSource: string;
      relevanceTier: string;
      description: string;
      confidenceScore?: number;
      observedAt?: string;
    }>;
  };

  citizenEvidence?: CitizenEvidenceItem[];

  alert?: {
    alertExists: boolean;
    alertId?: string;
    status?: string;
    severity?: string;
    triageState?: string;
    title?: string;
    message?: string;
    acknowledgedAt?: string;
    resolvedAt?: string;
    assignedTeamName?: string;
    verificationResult?: string;
  };
}
