import { BadgeVariant } from '../components/common/Badge';
import { GlobalModel, NodeStatus, RoundStatus } from '../types/federated';

export interface NodeStatusConfig {
  variant: BadgeVariant;
  label: string;
  pulse: boolean;
  color: string;
  bg: string;
}

export interface RoundStatusConfig {
  variant: BadgeVariant;
  label: string;
  isTerminal: boolean;
}

export interface QuorumProgress {
  percentage: number;
  isQuorumMet: boolean;
  received: number;
  required: number;
  label: string;
}

/**
 * Returns badge styling and attributes based on municipal node status.
 */
export function getNodeStatusConfig(status?: string | null): NodeStatusConfig {
  const normalized = (status || '').toUpperCase() as NodeStatus;
  switch (normalized) {
    case 'ONLINE':
      return {
        variant: 'success',
        label: 'ONLINE',
        pulse: true,
        color: '#10b981',
        bg: 'rgba(16, 185, 129, 0.15)',
      };
    case 'TRAINING':
      return {
        variant: 'info',
        label: 'TRAINING',
        pulse: true,
        color: '#38bdf8',
        bg: 'rgba(56, 189, 248, 0.15)',
      };
    case 'OFFLINE':
      return {
        variant: 'danger',
        label: 'OFFLINE',
        pulse: false,
        color: '#ef4444',
        bg: 'rgba(239, 68, 68, 0.15)',
      };
    case 'STALE':
      return {
        variant: 'warning',
        label: 'STALE',
        pulse: false,
        color: '#f59e0b',
        bg: 'rgba(245, 158, 11, 0.15)',
      };
    default:
      return {
        variant: 'neutral',
        label: status || 'UNKNOWN',
        pulse: false,
        color: '#9ca3af',
        bg: 'rgba(156, 163, 175, 0.15)',
      };
  }
}

/**
 * Returns badge styling and configuration for round lifecycle states.
 */
export function getRoundStatusConfig(status?: string | null): RoundStatusConfig {
  const normalized = (status || '').toUpperCase() as RoundStatus;
  switch (normalized) {
    case 'COMPLETED':
      return { variant: 'success', label: 'COMPLETED', isTerminal: true };
    case 'FAILED':
      return { variant: 'danger', label: 'FAILED', isTerminal: true };
    case 'AGGREGATING':
      return { variant: 'info', label: 'AGGREGATING (FedAvg)', isTerminal: false };
    case 'UPDATES_COLLECTING':
      return { variant: 'warning', label: 'COLLECTING UPDATES', isTerminal: false };
    case 'TRAINING':
      return { variant: 'info', label: 'LOCAL TRAINING', isTerminal: false };
    case 'MODEL_DISTRIBUTED':
      return { variant: 'info', label: 'MODEL DISTRIBUTED', isTerminal: false };
    case 'CREATED':
      return { variant: 'neutral', label: 'INITIALIZED', isTerminal: false };
    default:
      return { variant: 'neutral', label: status || 'UNKNOWN', isTerminal: false };
  }
}

/**
 * Computes quorum progress percentages and thresholds.
 */
export function calculateQuorumProgress(
  receivedUpdatesCount: number = 0,
  minQuorum: number = 2
): QuorumProgress {
  const req = Math.max(1, minQuorum);
  const rec = Math.max(0, receivedUpdatesCount);
  const isQuorumMet = rec >= req;
  const percentage = Math.min(100, Math.round((rec / req) * 100));

  return {
    percentage,
    isQuorumMet,
    received: rec,
    required: req,
    label: `${rec} / ${req} updates received (${percentage}%)`,
  };
}

/**
 * Formats a metric with decimal precision or default fallback.
 */
export function formatMetric(
  val?: number | null,
  precision: number = 4,
  fallback: string = 'N/A'
): string {
  if (val === undefined || val === null || isNaN(val)) {
    return fallback;
  }
  return Number(val).toFixed(precision);
}

/**
 * Formats relative timestamp or formatted date string.
 */
export function formatRelativeTime(isoString?: string | null): string {
  if (!isoString) return 'Never';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return 'Invalid date';

    const now = new Date();
    const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffSec < 10) return 'Just now';
    if (diffSec < 60) return `${diffSec}s ago`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}h ago`;
    const diffDays = Math.floor(diffHour / 24);
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  } catch {
    return 'Invalid date';
  }
}

/**
 * Formats participating nodes array into a readable joined badge string.
 */
export function formatNodeList(nodes?: string[] | null): string {
  if (!nodes || nodes.length === 0) return 'None';
  return nodes.join(', ');
}

/**
 * Sorts global model catalog with active model prioritized first, followed by creation date desc.
 */
export function sortModelCatalog(models: GlobalModel[]): GlobalModel[] {
  return [...models].sort((a, b) => {
    if (a.isActive && !b.isActive) return -1;
    if (!a.isActive && b.isActive) return 1;
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    return dateB - dateA;
  });
}
