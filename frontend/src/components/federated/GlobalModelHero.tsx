import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { GlobalModel } from '../../types/federated';
import { formatMetric, formatRelativeTime } from '../../utils/federatedUtils';
import { Cpu, Database, CheckCircle2, GitBranch, Layers, HardDrive } from 'lucide-react';

interface GlobalModelHeroProps {
  model?: GlobalModel | null;
  isLoading?: boolean;
}

export const GlobalModelHero: React.FC<GlobalModelHeroProps> = ({ model, isLoading }) => {
  if (isLoading) {
    return (
      <Card>
        <div style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
          Loading active consensus model details...
        </div>
      </Card>
    );
  }

  if (!model) {
    return (
      <Card>
        <div style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
          No active consensus global model registered yet.
        </div>
      </Card>
    );
  }

  const metrics = model.metrics || {};
  const mae = metrics.mae;
  const rmse = metrics.rmse;
  const rocAuc = metrics.rocAuc ?? metrics.roc_auc;
  const brierScore = metrics.brierScore ?? metrics.brier_score;

  return (
    <Card className="global-model-hero">
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              padding: '0.85rem',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.2), rgba(59, 130, 246, 0.2))',
              color: '#38bdf8',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              boxShadow: '0 0 15px rgba(56, 189, 248, 0.15)',
            }}
          >
            <Cpu size={32} />
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f3f4f6', margin: 0 }}>
                {model.modelVersion}
              </h3>
              {model.isActive && (
                <Badge variant="success" pulse size="sm">
                  <CheckCircle2 size={12} style={{ marginRight: '0.25rem' }} />
                  ACTIVE CONSENSUS
                </Badge>
              )}
            </div>

            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                alignItems: 'center',
                gap: '0.75rem',
                marginTop: '0.4rem',
                fontSize: '0.8rem',
                color: '#9ca3af',
              }}
            >
              {model.baseModelVersion && (
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                  <GitBranch size={13} color="#a855f7" />
                  Base: <strong style={{ color: '#e5e7eb' }}>{model.baseModelVersion}</strong>
                </span>
              )}
              {model.roundId && (
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                  <Layers size={13} color="#f59e0b" />
                  Round: <strong style={{ color: '#e5e7eb' }}>{model.roundId}</strong>
                </span>
              )}
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <Database size={13} color="#10b981" />
                Aggregated Samples:{' '}
                <strong style={{ color: '#e5e7eb' }}>
                  {model.totalSamples?.toLocaleString() || 0}
                </strong>
              </span>
            </div>
          </div>
        </div>

        {model.artifactPath && (
          <div
            style={{
              padding: '0.4rem 0.75rem',
              borderRadius: '6px',
              background: 'rgba(0, 0, 0, 0.35)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              fontFamily: 'monospace',
              fontSize: '0.75rem',
              color: '#9ca3af',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            <HardDrive size={13} color="#9ca3af" />
            <span>{model.artifactPath}</span>
          </div>
        )}
      </div>

      {/* Participating Municipal Nodes Chips */}
      <div
        style={{
          marginTop: '1rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          flexWrap: 'wrap',
        }}
      >
        <span style={{ fontSize: '0.75rem', color: '#9ca3af', fontWeight: 500 }}>
          Participating Nodes:
        </span>
        {model.participatingNodes && model.participatingNodes.length > 0 ? (
          model.participatingNodes.map((city) => (
            <span
              key={city}
              style={{
                fontSize: '0.7rem',
                fontWeight: 600,
                padding: '0.2rem 0.55rem',
                borderRadius: '6px',
                background: 'rgba(56, 189, 248, 0.12)',
                border: '1px solid rgba(56, 189, 248, 0.25)',
                color: '#38bdf8',
                letterSpacing: '0.04em',
              }}
            >
              {city}
            </span>
          ))
        ) : (
          <span style={{ fontSize: '0.75rem', color: '#6b7280' }}>All Active Municipalities</span>
        )}
        <span style={{ fontSize: '0.75rem', color: '#6b7280', marginLeft: 'auto' }}>
          Created: {formatRelativeTime(model.createdAt)}
        </span>
      </div>

      {/* 4-Column Metric Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
          gap: '1rem',
          marginTop: '1.25rem',
          paddingTop: '1rem',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        <div
          style={{
            padding: '0.75rem',
            background: 'rgba(255, 255, 255, 0.02)',
            borderRadius: '8px',
            border: '1px solid rgba(255, 255, 255, 0.04)',
          }}
        >
          <div style={{ fontSize: '0.7rem', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            MAE (Error)
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f3f4f6', marginTop: '0.2rem' }}>
            {formatMetric(mae)}
          </div>
          <div style={{ fontSize: '0.7rem', color: '#6b7280', marginTop: '0.1rem' }}>
            Lower is better
          </div>
        </div>

        <div
          style={{
            padding: '0.75rem',
            background: 'rgba(255, 255, 255, 0.02)',
            borderRadius: '8px',
            border: '1px solid rgba(255, 255, 255, 0.04)',
          }}
        >
          <div style={{ fontSize: '0.7rem', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            RMSE
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f3f4f6', marginTop: '0.2rem' }}>
            {formatMetric(rmse)}
          </div>
          <div style={{ fontSize: '0.7rem', color: '#6b7280', marginTop: '0.1rem' }}>
            Quadratic penalty
          </div>
        </div>

        <div
          style={{
            padding: '0.75rem',
            background: 'rgba(255, 255, 255, 0.02)',
            borderRadius: '8px',
            border: '1px solid rgba(255, 255, 255, 0.04)',
          }}
        >
          <div style={{ fontSize: '0.7rem', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            ROC-AUC
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#38bdf8', marginTop: '0.2rem' }}>
            {formatMetric(rocAuc)}
          </div>
          <div style={{ fontSize: '0.7rem', color: '#6b7280', marginTop: '0.1rem' }}>
            Discrimination power
          </div>
        </div>

        <div
          style={{
            padding: '0.75rem',
            background: 'rgba(255, 255, 255, 0.02)',
            borderRadius: '8px',
            border: '1px solid rgba(255, 255, 255, 0.04)',
          }}
        >
          <div style={{ fontSize: '0.7rem', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Brier Score
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#10b981', marginTop: '0.2rem' }}>
            {formatMetric(brierScore)}
          </div>
          <div style={{ fontSize: '0.7rem', color: '#6b7280', marginTop: '0.1rem' }}>
            Calibration accuracy
          </div>
        </div>
      </div>
    </Card>
  );
};

export default GlobalModelHero;
