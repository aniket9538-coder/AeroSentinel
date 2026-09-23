import React from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { CheckCircle2 } from 'lucide-react';

interface ResolutionPanelProps {
  alertId?: string;
  onResolve: (alertId: string) => void;
  isLoading?: boolean;
}

export const ResolutionPanel: React.FC<ResolutionPanelProps> = ({ alertId, onResolve, isLoading = false }) => {
  return (
    <Card title="Incident Resolution">
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', padding: '0.5rem 0' }}>
        <CheckCircle2 size={32} color="#10b981" />
        <div>
          <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#f3f4f6' }}>Close Incident Lifecycle</div>
          <div style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
            Mark alert as officially verified, mitigated, and resolved.
          </div>
        </div>
      </div>
      <div style={{ marginTop: '1rem' }}>
        <Button
          variant="primary"
          style={{ width: '100%', background: '#059669' }}
          disabled={!alertId}
          onClick={() => alertId && onResolve(alertId)}
          isLoading={isLoading}
        >
          Mark Incident Resolved
        </Button>
      </div>
    </Card>
  );
};
