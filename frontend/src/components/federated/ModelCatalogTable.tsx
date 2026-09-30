import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { GlobalModel } from '../../types/federated';
import { formatMetric, formatRelativeTime, sortModelCatalog } from '../../utils/federatedUtils';
import { History, Search, CheckCircle2, GitBranch, Cpu } from 'lucide-react';

interface ModelCatalogTableProps {
  models: GlobalModel[];
  isLoading?: boolean;
}

export const ModelCatalogTable: React.FC<ModelCatalogTableProps> = ({ models, isLoading }) => {
  const [search, setSearch] = useState('');

  const sorted = sortModelCatalog(models);
  const filtered = sorted.filter((m) => {
    const term = search.toLowerCase();
    return (
      m.modelVersion.toLowerCase().includes(term) ||
      (m.baseModelVersion && m.baseModelVersion.toLowerCase().includes(term)) ||
      (m.roundId && m.roundId.toLowerCase().includes(term)) ||
      (m.participatingNodes && m.participatingNodes.some((n) => n.toLowerCase().includes(term)))
    );
  });

  return (
    <Card className="model-catalog-table">
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '1rem',
          marginBottom: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <History size={18} color="#06b6d4" />
          <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: '#f3f4f6', margin: 0 }}>
            Consensus Model Lineage & Version Catalog
          </h3>
          <span
            style={{
              fontSize: '0.75rem',
              color: '#9ca3af',
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '0.15rem 0.5rem',
              borderRadius: '9999px',
            }}
          >
            {models.length} Versions
          </span>
        </div>

        <div style={{ position: 'relative', minWidth: '220px' }}>
          <Search
            size={14}
            color="#9ca3af"
            style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' }}
          />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search version or city..."
            style={{
              width: '100%',
              padding: '0.45rem 0.75rem 0.45rem 2.2rem',
              borderRadius: '6px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: '#f3f4f6',
              fontSize: '0.8rem',
              outline: 'none',
            }}
          />
        </div>
      </div>

      {isLoading ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
          Loading consensus model lineage...
        </div>
      ) : filtered.length === 0 ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
          No models matching search criteria.
        </div>
      ) : (
        <div style={{ overflowX: 'auto', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'rgba(255, 255, 255, 0.04)', color: '#9ca3af' }}>
                <th style={{ padding: '0.65rem 0.85rem' }}>Model Version</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Base Version</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Round ID</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Aggregated Samples</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Participating Cities</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>MAE</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>RMSE</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>ROC-AUC</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Status</th>
                <th style={{ padding: '0.65rem 0.85rem' }}>Created</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((m) => {
                const metrics = m.metrics || {};
                const mae = metrics.mae;
                const rmse = metrics.rmse;
                const rocAuc = metrics.rocAuc ?? metrics.roc_auc;

                return (
                  <tr
                    key={m.modelVersion}
                    style={{
                      borderTop: '1px solid rgba(255, 255, 255, 0.05)',
                      background: m.isActive ? 'rgba(16, 185, 129, 0.03)' : 'transparent',
                    }}
                  >
                    <td style={{ padding: '0.65rem 0.85rem', fontWeight: 600, color: '#f3f4f6' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Cpu size={14} color={m.isActive ? '#10b981' : '#9ca3af'} />
                        <span>{m.modelVersion}</span>
                      </div>
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#9ca3af' }}>
                      {m.baseModelVersion ? (
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <GitBranch size={11} color="#a855f7" />
                          {m.baseModelVersion}
                        </span>
                      ) : (
                        <span style={{ color: '#6b7280' }}>Seed / Initial</span>
                      )}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#e5e7eb', fontFamily: 'monospace' }}>
                      {m.roundId || '—'}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#38bdf8', fontWeight: 500 }}>
                      {m.totalSamples?.toLocaleString() || 0}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem' }}>
                      <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
                        {m.participatingNodes && m.participatingNodes.length > 0 ? (
                          m.participatingNodes.map((n) => (
                            <span
                              key={n}
                              style={{
                                fontSize: '0.65rem',
                                padding: '0.1rem 0.35rem',
                                borderRadius: '4px',
                                background: 'rgba(255, 255, 255, 0.08)',
                                color: '#e5e7eb',
                              }}
                            >
                              {n}
                            </span>
                          ))
                        ) : (
                          <span style={{ color: '#6b7280', fontSize: '0.75rem' }}>Initial</span>
                        )}
                      </div>
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#f3f4f6' }}>
                      {formatMetric(mae)}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#f3f4f6' }}>
                      {formatMetric(rmse)}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#38bdf8', fontWeight: 500 }}>
                      {formatMetric(rocAuc)}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem' }}>
                      {m.isActive ? (
                        <Badge variant="success" size="sm" pulse>
                          ACTIVE
                        </Badge>
                      ) : (
                        <Badge variant="neutral" size="sm">
                          ARCHIVED
                        </Badge>
                      )}
                    </td>
                    <td style={{ padding: '0.65rem 0.85rem', color: '#6b7280', fontSize: '0.75rem' }}>
                      {formatRelativeTime(m.createdAt)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
};

export default ModelCatalogTable;
