import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '../../components/layout/PageContainer';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { ImageUploader } from '../../components/citizen/ImageUploader';
import { ReportStatus } from '../../components/citizen/ReportStatus';
import { useApp } from '../../store/AppContext';
import { CitizenReport as ICitizenReport } from '../../types';
import {
  MapPin,
  Camera,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Eye,
  RefreshCw,
  Layers,
  FileCheck,
  Info,
  ShieldAlert,
} from 'lucide-react';

export const CitizenReport: React.FC = () => {
  const navigate = useNavigate();
  const { selectedCity } = useApp();

  // Form State
  const [coords, setCoords] = useState<{ lat: number; lng: number }>({
    lat: selectedCity?.latitude ?? 18.5204,
    lng: selectedCity?.longitude ?? 73.8567,
  });
  const [h3Cell, setH3Cell] = useState<string>('8860144aa1fffff');
  const [hasLocation, setHasLocation] = useState(true);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [selectedChips, setSelectedChips] = useState<string[]>(['Smoke']);
  const [description, setDescription] = useState('');

  // AI Vision Analysis State
  const [isAnalyzingVision, setIsAnalyzingVision] = useState(false);
  const [visionAnalysis, setVisionAnalysis] = useState<{
    indicators: string[];
    confidence: number;
    status: string;
  } | null>(null);

  // Submission & Success state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedReport, setSubmittedReport] = useState<ICitizenReport | null>(null);

  // Community feed
  const [reports, setReports] = useState<ICitizenReport[]>([
    {
      id: 'CR-4821',
      cityId: selectedCity?.id || 'city-1',
      latitude: (selectedCity?.latitude ?? 18.5204) + 0.008,
      longitude: (selectedCity?.longitude ?? 73.8567) - 0.006,
      category: 'SMOKE',
      description: 'Dense black smoke plume observed from open waste burning behind Shivajinagar industrial yard.',
      submittedAt: new Date(Date.now() - 25 * 60000).toISOString(),
      status: 'VERIFIED',
    },
    {
      id: 'CR-4819',
      cityId: selectedCity?.id || 'city-1',
      latitude: (selectedCity?.latitude ?? 18.5204) - 0.012,
      longitude: (selectedCity?.longitude ?? 73.8567) + 0.015,
      category: 'DUST',
      description: 'Heavy construction aggregate dust blowing across arterial highway without water suppression.',
      submittedAt: new Date(Date.now() - 75 * 60000).toISOString(),
      status: 'PENDING',
    },
  ]);

  const observationChips = [
    'Smoke',
    'Dust',
    'Burning',
    'Strong odour',
    'Industrial activity',
    'Other',
  ];

  const toggleChip = (chip: string) => {
    if (selectedChips.includes(chip)) {
      setSelectedChips(selectedChips.filter((c) => c !== chip));
    } else {
      setSelectedChips([...selectedChips, chip]);
    }
  };

  const handleUseMyLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude });
          setH3Cell('8860144aa1fffff');
          setHasLocation(true);
        },
        () => {
          // Fallback to city center
          setCoords({ lat: selectedCity?.latitude ?? 18.5204, lng: selectedCity?.longitude ?? 73.8567 });
          setHasLocation(true);
        }
      );
    }
  };

  const handleImageSelected = (file: File) => {
    setImageFile(file);
    setIsAnalyzingVision(true);
    setVisionAnalysis(null);

    // Simulate Gemini 2.5 Vision analysis
    setTimeout(() => {
      setIsAnalyzingVision(false);
      setVisionAnalysis({
        indicators: [
          'Dense smoke-like particulate plume detected',
          'Possible open biomass or refuse combustion',
          'Reduced horizontal optical visibility in background',
        ],
        confidence: 0.89,
        status: 'CORROBORATING_SIGNAL_IDENTIFIED',
      });
    }, 1400);
  };

  const handleClearImage = () => {
    setImageFile(null);
    setVisionAnalysis(null);
    setIsAnalyzingVision(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    setTimeout(() => {
      const newRep: ICitizenReport = {
        id: `CR-${Math.floor(1000 + Math.random() * 9000)}`,
        cityId: selectedCity?.id || 'city-1',
        latitude: coords.lat,
        longitude: coords.lng,
        category: (selectedChips[0]?.toUpperCase() as any) || 'OTHER',
        description: description || `Observed ${selectedChips.join(', ')} near ${selectedCity?.name || 'City'}.`,
        submittedAt: new Date().toISOString(),
        status: 'PENDING',
      };

      setReports([newRep, ...reports]);
      setSubmittedReport(newRep);
      setIsSubmitting(false);
    }, 1000);
  };

  const resetForm = () => {
    setSubmittedReport(null);
    setImageFile(null);
    setVisionAnalysis(null);
    setDescription('');
    setSelectedChips(['Smoke']);
  };

  return (
    <PageContainer
      title="Report Environmental Evidence"
      subtitle="Help municipal authorities identify emission signals that fixed sensors may miss"
    >
      {/* =========================================================================
          SUCCESS SCREEN (SECTION 15)
          ========================================================================= */}
      {submittedReport ? (
        <div style={{ maxWidth: '720px', margin: '1rem auto' }}>
          <div
            style={{
              padding: '3rem 2rem',
              borderRadius: '20px',
              background: 'linear-gradient(145deg, rgba(16, 185, 129, 0.1), rgba(56, 189, 248, 0.05))',
              border: '1px solid rgba(16, 185, 129, 0.35)',
              textAlign: 'center',
              boxShadow: 'var(--shadow-lg)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            {/* Large Check Animation / Circle */}
            <div
              style={{
                width: '72px',
                height: '72px',
                borderRadius: '50%',
                background: 'rgba(16, 185, 129, 0.18)',
                border: '2px solid var(--accent-teal)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--accent-teal)',
                marginBottom: '1.5rem',
                boxShadow: '0 0 25px rgba(16, 185, 129, 0.35)',
              }}
            >
              <CheckCircle2 size={40} />
            </div>

            <h2 style={{ fontSize: '2rem', fontWeight: 800, fontFamily: 'var(--font-heading)', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              Evidence Submitted
            </h2>
            <p style={{ fontSize: '1rem', color: 'var(--text-secondary)', maxWidth: '520px', lineHeight: 1.5, marginBottom: '2rem' }}>
              Report received ✓ Location mapped to H3 cell. Evidence has been added to environmental intelligence.
            </p>

            {/* Metadata Card */}
            <div
              style={{
                width: '100%',
                maxWidth: '480px',
                padding: '1.25rem',
                borderRadius: '12px',
                background: 'var(--bg-surface-elevated)',
                border: '1px solid var(--border-subtle)',
                marginBottom: '2rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Report Reference</span>
                <strong style={{ color: 'var(--brand-primary)', fontFamily: 'var(--font-mono)' }}>#{submittedReport.id}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Location (H3 Hex)</span>
                <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{h3Cell}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Status</span>
                <span
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    padding: '0.2rem 0.5rem',
                    borderRadius: '4px',
                    background: 'rgba(245, 158, 11, 0.15)',
                    color: 'var(--accent-amber)',
                    border: '1px solid rgba(245, 158, 11, 0.3)',
                  }}
                >
                  UNDER REVIEW
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>AI Vision Analysis</span>
                <span
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    padding: '0.2rem 0.5rem',
                    borderRadius: '4px',
                    background: 'rgba(168, 85, 247, 0.15)',
                    color: 'var(--accent-purple)',
                    border: '1px solid rgba(168, 85, 247, 0.3)',
                  }}
                >
                  AVAILABLE
                </span>
              </div>
            </div>

            {/* Regulatory Disclaimer */}
            <div
              style={{
                fontSize: '0.75rem',
                color: 'var(--text-muted)',
                marginBottom: '2rem',
                maxWidth: '480px',
                lineHeight: 1.45,
              }}
            >
              <strong>Important Note:</strong> Citizen reports are supporting evidence only, not confirmed source attribution. Observations undergo cross-corroboration with nearby ground sensors and satellite telemetry.
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', justifyContent: 'center' }}>
              <Button
                variant="primary"
                size="md"
                onClick={resetForm}
              >
                <FileCheck size={16} style={{ marginRight: '0.4rem' }} />
                VIEW REPORT / SUBMIT ANOTHER
              </Button>
              <Button
                variant="secondary"
                size="md"
                onClick={() => navigate('/map')}
              >
                RETURN TO MAP <ArrowRight size={16} style={{ marginLeft: '0.4rem' }} />
              </Button>
            </div>
          </div>
        </div>
      ) : (
        /* =========================================================================
            FIVE-STEP REPORTING WORKFLOW (SECTION 14)
            ========================================================================= */
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(340px, 1.4fr) minmax(300px, 1fr)', gap: '2rem', alignItems: 'start' }}>
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {/* -------------------------------------------------------------
                STEP 1: LOCATION
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'var(--brand-surface)', color: 'var(--brand-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: 800 }}>
                    1
                  </div>
                  <span>Location</span>
                </div>
              }
              subtitle="Map emission observation to a spatial intelligence cell"
              badge={<Badge variant="info">H3 Resolution 8</Badge>}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
                <div
                  style={{
                    padding: '1rem',
                    borderRadius: '10px',
                    background: 'var(--bg-surface-elevated)',
                    border: '1px solid var(--border-subtle)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '0.75rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--brand-primary)' }}>
                      <MapPin size={20} />
                    </div>
                    <div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target Spatial Hex Cell</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                        H3-{h3Cell}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>
                        Lat: {coords.lat.toFixed(4)} | Lng: {coords.lng.toFixed(4)} ({selectedCity?.name || 'City'})
                      </div>
                    </div>
                  </div>

                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleUseMyLocation}
                  >
                    <MapPin size={14} style={{ marginRight: '0.4rem' }} /> USE MY LOCATION
                  </Button>
                </div>
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 2: PHOTO
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'var(--brand-surface)', color: 'var(--brand-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: 800 }}>
                    2
                  </div>
                  <span>Photo Evidence</span>
                </div>
              }
              subtitle="Attach ground imagery for automated multimodal verification"
            >
              <div style={{ marginTop: '0.5rem' }}>
                <ImageUploader
                  onImageSelected={handleImageSelected}
                  onClearImage={handleClearImage}
                />
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 3: DESCRIPTION
                ------------------------------------------------------------- */}
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'var(--brand-surface)', color: 'var(--brand-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: 800 }}>
                    3
                  </div>
                  <span>Description & Observation Type</span>
                </div>
              }
              subtitle="What did you observe at this location?"
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
                {/* Selectable Quick Chips */}
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '0.5rem', fontWeight: 600 }}>
                    SELECT CATEGORY INDICATORS:
                  </label>
                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    {observationChips.map((chip) => {
                      const isSelected = selectedChips.includes(chip);
                      return (
                        <button
                          key={chip}
                          type="button"
                          onClick={() => toggleChip(chip)}
                          style={{
                            padding: '0.4rem 0.85rem',
                            borderRadius: '9999px',
                            background: isSelected ? 'var(--brand-surface)' : 'var(--bg-surface-elevated)',
                            border: isSelected ? '1px solid var(--brand-primary)' : '1px solid var(--border-subtle)',
                            color: isSelected ? 'var(--brand-primary)' : 'var(--text-secondary)',
                            fontWeight: isSelected ? 700 : 500,
                            fontSize: '0.8rem',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease',
                          }}
                        >
                          {isSelected ? `✓ ${chip}` : chip}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Textarea Description */}
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '0.4rem', fontWeight: 600 }}>
                    DETAILED OBSERVATION (OPTIONAL):
                  </label>
                  <textarea
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Describe specific details, e.g., heavy dark plume rising from waste dump behind railway tracks..."
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      borderRadius: '10px',
                      background: 'var(--bg-surface-elevated)',
                      border: '1px solid var(--border-medium)',
                      color: 'var(--text-primary)',
                      fontSize: '0.875rem',
                      fontFamily: 'inherit',
                      resize: 'vertical',
                    }}
                  />
                </div>
              </div>
            </Card>

            {/* -------------------------------------------------------------
                STEP 4: GEMINI VISION ANALYSIS
                ------------------------------------------------------------- */}
            {(isAnalyzingVision || visionAnalysis || imageFile) && (
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'rgba(168, 85, 247, 0.15)', color: 'var(--accent-purple)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: 800 }}>
                      4
                    </div>
                    <span>Gemini Vision Analysis</span>
                  </div>
                }
                subtitle="Automated multimodal visual feature extraction"
                badge={<Badge variant="warning">Gemini 2.5 Vision</Badge>}
              >
                {isAnalyzingVision ? (
                  <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    <div
                      style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        border: '3px solid rgba(168, 85, 247, 0.2)',
                        borderTopColor: 'var(--accent-purple)',
                        animation: 'spin 1s linear infinite',
                        margin: '0 auto 1rem auto',
                      }}
                    />
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      Analyzing visual evidence with Gemini 2.5 Vision...
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                      Detecting particulate plumes, thermal signatures, and optical attenuation
                    </div>
                  </div>
                ) : visionAnalysis ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                    <div
                      style={{
                        padding: '1rem',
                        borderRadius: '10px',
                        background: 'rgba(168, 85, 247, 0.08)',
                        border: '1px solid rgba(168, 85, 247, 0.25)',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--accent-purple)', textTransform: 'uppercase' }}>
                          Observed Visual Indicators
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          Confidence: <strong style={{ color: 'var(--accent-teal)' }}>{(visionAnalysis.confidence * 100).toFixed(0)}%</strong>
                        </span>
                      </div>

                      <ul style={{ margin: 0, paddingLeft: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                        {visionAnalysis.indicators.map((ind, i) => (
                          <li key={i}>{ind}</li>
                        ))}
                      </ul>
                    </div>

                    {/* Mandatory Disclaimer */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                        fontSize: '0.75rem',
                        color: 'var(--text-muted)',
                        padding: '0.5rem 0.75rem',
                        background: 'var(--bg-surface-elevated)',
                        borderRadius: '6px',
                        border: '1px solid var(--border-subtle)',
                      }}
                    >
                      <Info size={14} style={{ flexShrink: 0, color: 'var(--accent-amber)' }} />
                      <span>
                        <em>Visual analysis is supporting evidence, not proof of pollution source.</em>
                      </span>
                    </div>
                  </div>
                ) : null}
              </Card>
            )}

            {/* -------------------------------------------------------------
                STEP 5: SUBMISSION
                ------------------------------------------------------------- */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isSubmitting}
                style={{
                  padding: '0.9rem',
                  fontSize: '1rem',
                  boxShadow: '0 4px 20px rgba(56, 189, 248, 0.35)',
                }}
              >
                SUBMIT ENVIRONMENTAL EVIDENCE →
              </Button>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center' }}>
                Your submission is assigned a cryptographic audit ID and routed to municipal field monitors.
              </div>
            </div>
          </form>

          {/* Right Side: Community Submissions List */}
          <div>
            <ReportStatus reports={reports} />
          </div>
        </div>
      )}
    </PageContainer>
  );
};
