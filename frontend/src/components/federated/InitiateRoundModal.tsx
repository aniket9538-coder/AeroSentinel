import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { CreateRoundRequest } from '../../types/federated';
import { PlusCircle, Info } from 'lucide-react';

interface InitiateRoundModalProps {
  isOpen: boolean;
  onClose: () => void;
  activeModelVersion?: string;
  onSubmit: (payload: CreateRoundRequest) => Promise<void>;
}

export const InitiateRoundModal: React.FC<InitiateRoundModalProps> = ({
  isOpen,
  onClose,
  activeModelVersion = 'global-v3',
  onSubmit,
}) => {
  const [baseVersion, setBaseVersion] = useState(activeModelVersion);
  const [roundId, setRoundId] = useState('');
  const [minQuorum, setMinQuorum] = useState(2);
  const [selectedNodes, setSelectedNodes] = useState<string[]>(['PUNE', 'MUMBAI', 'DELHI']);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const availableNodes = [
    { id: 'PUNE', label: 'Pune Municipal Environmental Node' },
    { id: 'MUMBAI', label: 'Mumbai BMC Environmental Node' },
    { id: 'DELHI', label: 'Delhi DPCC Network Node' },
  ];

  const handleToggleNode = (nodeId: string) => {
    if (selectedNodes.includes(nodeId)) {
      setSelectedNodes(selectedNodes.filter((n) => n !== nodeId));
    } else {
      setSelectedNodes([...selectedNodes, nodeId]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!baseVersion.trim()) {
      setError('Base model version is required.');
      return;
    }
    if (selectedNodes.length < minQuorum) {
      setError(`Selected nodes (${selectedNodes.length}) must be at least minimum quorum (${minQuorum}).`);
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await onSubmit({
        roundId: roundId.trim() || undefined,
        baseModelVersion: baseVersion.trim(),
        minQuorum,
        participatingNodes: selectedNodes,
      });
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to start round.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Initiate Federated Training Round">
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div
          style={{
            padding: '0.75rem',
            borderRadius: '8px',
            background: 'rgba(56, 189, 248, 0.1)',
            border: '1px solid rgba(56, 189, 248, 0.2)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: '0.8rem',
            color: '#38bdf8',
          }}
        >
          <Info size={16} />
          <span>
            Initiating a round broadcasts the base model parameters to municipal nodes for local training.
          </span>
        </div>

        {error && (
          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              color: '#ef4444',
              fontSize: '0.8rem',
            }}
          >
            {error}
          </div>
        )}

        <div>
          <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Base Model Version *
          </label>
          <input
            type="text"
            value={baseVersion}
            onChange={(e) => setBaseVersion(e.target.value)}
            placeholder="e.g. global-v3"
            required
            style={{
              width: '100%',
              padding: '0.6rem 0.8rem',
              borderRadius: '8px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              color: '#f3f4f6',
              fontSize: '0.85rem',
              outline: 'none',
            }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Custom Round ID (Optional)
          </label>
          <input
            type="text"
            value={roundId}
            onChange={(e) => setRoundId(e.target.value)}
            placeholder="Auto-generated if left blank (e.g. ROUND-003)"
            style={{
              width: '100%',
              padding: '0.6rem 0.8rem',
              borderRadius: '8px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              color: '#f3f4f6',
              fontSize: '0.85rem',
              outline: 'none',
            }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Minimum Consensus Quorum: <strong style={{ color: '#38bdf8' }}>{minQuorum} nodes</strong>
          </label>
          <input
            type="range"
            min={1}
            max={3}
            value={minQuorum}
            onChange={(e) => setMinQuorum(parseInt(e.target.value, 10))}
            style={{ width: '100%', accentColor: '#38bdf8' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: '#6b7280' }}>
            <span>1 Node (Lenient)</span>
            <span>2 Nodes (Standard FedAvg)</span>
            <span>3 Nodes (Full Consensus)</span>
          </div>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.8rem', color: '#9ca3af', marginBottom: '0.5rem' }}>
            Participating Municipalities
          </label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {availableNodes.map((node) => (
              <label
                key={node.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.6rem',
                  fontSize: '0.85rem',
                  color: '#e5e7eb',
                  cursor: 'pointer',
                  padding: '0.4rem 0.6rem',
                  borderRadius: '6px',
                  background: selectedNodes.includes(node.id)
                    ? 'rgba(56, 189, 248, 0.08)'
                    : 'transparent',
                }}
              >
                <input
                  type="checkbox"
                  checked={selectedNodes.includes(node.id)}
                  onChange={() => handleToggleNode(node.id)}
                  style={{ accentColor: '#38bdf8' }}
                />
                <span>{node.label}</span>
                <span style={{ fontSize: '0.75rem', color: '#9ca3af', marginLeft: 'auto' }}>
                  [{node.id}]
                </span>
              </label>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
          <Button variant="ghost" onClick={onClose} type="button">
            Cancel
          </Button>
          <Button variant="primary" type="submit" isLoading={isSubmitting}>
            <PlusCircle size={14} style={{ marginRight: '0.4rem' }} />
            Start Training Round
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default InitiateRoundModal;
