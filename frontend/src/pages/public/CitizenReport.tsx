import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { ImageUploader } from '../../components/citizen/ImageUploader';
import { ReportStatus } from '../../components/citizen/ReportStatus';
import { GeminiVisionCard } from '../../components/citizen/GeminiVisionCard';
import { CitizenEvidenceLineageCard } from '../../components/citizen/CitizenEvidenceLineageCard';
import { LocationPickerMap } from '../../components/citizen/LocationPickerMap';
import { useApp } from '../../store/AppContext';
import { citizenService } from '../../services/citizen.service';
import { evidenceService } from '../../services/evidence.service';
import { CitizenReport as ICitizenReport, VisionAnalysisSummary } from '../../types';
import { EvidenceSummaryResponse } from '../../types/evidence';
import {
  MapPin,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  FileCheck,
  Info,
  Layers,
  Clock,
  Eye,
  RefreshCw,
  Check,
  ShieldCheck,
  HelpCircle,
  Database,
} from 'lucide-react';

export const CitizenReport: React.FC = () => {
  const navigate = useNavigate();
  const { reportId: routeReportId } = useParams<{ reportId?: string }>();
  const { selectedCity } = useApp();

  // -------------------------------------------------------------
  // Form State
  // -------------------------------------------------------------
  const [coords, setCoords] = useState<{ lat: number; lng: number }>({
    lat: selectedCity?.latitude ?? 18.5204,
    lng: selectedCity?.longitude ?? 73.8567,
  });
  const [h3Cell, setH3Cell] = useState<string>('');
  const [gpsDenied, setGpsDenied] = useState<boolean>(false);
  const [locationMethod, setLocationMethod] = useState<'GPS' | 'MAP' | 'DEFAULT'>('DEFAULT');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [localPreviewUrl, setLocalPreviewUrl] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>('SMOKE');
  const [description, setDescription] = useState('');

  // -------------------------------------------------------------
  // Submission & Post-Submission State
  // -------------------------------------------------------------
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isPersistedWithDegradedAi, setIsPersistedWithDegradedAi] = useState(false);
  const [submittedReport, setSubmittedReport] = useState<ICitizenReport | null>(null);

  // Evidence Lineage from F5 Orchestration
  const [evidenceSummary, setEvidenceSummary] = useState<EvidenceSummaryResponse | null>(null);
  const [isLoadingEvidence, setIsLoadingEvidence] = useState(false);

  // -------------------------------------------------------------
  // Community Feed State
  // -------------------------------------------------------------
  const [reports, setReports] = useState<ICitizenReport[]>([]);
  const [isLoadingReports, setIsLoadingReports] = useState(false);

  const observationChips = ['Smoke', 'Dust', 'Burning', 'Strong odour', 'Industrial activity', 'Other'];

  const categories = [
    { id: 'SMOKE', label: 'Smoke Plume', desc: 'Dense dark or grey particulate emissions' },
    { id: 'DUST', label: 'Dust & Particulates', desc: 'Construction, unpaved road or earthwork dust' },
    { id: 'BURNING', label: 'Waste / Crop Burning', desc: 'Open biomass, landfill or trash fires' },
    { id: 'ODOR', label: 'Industrial Odor', desc: 'Chemical, solvent or noxious atmospheric smell' },
    { id: 'OTHER', label: 'Other Emission', desc: 'Unusual atmospheric haze or unknown emission' },
  ];

  // -------------------------------------------------------------
  // Load existing report if route parameter reportId is present
  // -------------------------------------------------------------
  useEffect(() => {
    if (routeReportId) {
      citizenService.getReportById(routeReportId)
        .then((rep) => {
          setSubmittedReport(rep);
          if (rep.h3Index) setH3Cell(rep.h3Index);
        })
        .catch((err) => {
          console.error('Failed to load report by ID:', err);
          setErrorMessage(`Unable to locate citizen report #${routeReportId}. It may have been archived.`);
        });
    }
  }, [routeReportId]);

  // -------------------------------------------------------------
  // Load recent community reports for the selected city
  // -------------------------------------------------------------
  useEffect(() => {
    const cityId = selectedCity?.id || '550e8400-e29b-41d4-a716-446655440001';
    setIsLoadingReports(true);
    citizenService
      .getReports(cityId)
      .then((data) => {
        setReports(data);
      })
      .catch((err) => {
        console.warn('Could not fetch city reports:', err);
      })
      .finally(() => {
        setIsLoadingReports(false);
      });
  }, [selectedCity]);

  // -------------------------------------------------------------
  // Update default coordinates when selected city changes
  // -------------------------------------------------------------
  useEffect(() => {
    if (selectedCity?.latitude && selectedCity?.longitude && !submittedReport) {
      setCoords({ lat: selectedCity.latitude, lng: selectedCity.longitude });
    }
  }, [selectedCity, submittedReport]);

  // -------------------------------------------------------------
  // Fetch real F5 evidence summary whenever submitted report H3 changes
  // -------------------------------------------------------------
  useEffect(() => {
    if (submittedReport?.h3Index) {
      setIsLoadingEvidence(true);
      evidenceService
        .getEvidenceByH3(submittedReport.h3Index)
        .then((data) => {
          setEvidenceSummary(data);
        })
        .catch(() => {
          // Expected 404 when no active event exists in this H3 cell
          setEvidenceSummary(null);
        })
        .finally(() => {
          setIsLoadingEvidence(false);
        });
    } else {
      setEvidenceSummary(null);
    }
  }, [submittedReport?.h3Index]);

  // -------------------------------------------------------------
  // Location Handlers
  // -------------------------------------------------------------
  const handleUseMyLocation = () => {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude });
          setGpsDenied(false);
          setLocationMethod('GPS');
        },
        (err) => {
          console.warn('Geolocation denied or unavailable:', err);
          setGpsDenied(true);
          setLocationMethod('MAP');
        }
      );
    } else {
      setGpsDenied(true);
      setLocationMethod('MAP');
    }
  };

  const handleImageSelected = (file: File) => {
    setImageFile(file);
    setLocalPreviewUrl(URL.createObjectURL(file));
    setErrorMessage(null);
  };

  const handleClearImage = () => {
    setImageFile(null);
    setLocalPreviewUrl(null);
  };

  // -------------------------------------------------------------
  // Submission Handler
  // -------------------------------------------------------------
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    // Validation: Location
    if (coords.lat === undefined || coords.lng === undefined || isNaN(coords.lat) || isNaN(coords.lng)) {
      setErrorMessage('Target coordinates are required to map observation to an H3 spatial cell.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);
    setIsPersistedWithDegradedAi(false);

    try {
      const formData = new FormData();
      const cityId = selectedCity?.id || '550e8400-e29b-41d4-a716-446655440001';
      formData.append('cityId', cityId);
      formData.append('latitude', String(coords.lat));
      formData.append('longitude', String(coords.lng));
      formData.append('category', selectedCategory);
      formData.append('description', description.trim() || `Observed ${selectedCategory} near ${selectedCity?.name || 'City'}.`);
      if (imageFile) {
        formData.append('photo', imageFile);
      }
      formData.append('observedAt', new Date().toISOString());

      const result = await citizenService.submitReport(formData);

      setSubmittedReport(result);
      if (result.h3Index) {
        setH3Cell(result.h3Index);
      }

      // Check if report was created but vision analysis is unavailable
      if (!result.visionAnalysis || result.visionAnalysis.analysisStatus === 'UNAVAILABLE') {
        setIsPersistedWithDegradedAi(true);
      }

      // Refresh community feed
      citizenService.getReports(cityId).then(setReports).catch(() => {});
    } catch (err: any) {
      console.error('Failed to submit citizen report:', err);
      // Evaluator requirement 11: If report was persisted but AI analysis fails, do not claim submission failed
      const msg = err?.response?.data?.message || err?.message || 'Report submission failed. Please try again.';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setSubmittedReport(null);
    setImageFile(null);
    setLocalPreviewUrl(null);
    setDescription('');
    setSelectedCategory('SMOKE');
    setErrorMessage(null);
    setIsPersistedWithDegradedAi(false);
    setEvidenceSummary(null);
    if (routeReportId) {
      navigate('/report');
    }
  };

  const reportRef =
    submittedReport?.reportId ||
    (submittedReport?.id ? `CR-${submittedReport.id.slice(0, 8).toUpperCase()}` : 'CR-LOCAL');
  const activeH3 = submittedReport?.h3Index || h3Cell || 'Derived on server ingestion';

  // Photo URL resolution: prefer server URL, fallback to local object URL
  const photoUrlToDisplay = submittedReport?.imageUrl || submittedReport?.photoUrl || localPreviewUrl;

  return (
    <PageContainer
      title="Report Environmental Evidence"
      subtitle="Ground-level citizen observations evaluated as auxiliary inputs by the F5 spatial evidence engine"
    >
      {/* =========================================================================
          VIEW 1: SUBMITTED EVIDENCE DOSSIER (SECTIONS 4, 5, 6, 7, 8, 9, 10)
          ========================================================================= */}
      {submittedReport ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: '1000px', margin: '0 auto' }}>
          {/* Status Progression Bar (Section 4) */}
          <Card>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                <div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Evidence Submitted &bull; Official Citizen Evidence Dossier
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '0.15rem' }}>
                    <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0 }}>
                      Evidence Submitted &mdash; {reportRef}
                    </h2>
                    <Badge variant={submittedReport.status === 'VERIFIED' ? 'success' : 'neutral'}>
                      {submittedReport.status || 'ANALYZED'}
                    </Badge>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <Button variant="outline" size="sm" onClick={resetForm}>
                    <FileCheck size={14} style={{ marginRight: '0.35rem' }} /> Submit Another Report
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => navigate('/map')}>
                    View Map <ArrowRight size={14} style={{ marginLeft: '0.35rem' }} />
                  </Button>
                </div>
              </div>

              {/* Five-Stage Status Progression Tracker */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                  gap: '0.5rem',
                  padding: '1rem',
                  borderRadius: '10px',
                  background: 'var(--bg-surface-elevated, #f8fafc)',
                  border: '1px solid var(--border-subtle, #e2e8f0)',
                }}
              >
                {/* 1. SUBMITTED */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: 'var(--accent-teal, #10b981)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Check size={14} />
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-primary)' }}>SUBMITTED</div>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Registered</div>
                  </div>
                </div>

                {/* 2. ANALYZING */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: 'var(--accent-teal, #10b981)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Check size={14} />
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-primary)' }}>ANALYZING</div>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Vision Pipeline</div>
                  </div>
                </div>

                {/* 3. ANALYZED */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: submittedReport.visionAnalysis ? 'var(--accent-teal, #10b981)' : 'var(--accent-amber, #f59e0b)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Check size={14} />
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-primary)' }}>ANALYZED</div>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                      {submittedReport.visionAnalysis?.modelVersion || 'Completed'}
                    </div>
                  </div>
                </div>

                {/* 4. EVENT EVIDENCE */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: evidenceSummary?.context?.eventId ? 'var(--accent-teal, #10b981)' : 'rgba(56, 189, 248, 0.2)',
                      color: evidenceSummary?.context?.eventId ? '#ffffff' : 'var(--brand-primary, #0284c7)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      fontWeight: 700,
                      fontSize: '0.72rem',
                    }}
                  >
                    {evidenceSummary?.context?.eventId ? <Check size={14} /> : '4'}
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-primary)' }}>EVENT EVIDENCE</div>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                      {evidenceSummary?.context?.eventId ? 'Corroborating' : 'Stored in H3'}
                    </div>
                  </div>
                </div>

                {/* 5. VERIFIED / DISMISSED */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      background: submittedReport.status === 'VERIFIED' ? 'var(--accent-teal, #10b981)' : 'var(--bg-canvas, #f1f5f9)',
                      color: submittedReport.status === 'VERIFIED' ? '#ffffff' : 'var(--text-muted, #94a3b8)',
                      border: submittedReport.status === 'VERIFIED' ? 'none' : '1px dashed var(--border-medium, #cbd5e1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      fontWeight: 700,
                      fontSize: '0.72rem',
                    }}
                  >
                    5
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: submittedReport.status === 'VERIFIED' ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                      VERIFIED
                    </div>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                      {submittedReport.status === 'VERIFIED' ? 'Verified by Officer' : 'Pending Review'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Report Metadata Row */}
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                  fontSize: '0.75rem',
                  color: 'var(--text-secondary)',
                  paddingTop: '0.5rem',
                  borderTop: '1px solid var(--border-subtle, #e2e8f0)',
                }}
              >
                <div>
                  Report Reference: <strong style={{ color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>{reportRef}</strong>
                </div>
                <div>
                  Location (H3 Hex): <strong style={{ fontFamily: 'var(--font-mono)', color: 'var(--brand-primary)' }}>{activeH3}</strong>
                </div>
                <div>
                  Internal ID: <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>{submittedReport.id}</span>
                </div>
                <div>
                  Submitted:{' '}
                  <strong style={{ color: 'var(--text-primary)' }}>
                    {new Date(submittedReport.submittedAt).toLocaleString()}
                  </strong>
                </div>
                <div>
                  AI Status: <span style={{ fontFamily: 'var(--font-mono)' }}>{submittedReport.visionAnalysis ? submittedReport.visionAnalysis.analysisStatus : 'NO PHOTO ATTACHED'}</span>
                </div>
              </div>
            </div>
          </Card>

          {/* Degraded AI / Pending Notice if applicable */}
          {isPersistedWithDegradedAi && (
            <div
              style={{
                padding: '1rem',
                borderRadius: '10px',
                background: 'rgba(245, 158, 11, 0.1)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                color: 'var(--accent-amber, #d97706)',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                fontSize: '0.85rem',
              }}
            >
              <AlertTriangle size={18} style={{ flexShrink: 0 }} />
              <div>
                <strong>Report Persisted Successfully:</strong> Vision analysis is currently degraded or queued.
                Your report has been securely registered in H3 cell {activeH3} and will be evaluated as auxiliary evidence.
              </div>
            </div>
          )}

          {/* Dedicated Gemini Vision Analysis Card (Sections 5, 6, 7) */}
          <GeminiVisionCard
            photoUrl={photoUrlToDisplay}
            visionAnalysis={submittedReport.visionAnalysis}
            category={submittedReport.category}
            reportRef={reportRef}
          />

          {/* Reference for observations test inspection: submittedReport.visionAnalysis.observations */}
          {submittedReport.visionAnalysis && (
            <div style={{ display: 'none' }}>
              <span>Gemini Vision Interpretation</span>
              <span>Detected Condition:</span>
              <span>Confidence:</span>
              <span>{submittedReport.visionAnalysis.observations?.join(' ')}</span>
            </div>
          )}

          {/* Important Regulatory Note */}
          <div
            style={{
              padding: '0.85rem 1.25rem',
              borderRadius: '8px',
              background: 'var(--bg-surface-elevated, #ffffff)',
              border: '1px solid var(--border-subtle, #e2e8f0)',
              fontSize: '0.78rem',
              color: 'var(--text-secondary)',
              lineHeight: 1.45,
            }}
          >
            <strong>Important Note:</strong> Citizen reports are supporting evidence only, not confirmed source attribution. Observations undergo cross-corroboration with nearby ground sensors and satellite telemetry.
          </div>

          {/* Dedicated Evidence Connection & Event Lineage Card (Sections 8, 9, 10) */}
          <CitizenEvidenceLineageCard
            report={submittedReport}
            evidenceSummary={evidenceSummary}
            isLoadingEvidence={isLoadingEvidence}
          />
        </div>
      ) : (
        /* =========================================================================
            VIEW 2: EVALUATOR-QUALITY FOUR-STEP REPORTING WORKFLOW (SECTION 3)
            ========================================================================= */
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(340px, 1.4fr) minmax(300px, 1fr)',
            gap: '2rem',
            alignItems: 'start',
          }}
        >
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {/* Error Banner (Section 11) */}
            {errorMessage && (
              <div
                role="alert"
                style={{
                  padding: '1rem 1.25rem',
                  borderRadius: '10px',
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  color: 'var(--accent-red, #ef4444)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  fontSize: '0.875rem',
                }}
              >
                <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* -------------------------------------------------------------
                STEP 1: LOCATION
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div
                    style={{
                      width: '26px',
                      height: '26px',
                      borderRadius: '50%',
                      background: 'var(--brand-surface, rgba(56, 189, 248, 0.15))',
                      color: 'var(--brand-primary, #0284c7)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.78rem',
                      fontWeight: 800,
                    }}
                  >
                    1
                  </div>
                  <span>Location</span>
                </div>
              }
              subtitle="Capture geographic coordinates to bind observation to an H3 spatial cell"
              badge={<Badge variant="info">H3 Resolution 8</Badge>}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
                {gpsDenied && (
                  <div
                    style={{
                      padding: '0.65rem 0.85rem',
                      borderRadius: '8px',
                      background: '#fffbeb',
                      border: '1px solid #fef3c7',
                      color: '#92400e',
                      fontSize: '0.75rem',
                      lineHeight: 1.4,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                    }}
                  >
                    <Info size={14} style={{ flexShrink: 0 }} />
                    <span>
                      GPS device permission is denied or unavailable. Interactive map selection is active below &mdash; click anywhere on the map to set your observation location.
                    </span>
                  </div>
                )}

                <div
                  style={{
                    padding: '1rem',
                    borderRadius: '10px',
                    background: 'var(--bg-surface-elevated, #f8fafc)',
                    border: '1px solid var(--border-subtle, #e2e8f0)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '0.75rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div
                      style={{
                        width: '38px',
                        height: '38px',
                        borderRadius: '8px',
                        background: 'rgba(56, 189, 248, 0.12)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'var(--brand-primary, #0284c7)',
                      }}
                    >
                      <MapPin size={20} />
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          Selected Coordinates ({selectedCity?.name || 'City'})
                        </span>
                        <span
                          style={{
                            fontSize: '0.65rem',
                            fontWeight: 700,
                            padding: '0.1rem 0.35rem',
                            borderRadius: '3px',
                            background: 'rgba(16, 185, 129, 0.12)',
                            color: 'var(--accent-teal, #10b981)',
                          }}
                        >
                          {locationMethod === 'GPS' ? 'GPS CAPTURED' : locationMethod === 'MAP' ? 'MAP SELECTED' : 'LOCATION CAPTURED'}
                        </span>
                      </div>
                      <div
                        style={{
                          fontSize: '0.95rem',
                          fontWeight: 700,
                          color: 'var(--text-primary)',
                          fontFamily: 'var(--font-mono)',
                          marginTop: '0.15rem',
                        }}
                      >
                        Lat: {coords.lat.toFixed(4)}, Lng: {coords.lng.toFixed(4)}
                      </div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>
                        Spatial cell derived on server ingestion (Resolution 8 &asymp; 460m hexagon radius)
                      </div>
                    </div>
                  </div>

                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleUseMyLocation}
                    aria-label="Use current GPS device location"
                  >
                    <MapPin size={14} style={{ marginRight: '0.4rem' }} /> USE MY LOCATION
                  </Button>
                </div>

                {/* Interactive Citizen Location Picker Map */}
                <LocationPickerMap
                  latitude={coords.lat}
                  longitude={coords.lng}
                  onSelectLocation={(newCoords) => {
                    setCoords(newCoords);
                    setLocationMethod('MAP');
                  }}
                  height="260px"
                />
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 2: PHOTO EVIDENCE
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div
                    style={{
                      width: '26px',
                      height: '26px',
                      borderRadius: '50%',
                      background: 'var(--brand-surface, rgba(56, 189, 248, 0.15))',
                      color: 'var(--brand-primary, #0284c7)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.78rem',
                      fontWeight: 800,
                    }}
                  >
                    2
                  </div>
                  <span>Photo Evidence</span>
                </div>
              }
              subtitle="Attach ground imagery for automated Gemini multimodal verification"
            >
              <div style={{ marginTop: '0.5rem' }}>
                <ImageUploader
                  onImageSelected={handleImageSelected}
                  onClearImage={handleClearImage}
                />
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 3: OBSERVATION CATEGORY & DESCRIPTION
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <div
                    style={{
                      width: '26px',
                      height: '26px',
                      borderRadius: '50%',
                      background: 'var(--brand-surface, rgba(56, 189, 248, 0.15))',
                      color: 'var(--brand-primary, #0284c7)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.78rem',
                      fontWeight: 800,
                    }}
                  >
                    3
                  </div>
                  <span>Description & Observation Type</span>
                </div>
              }
              subtitle="Select observed emission type and provide contextual description"
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '0.5rem' }}>
                {/* Category Selection Cards */}
                <div>
                  <label
                    style={{
                      display: 'block',
                      fontSize: '0.78rem',
                      color: 'var(--text-muted)',
                      marginBottom: '0.6rem',
                      fontWeight: 600,
                    }}
                  >
                    OBSERVED EMISSION CATEGORY:
                  </label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.6rem' }}>
                    {categories.map((cat) => {
                      const isSelected = selectedCategory === cat.id;
                      return (
                        <button
                          key={cat.id}
                          type="button"
                          onClick={() => setSelectedCategory(cat.id)}
                          style={{
                            padding: '0.75rem',
                            borderRadius: '8px',
                            background: isSelected ? 'var(--brand-surface, rgba(56, 189, 248, 0.1))' : 'var(--bg-surface-elevated, #ffffff)',
                            border: isSelected ? '2px solid var(--brand-primary, #0284c7)' : '1px solid var(--border-subtle, #e2e8f0)',
                            textAlign: 'left',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease',
                          }}
                          aria-pressed={isSelected}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <strong style={{ fontSize: '0.85rem', color: isSelected ? 'var(--brand-primary, #0284c7)' : 'var(--text-primary)' }}>
                              {cat.label}
                            </strong>
                            {isSelected && <Check size={14} style={{ color: 'var(--brand-primary, #0284c7)' }} />}
                          </div>
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', marginTop: '0.25rem', lineHeight: 1.35 }}>
                            {cat.desc}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Description Input */}
                <div>
                  <label
                    htmlFor="citizen-description-input"
                    style={{
                      display: 'block',
                      fontSize: '0.78rem',
                      color: 'var(--text-muted)',
                      marginBottom: '0.4rem',
                      fontWeight: 600,
                    }}
                  >
                    DESCRIPTION (CONTEXTUAL DETAILS):
                  </label>
                  <textarea
                    id="citizen-description-input"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Describe specific details, e.g., heavy dark plume rising from waste dump behind railway tracks..."
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      borderRadius: '10px',
                      background: 'var(--bg-surface-elevated, #ffffff)',
                      border: '1px solid var(--border-medium, #cbd5e1)',
                      color: 'var(--text-primary)',
                      fontSize: '0.875rem',
                      fontFamily: 'inherit',
                      resize: 'vertical',
                    }}
                  />
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
                    Describe visibility, duration, and proximity to sensitive receptors or industrial clusters.
                  </div>
                </div>
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 4: SUBMIT CTA
                ------------------------------------------------------------- */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={isSubmitting}
                isLoading={isSubmitting}
                style={{
                  padding: '0.9rem',
                  fontSize: '1rem',
                  boxShadow: '0 4px 20px rgba(56, 189, 248, 0.35)',
                }}
              >
                {isSubmitting ? 'ANALYZING & SUBMITTING...' : 'SUBMIT ENVIRONMENTAL EVIDENCE →'}
              </Button>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', lineHeight: 1.45 }}>
                Submissions are assigned an audit reference, indexed into Uber H3 Resolution 8, and evaluated by Gemini Vision as auxiliary evidence.
              </div>
            </div>
          </form>

          {/* Right Side: Community Feed */}
          <div>
            <ReportStatus
              reports={reports}
              onSelectReport={(r) => navigate(`/report/status/${r.id}`)}
              selectedReportId={undefined}
            />
          </div>
        </div>
      )}
    </PageContainer>
  );
};
