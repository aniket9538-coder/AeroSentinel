import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { FederatedStatus } from '../../components/federated/FederatedStatus';
import { CityNodeCard } from '../../components/federated/CityNodeCard';
import { ModelVersionCard } from '../../components/federated/ModelVersionCard';
import { Button } from '../../components/common/Button';
import { RefreshCw } from 'lucide-react';
import { FederatedNode } from '../../types';

export const FederatedNetwork: React.FC = () => {
  const [nodes, setNodes] = useState<FederatedNode[]>([
    { id: 'node-pun', nodeName: 'Pune Municipal Node', cityId: 'pune', status: 'ONLINE', modelVersion: 'xgb-pun-v1.2', lastUpdateAt: new Date().toISOString() },
    { id: 'node-mum', nodeName: 'Mumbai Coastal Node', cityId: 'mumbai', status: 'ONLINE', modelVersion: 'xgb-mum-v1.2', lastUpdateAt: new Date().toISOString() },
    { id: 'node-del', nodeName: 'Delhi Regional Node', cityId: 'delhi', status: 'ONLINE', modelVersion: 'xgb-del-v1.2', lastUpdateAt: new Date().toISOString() },
  ]);

  const [round, setRound] = useState(4);
  const [isAggregating, setIsAggregating] = useState(false);
  const [globalVersion, setGlobalVersion] = useState('global-ensemble-v1.2');

  const handleTriggerRound = () => {
    setIsAggregating(true);
    setTimeout(() => {
      setRound(round + 1);
      setGlobalVersion(`global-ensemble-v1.${round}`);
      setIsAggregating(false);
      alert(`Federated round #${round} completed! Parameter weights successfully averaged via FedAvg.`);
    }, 1200);
  };

  return (
    <PageContainer
      title="Multi-City Federated Network"
      subtitle="Decentralized collaborative model training without raw telemetry centralization"
      action={
        <Button variant="primary" onClick={handleTriggerRound} isLoading={isAggregating}>
          <RefreshCw size={14} style={{ marginRight: '0.4rem' }} />
          Trigger Aggregation Round
        </Button>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <FederatedStatus
          currentRound={round}
          totalNodes={3}
          activeNodes={3}
          status="SYNCHRONIZED"
        />

        <div>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#9ca3af', marginBottom: '1rem' }}>
            Participating Municipal Federated Nodes
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.5rem' }}>
            {nodes.map((node) => (
              <CityNodeCard key={node.id} node={node} />
            ))}
          </div>
        </div>

        <ModelVersionCard
          globalVersion={globalVersion}
          aggregationStrategy="Sample-Weighted Federated Averaging (FedAvg)"
          lastAggregatedAt={new Date().toLocaleTimeString()}
        />
      </div>
    </PageContainer>
  );
};
