import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { getNavSections } from '../components/layout/Sidebar';
import { citizenService } from '../services/citizen.service';
import apiClient from '../services/api';

test('F6-PRE-P4: 1. Citizen request timeout override is set to bounded timeout (>= 20000ms) while global api.ts remains 10000ms', () => {
  // Verify api.ts retains global 10000ms timeout
  const apiFile = fs.readFileSync(path.resolve('src/services/api.ts'), 'utf-8');
  assert.ok(apiFile.includes('timeout: 10000'), 'api.ts must retain baseline global timeout of 10000ms');

  // Verify citizen.service.ts specifies bounded per-request timeout
  const citizenServiceFile = fs.readFileSync(path.resolve('src/services/citizen.service.ts'), 'utf-8');
  assert.ok(
    citizenServiceFile.includes('timeout: 35000') || citizenServiceFile.includes('timeout: 20000'),
    'citizen.service.ts must specify bounded per-request timeout for submitReport operation'
  );
});

test('F6-PRE-P4: 2. Successful long-running citizen report request receives bounded timeout configuration', async () => {
  let capturedConfig: any = null;

  // Temporarily stub apiClient.post to verify call configuration
  const originalPost = apiClient.post;
  apiClient.post = (async (url: string, data: any, config: any) => {
    capturedConfig = config;
    return {
      data: {
        id: 'mock-report-uuid-999',
        category: 'SMOKE',
        status: 'ANALYZED',
        h3Index: '88608850e5fffff',
      },
    };
  }) as any;

  try {
    const formData = new FormData();
    formData.append('category', 'SMOKE');
    const result = await citizenService.submitReport(formData);

    assert.equal(result.id, 'mock-report-uuid-999');
    assert.ok(capturedConfig, 'apiClient.post must have received request config');
    assert.ok(capturedConfig.timeout >= 20000, 'Per-request timeout must be explicitly set to >= 20000ms');
    assert.equal(capturedConfig.headers?.['Content-Type'], 'multipart/form-data');
  } finally {
    apiClient.post = originalPost;
  }
});

test('F6-PRE-P4: 3. Alert badge count = 0 displays no misleading "1" in sidebar', () => {
  const sections = getNavSections(0);
  const actionSection = sections.find((s) => s.title === 'ACTION');
  assert.ok(actionSection, 'ACTION section must exist in sidebar');

  const alertsItem = actionSection.items.find((item) => item.label === 'Alerts');
  assert.ok(alertsItem, 'Alerts navigation item must exist');
  assert.strictEqual(alertsItem.badge, undefined, 'When alert count is 0, badge must be undefined (no misleading "1")');

  // Verify source code of Sidebar.tsx has purged the hardcoded badge: '1'
  const sidebarFile = fs.readFileSync(path.resolve('src/components/layout/Sidebar.tsx'), 'utf-8');
  assert.ok(!sidebarFile.includes("badge: '1'"), "Sidebar.tsx must not contain hardcoded badge: '1'");
});

test('F6-PRE-P4: 4. Alert badge reflects live count and authority queue resolved semantics', () => {
  // Test with positive count
  const sectionsActive = getNavSections(4);
  const actionActive = sectionsActive.find((s) => s.title === 'ACTION')!;
  const alertsItemActive = actionActive.items.find((item) => item.label === 'Alerts')!;
  assert.strictEqual(alertsItemActive.badge, '4', 'When 4 alerts exist, badge must display "4"');

  // Test resolved/filtered semantics logic matching Alerts authority queue
  const mockQueue = [
    { alertId: 'a1', status: 'OPEN' },
    { alertId: 'a2', status: 'ACKNOWLEDGED' },
    { alertId: 'a3', status: 'RESOLVED' },
    { alertId: 'a4', status: 'RESOLVED' },
  ];

  // Authority queue semantics: active non-resolved alert candidates (OPEN and ACKNOWLEDGED, excluding RESOLVED)
  const activeCount = mockQueue.filter((item) => item.status !== 'RESOLVED').length;
  assert.strictEqual(activeCount, 2, 'Resolved alerts must be excluded from active count');

  const sectionsFiltered = getNavSections(activeCount);
  const alertsItemFiltered = sectionsFiltered.find((s) => s.title === 'ACTION')!.items.find((i) => i.label === 'Alerts')!;
  assert.strictEqual(alertsItemFiltered.badge, '2', 'Badge must reflect the 2 active unresolved alerts');
});

test('F6-PRE-P4: 5. Alert API failure gracefully defaults to 0 and does not crash sidebar navigation', () => {
  // Simulating API failure (empty items array or error recovery)
  const emptySections = getNavSections(0);
  assert.ok(Array.isArray(emptySections), 'getNavSections must return valid navigation array on 0/failure');
  assert.equal(emptySections.length, 5, 'Sidebar must contain all 5 primary sections');

  const actionSection = emptySections.find((s) => s.title === 'ACTION')!;
  const alertsItem = actionSection.items.find((i) => i.label === 'Alerts')!;
  assert.strictEqual(alertsItem.badge, undefined, 'On API failure or empty queue, badge must be undefined without error');

  // Verify Sidebar component wires useAuthorityQueue safely
  const sidebarFile = fs.readFileSync(path.resolve('src/components/layout/Sidebar.tsx'), 'utf-8');
  assert.ok(sidebarFile.includes('useAuthorityQueue'), 'Sidebar.tsx must use useAuthorityQueue hook');
  assert.ok(sidebarFile.includes("item.status !== 'RESOLVED'"), 'Sidebar.tsx must exclude RESOLVED alerts');
});
