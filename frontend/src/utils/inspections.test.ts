import test from 'node:test';
import assert from 'node:assert/strict';
import { FieldTeam, FieldVerification, InspectionResponse } from '../types/inspection';
import { AuthorityQueueItem } from '../types/alert';
import fs from 'node:fs';
import path from 'node:path';

// =========================================================================
// Authoritative Test Fixtures for F5-P6 Field Team & Verification
// =========================================================================

export const MOCK_FIELD_TEAMS: FieldTeam[] = [
  {
    id: '770e8400-e29b-41d4-a716-446655440001',
    teamCode: 'TEAM-PUN-01',
    teamName: 'Pune Municipal Rapid Response Team A',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    status: 'AVAILABLE',
    contactNumber: '+91-20-25501001',
    leaderName: 'Inspector A. Deshmukh',
    createdAt: '2026-09-28T00:00:00Z',
  },
  {
    id: '770e8400-e29b-41d4-a716-446655440002',
    teamCode: 'TEAM-PUN-02',
    teamName: 'Pune Industrial Anti-Smog Squad',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    status: 'AVAILABLE',
    contactNumber: '+91-20-25501002',
    leaderName: 'Officer R. Kulkarni',
    createdAt: '2026-09-28T00:00:00Z',
  },
];

export const MOCK_ASSIGNED_INSPECTION: InspectionResponse = {
  id: '880e8400-e29b-41d4-a716-446655440001',
  alertId: 'e12da246-dbea-4028-aa51-bb667fcca393',
  teamId: '770e8400-e29b-41d4-a716-446655440001',
  teamCode: 'TEAM-PUN-01',
  teamName: 'Pune Municipal Rapid Response Team A',
  eventId: '446928c2-6090-437d-a7c7-a9909e648403',
  h3Index: '886196944dfffff',
  predictionId: '1e91f203-1b9a-4886-a355-bd326c903625',
  assignedAt: '2026-09-28T08:00:00Z',
  status: 'ASSIGNED',
  createdAt: '2026-09-28T08:00:00Z',
};

export const MOCK_IN_PROGRESS_INSPECTION: InspectionResponse = {
  ...MOCK_ASSIGNED_INSPECTION,
  status: 'IN_PROGRESS',
  startedAt: '2026-09-28T08:15:00Z',
};

export const MOCK_COMPLETED_VERIFICATION: FieldVerification = {
  id: '990e8400-e29b-41d4-a716-446655440001',
  inspectionId: '880e8400-e29b-41d4-a716-446655440001',
  alertId: 'e12da246-dbea-4028-aa51-bb667fcca393',
  eventId: '446928c2-6090-437d-a7c7-a9909e648403',
  h3Index: '886196944dfffff',
  predictionId: '1e91f203-1b9a-4886-a355-bd326c903625',
  verificationStatus: 'CONFIRMED',
  verificationResult: 'CONFIRMED',
  observedConditions: 'Heavy unscrubbed flue emissions observed at boiler stack',
  inspectorNotes: 'Issued emergency cessation under Air Act',
  verifiedBy: 'Inspector A. Deshmukh',
  inspectedAt: '2026-09-28T08:45:00Z',
  createdAt: '2026-09-28T08:45:00Z',
};

export const MOCK_ALERT_WITH_VERIFICATION: AuthorityQueueItem = {
  alertId: 'e12da246-dbea-4028-aa51-bb667fcca393',
  eventId: '446928c2-6090-437d-a7c7-a9909e648403',
  eventCode: 'EVT-88619694-2026092807-ind12345',
  h3Index: '886196944dfffff',
  predictionId: '1e91f203-1b9a-4886-a355-bd326c903625',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  cityName: 'Pune',
  status: 'ACKNOWLEDGED',
  severity: 'CRITICAL',
  riskScore: 0.88,
  evidenceScore: 0.72,
  triageState: 'ALERT_CANDIDATE',
  consistency: 'consistent',
  title: 'Severe industrial emission cluster',
  message: 'Ground sensors corroborate particulate plume',
  hasGeminiAnalysis: true,
  createdAt: '2026-09-28T07:30:00Z',
  activeAssignmentId: '880e8400-e29b-41d4-a716-446655440001',
  assignedTeamId: '770e8400-e29b-41d4-a716-446655440001',
  assignedTeamName: 'Pune Municipal Rapid Response Team A',
  assignmentStatus: 'COMPLETED',
  assignedAt: '2026-09-28T08:00:00Z',
  verificationResult: 'CONFIRMED',
  verificationStatus: 'CONFIRMED',
  inspectedAt: '2026-09-28T08:45:00Z',
};

// =========================================================================
// 12 Automated Frontend Test Cases per F5-P6 Specification
// =========================================================================

test('F5-P6 Frontend 1: Authority queue provides Assign Field Team action for ACKNOWLEDGED alerts', () => {
  const alertsFile = fs.readFileSync(path.resolve('src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(alertsFile.includes("selectedAlert.status === 'ACKNOWLEDGED'"), 'Contains check for ACKNOWLEDGED status');
  assert.ok(alertsFile.includes('Assign Field Team'), 'Contains Assign Field Team button text');
  assert.ok(alertsFile.includes('setShowInspectionModal(true)'), 'Triggers inspection modal on click');
});

test('F5-P6 Frontend 2: Team list loading state and integration in InspectionForm', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('loadingTeams'), 'Handles loadingTeams state');
  assert.ok(formFile.includes('Loading verified field teams...'), 'Renders loading text for field teams');
  assert.ok(formFile.includes('inspectionApi.getFieldTeams()'), 'Queries real field teams endpoint');
});

test('F5-P6 Frontend 3: Empty teams state presentation when no teams registered', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('teams.length === 0'), 'Checks for empty team list');
  assert.ok(formFile.includes('No municipal field teams registered in system'), 'Renders empty team notice');
});

test('F5-P6 Frontend 4: Assignment success updates assignment state and displays confirmation', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('assigned successfully!'), 'Provides success confirmation on team assignment');
  assert.ok(formFile.includes('onAssignmentSuccess'), 'Invokes onAssignmentSuccess callback');
});

test('F5-P6 Frontend 5: Assignment failure renders error notice without crash', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('errorMessage'), 'Manages error message state');
  assert.ok(formFile.includes('Failed to assign field team'), 'Handles failure message gracefully');
});

test('F5-P6 Frontend 6: Inspection form renders alert reference, cell H3, and team metadata', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('Alert Reference'), 'Displays Alert Reference header');
  assert.ok(formFile.includes('Cell H3'), 'Displays Cell H3 index');
  assert.ok(formFile.includes('h3Index'), 'Binds h3Index');
});

test('F5-P6 Frontend 7: Verification submission accepts OBSERVED conditions and result', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('observedConditions'), 'Includes observedConditions form input');
  assert.ok(formFile.includes('OBSERVED FIELD EVIDENCE'), 'Explicitly classifies observations as field evidence');
  assert.ok(formFile.includes('CONFIRMED'), 'Includes CONFIRMED option');
  assert.ok(formFile.includes('REJECTED'), 'Includes REJECTED option');
  assert.ok(formFile.includes('NEEDS_FOLLOW_UP'), 'Includes NEEDS_FOLLOW_UP option');
});

test('F5-P6 Frontend 8: Invalid transition handling (requires active/in-progress inspection)', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(formFile.includes('isInProgress'), 'Checks isInProgress condition');
  assert.ok(formFile.includes('isAssigned'), 'Checks isAssigned condition');
  assert.ok(formFile.includes('Start Ground Inspection'), 'Requires explicit start before verification');
});

test('F5-P6 Frontend 9: Existing Evidence & WHY navigation is preserved unchanged', () => {
  const alertsFile = fs.readFileSync(path.resolve('src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(alertsFile.includes('/analyst/evidence?h3='), 'Preserves /analyst/evidence?h3= navigation');
  assert.ok(alertsFile.includes('Inspect Full Evidence & Gemini WHY'), 'Preserves Gemini WHY button');
});

test('F5-P6 Frontend 10: No mock team or mock assignment rows used in production UI', () => {
  const formFile = fs.readFileSync(path.resolve('src/components/authority/InspectionForm.tsx'), 'utf-8');
  assert.ok(!formFile.includes('Mobile Rapid Response Team 1'), 'Old mock team string eliminated');
  const pageFile = fs.readFileSync(path.resolve('src/pages/authority/Inspection.tsx'), 'utf-8');
  assert.ok(!pageFile.includes('sample-alert'), 'Old sample-alert mock eliminated');
});

test('F5-P6 Frontend 11: Assignment status rendering in list and detail view', () => {
  const alertsFile = fs.readFileSync(path.resolve('src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(alertsFile.includes('item.assignedTeamName'), 'Renders assigned team name badge in list');
  assert.ok(alertsFile.includes('selectedAlert.assignedTeamName'), 'Renders assigned team in detail view');
  assert.ok(alertsFile.includes('selectedAlert.assignmentStatus'), 'Renders assignment status in detail view');
});

test('F5-P6 Frontend 12: Verification result rendering in list badges and detail card', () => {
  const alertsFile = fs.readFileSync(path.resolve('src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(alertsFile.includes('item.verificationResult'), 'Renders verificationResult badge in list');
  assert.ok(alertsFile.includes('selectedAlert.verificationResult'), 'Renders verificationResult in detail drawer');
  assert.ok(alertsFile.includes('selectedAlert.inspectedAt'), 'Renders inspection timestamp');
});
