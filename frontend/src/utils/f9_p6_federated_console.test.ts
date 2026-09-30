import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  getNodeStatusConfig,
  getRoundStatusConfig,
  calculateQuorumProgress,
  formatMetric,
  formatRelativeTime,
  formatNodeList,
  sortModelCatalog,
} from './federatedUtils';
import { GlobalModel, CreateRoundRequest, NodeHeartbeatRequest } from '../types/federated';

describe('F9-P6: React Federated Network Console UI & Visualization Unit Tests', () => {

  // Test 1: Node status classification, health color mapping, and badge rendering logic
  it('Test 1: should correctly map municipal node statuses to badge variants and colors', () => {
    const onlineCfg = getNodeStatusConfig('ONLINE');
    assert.equal(onlineCfg.variant, 'success');
    assert.equal(onlineCfg.label, 'ONLINE');
    assert.equal(onlineCfg.pulse, true);
    assert.equal(onlineCfg.color, '#10b981');

    const trainingCfg = getNodeStatusConfig('TRAINING');
    assert.equal(trainingCfg.variant, 'info');
    assert.equal(trainingCfg.label, 'TRAINING');
    assert.equal(trainingCfg.pulse, true);
    assert.equal(trainingCfg.color, '#38bdf8');

    const offlineCfg = getNodeStatusConfig('OFFLINE');
    assert.equal(offlineCfg.variant, 'danger');
    assert.equal(offlineCfg.label, 'OFFLINE');
    assert.equal(offlineCfg.pulse, false);
    assert.equal(offlineCfg.color, '#ef4444');

    const staleCfg = getNodeStatusConfig('STALE');
    assert.equal(staleCfg.variant, 'warning');
    assert.equal(staleCfg.label, 'STALE');
    assert.equal(staleCfg.pulse, false);

    const unknownCfg = getNodeStatusConfig(null);
    assert.equal(unknownCfg.variant, 'neutral');
    assert.equal(unknownCfg.label, 'UNKNOWN');
  });

  // Test 2: Quorum progress calculation & boundary conditions
  it('Test 2: should compute consensus quorum percentage and threshold satisfaction', () => {
    // 0 of 2 received
    const q0 = calculateQuorumProgress(0, 2);
    assert.equal(q0.percentage, 0);
    assert.equal(q0.isQuorumMet, false);
    assert.equal(q0.received, 0);
    assert.equal(q0.required, 2);

    // 1 of 2 received (50% progress, quorum NOT met)
    const q1 = calculateQuorumProgress(1, 2);
    assert.equal(q1.percentage, 50);
    assert.equal(q1.isQuorumMet, false);

    // 2 of 2 received (100% progress, quorum MET)
    const q2 = calculateQuorumProgress(2, 2);
    assert.equal(q2.percentage, 100);
    assert.equal(q2.isQuorumMet, true);

    // 3 of 2 received (capped at 100% progress, quorum MET)
    const q3 = calculateQuorumProgress(3, 2);
    assert.equal(q3.percentage, 100);
    assert.equal(q3.isQuorumMet, true);
    assert.equal(q3.received, 3);

    // Fallback for minQuorum <= 0
    const qEdge = calculateQuorumProgress(1, 0);
    assert.equal(qEdge.required, 1);
    assert.equal(qEdge.isQuorumMet, true);
  });

  // Test 3: Model validation metrics formatting
  it('Test 3: should accurately format ML validation metrics (MAE, RMSE, ROC-AUC, Brier score)', () => {
    assert.equal(formatMetric(0.41624, 4), '0.4162');
    assert.equal(formatMetric(0.46231, 4), '0.4623');
    assert.equal(formatMetric(0.7287, 4), '0.7287');
    assert.equal(formatMetric(0.2137, 4), '0.2137');
    assert.equal(formatMetric(0.5, 2), '0.50');
    assert.equal(formatMetric(undefined, 4), 'N/A');
    assert.equal(formatMetric(null, 4), 'N/A');
    assert.equal(formatMetric(NaN, 4), 'N/A');
  });

  // Test 4: Model lineage ordering and active consensus version prioritization
  it('Test 4: should sort model lineage catalog prioritizing active consensus model first', () => {
    const sampleModels: GlobalModel[] = [
      {
        modelVersion: 'global-v1',
        isActive: false,
        participatingNodes: ['PUNE', 'MUMBAI', 'DELHI'],
        totalSamples: 0,
        metrics: {},
        createdAt: '2026-09-29T23:35:22Z',
      },
      {
        modelVersion: 'global-v3',
        isActive: true,
        participatingNodes: ['PUNE', 'MUMBAI'],
        totalSamples: 2400,
        metrics: { mae: 0.4162 },
        createdAt: '2026-09-30T00:04:48Z',
      },
      {
        modelVersion: 'global-v2',
        isActive: false,
        participatingNodes: ['PUNE', 'MUMBAI', 'DELHI'],
        totalSamples: 3000,
        metrics: { mae: 0.3477 },
        createdAt: '2026-09-30T00:04:47Z',
      },
    ];

    const sorted = sortModelCatalog(sampleModels);
    assert.equal(sorted[0].modelVersion, 'global-v3');
    assert.equal(sorted[0].isActive, true);
    assert.equal(sorted[1].modelVersion, 'global-v2');
    assert.equal(sorted[2].modelVersion, 'global-v1');
  });

  // Test 5: Round lifecycle state transitions and terminal indicators
  it('Test 5: should map round lifecycle state configurations and identify terminal states', () => {
    const completedCfg = getRoundStatusConfig('COMPLETED');
    assert.equal(completedCfg.variant, 'success');
    assert.equal(completedCfg.isTerminal, true);

    const failedCfg = getRoundStatusConfig('FAILED');
    assert.equal(failedCfg.variant, 'danger');
    assert.equal(failedCfg.isTerminal, true);

    const aggregatingCfg = getRoundStatusConfig('AGGREGATING');
    assert.equal(aggregatingCfg.variant, 'info');
    assert.equal(aggregatingCfg.isTerminal, false);

    const collectingCfg = getRoundStatusConfig('UPDATES_COLLECTING');
    assert.equal(collectingCfg.variant, 'warning');
    assert.equal(collectingCfg.isTerminal, false);

    const createdCfg = getRoundStatusConfig('CREATED');
    assert.equal(createdCfg.variant, 'neutral');
    assert.equal(createdCfg.isTerminal, false);
  });

  // Test 6: API payload structure serialization
  it('Test 6: should properly structure round initiation and heartbeat payloads', () => {
    const roundPayload: CreateRoundRequest = {
      roundId: 'ROUND-004',
      baseModelVersion: 'global-v3',
      minQuorum: 2,
      participatingNodes: ['PUNE', 'MUMBAI'],
    };
    assert.equal(roundPayload.roundId, 'ROUND-004');
    assert.equal(roundPayload.baseModelVersion, 'global-v3');
    assert.equal(roundPayload.minQuorum, 2);
    assert.deepEqual(roundPayload.participatingNodes, ['PUNE', 'MUMBAI']);

    const heartbeatPayload: NodeHeartbeatRequest = {
      status: 'ONLINE',
      modelVersion: 'global-v3',
    };
    assert.equal(heartbeatPayload.status, 'ONLINE');
    assert.equal(heartbeatPayload.modelVersion, 'global-v3');
  });

  // Test 7: Multi-city participating node string formatting
  it('Test 7: should format participating node arrays into clean joined strings', () => {
    assert.equal(formatNodeList(['PUNE', 'MUMBAI', 'DELHI']), 'PUNE, MUMBAI, DELHI');
    assert.equal(formatNodeList(['PUNE', 'MUMBAI']), 'PUNE, MUMBAI');
    assert.equal(formatNodeList(['PUNE']), 'PUNE');
    assert.equal(formatNodeList([]), 'None');
    assert.equal(formatNodeList(null), 'None');
  });

  // Test 8: Relative time formatting
  it('Test 8: should correctly compute human-readable relative time strings', () => {
    const now = new Date();
    assert.equal(formatRelativeTime(now.toISOString()), 'Just now');

    const twoMinutesAgo = new Date(now.getTime() - 2 * 60 * 1000);
    assert.equal(formatRelativeTime(twoMinutesAgo.toISOString()), '2m ago');

    const threeHoursAgo = new Date(now.getTime() - 3 * 3600 * 1000);
    assert.equal(formatRelativeTime(threeHoursAgo.toISOString()), '3h ago');

    assert.equal(formatRelativeTime(null), 'Never');
    assert.equal(formatRelativeTime('invalid-date'), 'Invalid date');
  });

});
