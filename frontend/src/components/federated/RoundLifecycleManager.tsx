import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { RoundDetail, RoundResponse } from '../../types/federated';
import { getRoundStatusConfig, formatRelativeTime, formatMetric } from '../../utils/federatedUtils';
import { RoundQuorumProgress } from './RoundQuorumProgress';
import { RefreshCw, Play, CheckCircle, AlertTriangle, Layers, Clock, Cpu } from 'lucide-react';

interface RoundLifecycleManagerProps {
  rounds: RoundResponse[];
  selectedRoundDetail?: RoundDetail | null;
  isLoadingDetail?: boolean;
  onSelectRound: (roundId: string) => void;
  onInitiateRoundClick: () => void;
  onTriggerAggregation: (roundId: string) => Promise<void>;
}

export const RoundLifecycleManager: React.FC<RoundLifecycleManagerProps> = ({
  rounds,
  selectedRoundDetail,
  isLoadingDetail,
  onSelectRound,
  onInitiateRoundClick,
  onTriggerAggregation,
}) => {
  const [isAggregating, setIsAggregating] = useState(false);

  const activeRound = selectedRoundDetail || (rounds.length > 0 ? (rounds[0] as any) : null);
  const statusCfg = getRoundStatusConfig(activeRound?.status);

  const canAggregate =
    activeRound &&
    activeRound.status !== 'COMPLETED' &&
    activeRound.status !== 'FAILED' &&
    (activeRound.receivedUpdatesCount ?? 0) >= (activeRound.minQuorum ?? 2);

  const handleAggregate = async () => {
    if (!activeRound || isAggregating) return;
    setIsAggregating(true);
    try {
      await onTriggerAggregation(activeRound.roundId);
    } finally {
      setIsAggregating(false);
    }
  };

  return (
    <Card className="round-lifecycle-manager">
      {/* Header and Controls */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '1rem',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          paddingBottom: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Layers size={20} color="#a855f7" />
          <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: '#f3f4f6', margin: 0 }}>
            Federated Training Round Lifecycle
          </h3>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          {rounds.length > 1 && (
            <select
              value={activeRound?.roundId || ''}
              onChange={(e) => onSelectRound(e.target.value)}
              style={{
                background: 'rgba(255, 255, 255, 0.06)',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                color: '#f3f4f6',
                borderRadius: '6px',
                padding: '0.4rem 0.6rem',
                fontSize: '0.8rem',
                outline: 'none',
              }}
            >
              {rounds.map((r) => (
                <option key={r.roundId} value={r.roundId} style={{ background: '#1e293b' }}>
                  {r.roundId} ({r.status})
                </option>
              ))}
            </select>
          )}

          <Button variant="outline" size="sm" onClick={onInitiateRoundClick}>
            <Play size={13} style={{ marginRight: '0.35rem' }} />
            New Round
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={handleAggregate}
            isLoading={isAggregating}
            disabled={!canAggregate}
          >
            <RefreshCw size={13} style={{ marginRight: '0.35rem' }} />
            Trigger FedAvg Aggregation
          </Button>
        </div>
      </div>

      {isLoadingDetail ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
          Loading round details...
        </div>
      ) : !activeRound ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
          No federated training rounds recorded. Click "New Round" to start.
        </div>
      ) : (
        <div style={{ marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Active Round Info Bar */}
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: 'rgba(255, 255, 255, 0.02)',
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              gap: '0.75rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <span style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f3f4f6' }}>
                {activeRound.roundId}
              </span>
              <Badge variant={statusCfg.variant}>{statusCfg.label}</Badge>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8rem', color: '#9ca3af' }}>
              <span>
                Base: <strong style={{ color: '#e5e7eb' }}>{activeRound.baseModelVersion}</strong>
              </span>
              {activeRound.targetModelVersion && (
                <span>
                  Target: <strong style={{ color: '#38bdf8' }}>{activeRound.targetModelVersion}</strong>
                </span>
              )}
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <Clock size={13} />
                Started: {formatRelativeTime(activeRound.createdAt)}
              </span>
            </div>
          </div>

          {/* Quorum Progress Meter */}
          <RoundQuorumProgress
            receivedCount={activeRound.receivedUpdatesCount ?? (activeRound.updates?.length || 0)}
            minQuorum={activeRound.minQuorum ?? 2}
          />

          {/* Failure Alert Banner if Failed */}
          {activeRound.status === 'FAILED' && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#ef4444',
                fontSize: '0.85rem',
              }}
            >
              <AlertTriangle size={18} />
              <span>
                <strong>Round Failed:</strong>{' '}
                {activeRound.failureReason || 'Minimum quorum consensus was not achieved.'}
              </span>
            </div>
          )}

          {/* Updates Table */}
          <div>
            <div
              style={{
                fontSize: '0.85rem',
                fontWeight: 600,
                color: '#9ca3af',
                marginBottom: '0.5rem',
                display: 'flex',
                justifyContent: 'space-between',
              }}
            >
              <span>Contributed Municipal Parameter Updates</span>
              <span>
                Total Samples Contributed:{' '}
                <strong style={{ color: '#f3f4f6' }}>
                  {activeRound.totalSamples?.toLocaleString() ||
                    activeRound.updates?.reduce((sum: number, u: any) => sum + (u.sampleCount || 0), 0)?.toLocaleString() ||
                    0}
                </strong>
              </span>
            </div>

            {activeRound.updates && activeRound.updates.length > 0 ? (
              <div
                style={{
                  overflowX: 'auto',
                  borderRadius: '8px',
                  border: '1px solid rgba(255, 255, 255, 0.08)',
                }}
              >
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ background: 'rgba(255, 255, 255, 0.04)', color: '#9ca3af' }}>
                      <th style={{ padding: '0.6rem 0.8rem' }}>City Node</th>
                      <th style={{ padding: '0.6rem 0.8rem' }}>Sample Contribution</th>
                      <th style={{ padding: '0.6rem 0.8rem' }}>Validation Status</th>
                      <th style={{ padding: '0.6rem 0.8rem' }}>Submission Timestamp</th>
                    </tr>
                  </thead>
                  <tbody>
                    {activeRound.updates.map((u: any) => (
                      <tr
                        key={u.updateId || `${u.roundId}-${u.cityName}`}
                        style={{ borderTop: '1px solid rgba(255, 255, 255, 0.05)' }}
                      >
                        <td style={{ padding: '0.6rem 0.8rem', fontWeight: 600, color: '#f3f4f6' }}>
                          {u.cityName}
                        </td>
                        <td style={{ padding: '0.6rem 0.8rem', color: '#38bdf8' }}>
                          {u.sampleCount?.toLocaleString()} samples
                        </td>
                        <td style={{ padding: '0.6rem 0.8rem' }}>
                          <Badge
                            variant={u.status === 'ACCEPTED' ? 'success' : 'warning'}
                            size="sm"
                          >
                            {u.status}
                          </Badge>
                        </td>
                        <td style={{ padding: '0.6rem 0.8rem', color: '#6b7280' }}>
                          {formatRelativeTime(u.submittedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div
                style={{
                  padding: '1.25rem',
                  textAlign: 'center',
                  background: 'rgba(255, 255, 255, 0.02)',
                  borderRadius: '8px',
                  color: '#6b7280',
                  fontSize: '0.8rem',
                }}
              >
                No municipal parameter updates submitted yet for this round.
              </div>
            )}
          </div>

          {/* Aggregated Consensus Metrics if Completed */}
          {activeRound.aggregatedMetrics && (
            <div
              style={{
                marginTop: '0.5rem',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                background: 'rgba(16, 185, 129, 0.06)',
                border: '1px solid rgba(16, 185, 129, 0.2)',
                display: 'flex',
                flexWrap: 'wrap',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '1rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#10b981', fontSize: '0.85rem', fontWeight: 600 }}>
                <CheckCircle size={16} />
                <span>Round Aggregation Consensus Achieved</span>
              </div>
              <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8rem', color: '#9ca3af' }}>
                <span>MAE: <strong style={{ color: '#f3f4f6' }}>{formatMetric(activeRound.aggregatedMetrics.mae)}</strong></span>
                <span>RMSE: <strong style={{ color: '#f3f4f6' }}>{formatMetric(activeRound.aggregatedMetrics.rmse)}</strong></span>
                <span>ROC-AUC: <strong style={{ color: '#38bdf8' }}>{formatMetric(activeRound.aggregatedMetrics.rocAuc)}</strong></span>
                <span>Brier: <strong style={{ color: '#10b981' }}>{formatMetric(activeRound.aggregatedMetrics.brierScore)}</strong></span>
              </div>
            </div>
          )}
        </div>
      )}
    </Card>
  );
};

export default RoundLifecycleManager;
